/*
 * Copyright 2021 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.esastack.codec.dubbo.client.handler;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.common.exception.UnknownResponseStatusException;
import io.esastack.codec.common.utils.NettyUtils;
import io.esastack.codec.dubbo.client.serialize.SerializeHandler;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.DubboMessageWrapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.Map;

/**
 * The client processes the response after receiving the response
 */
public class DubboClientHandler extends SimpleChannelInboundHandler<DubboMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboClientHandler.class);
    private static final int MAX_SENT_HEARTBEAT_COUNT = 2;
    private final String connectionName;
    private final Map<Long, ResponseCallback> callbackMap;
    /**
     * Only read/write in one Thread
     */
    private int sentHeartbeatCount;

    public DubboClientHandler(String connectionName, Map<Long, ResponseCallback> callbackMap) {
        // NOT auto release
        super(false);
        this.connectionName = connectionName;
        this.callbackMap = callbackMap;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) {
        Throwable error = wrapIfNecessary(t);
        // Call back all DubboCallback
        for (Map.Entry<Long, ResponseCallback> entry : callbackMap.entrySet()) {
            ResponseCallback callback = callbackMap.remove(entry.getKey());
            if (callback != null) {
                callback.onError(error);
            }
        }

        // WARNING: Closing the connection should lag the callback, otherwise the error message will be swallowed
        // A network exception or decoding exception occurs, and the connection is closed
        ctx.close();
    }

    private Throwable wrapIfNecessary(Throwable t) {
        if (NettyUtils.isUnknownProtocolException(t)) {
            return t;
        }
        String errMsg = connectionName + (t instanceof IOException ? " force disconnect" : " catch exception");
        return new UnknownResponseStatusException(errMsg, t);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        for (Map.Entry<Long, ResponseCallback> entry : callbackMap.entrySet()) {
            ResponseCallback callback = callbackMap.get(entry.getKey());
            if (callback != null) {
                callback.onError(new UnknownResponseStatusException(
                        "Could not get remote server handle result",
                        new ConnectException(
                                "Connection is inactive.(maybe caused by remote server closed the connection)")));
            }
        }
        callbackMap.clear();

        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DubboMessage response) {

        // Reset un-responded heartbeat count
        sentHeartbeatCount = 0;

        long requestId = response.getHeader().getRequestId();
        // The heartbeat packet returns directly and the response is actively released
        if (response.getHeader().isHeartbeat()) {
            response.release();
            return;
        }
        // Get the asynchronous request callback function
        ResponseCallback callback = callbackMap.remove(requestId);

        // In the case of protocol errors, timeouts, etc.,
        // the callback is cleaned up, and we need to actively release the response
        if (callback == null) {
            response.release();
            return;
        }

        final Map<String, String> ttfbAttachments = NettyUtils.extractTtfbKey(ctx.channel());

        // Synchronous call, business thread deserialize
        if (!callback.deserialized()) {
            // Prevent refCnt from becoming 0 and cause ByteBuf to be freed
            DubboMessageWrapper messageWrapper = new DubboMessageWrapper(response);
            messageWrapper.addAttachments(ttfbAttachments);
            callback.onResponse(messageWrapper);
            return;
        }

        SerializeHandler.get().deserialize(response, callback, ttfbAttachments);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            String channelInfo = getChannelInfo(ctx);
            if (sentHeartbeatCount >= MAX_SENT_HEARTBEAT_COUNT) {
                LOGGER.info("Idle event triggered 3 times and no heartbeat responded, disconnect the channel{}",
                        channelInfo);
                ctx.close();
                return;
            }
            DubboMessage heartbeat = new DubboMessage();
            DubboHeader header = new DubboHeader();
            header.setSeriType((byte) 2).setHeartbeat(true).setRequest(true);
            heartbeat.setHeader(header);
            heartbeat.setBody(NettyUtils.nullValue((byte) 2));
            ctx.writeAndFlush(heartbeat).addListener(future -> {
                if (future.isSuccess()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.info("Idle event triggered, client send heart beat request. The idle channel{}",
                                channelInfo);
                    }
                    sentHeartbeatCount++;
                } else {
                    LOGGER.info("Failed to send heartbeat request, disconnect the channel" +
                            channelInfo, future.cause());
                    ctx.close();
                }
            });
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private String getChannelInfo(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        SocketAddress localAddress = channel.localAddress();
        SocketAddress remoteAddress = channel.remoteAddress();
        return "[" + localAddress + " -> " + remoteAddress + "]";
    }
}
