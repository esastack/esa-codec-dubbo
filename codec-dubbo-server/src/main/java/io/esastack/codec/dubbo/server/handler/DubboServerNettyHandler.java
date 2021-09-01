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
package io.esastack.codec.dubbo.server.handler;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.common.exception.UnknownProtocolException;
import io.esastack.codec.common.ssl.SslUtils;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleStateEvent;

import java.net.SocketAddress;

/**
 * 服务端接受请求后 进行I/O数据处理
 * extract tls peer certificate into attribute
 */
@ChannelHandler.Sharable
public class DubboServerNettyHandler extends SimpleChannelInboundHandler<DubboMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboServerNettyHandler.class);

    private DubboServerBizHandler handler;

    public DubboServerNettyHandler(DubboServerBizHandler handler) {
        this.handler = handler;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable t) throws Exception {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        if (socketAddress == null) {
            LOGGER.error("Exception caught, remote address is null: ", t);
        } else {
            if (t instanceof UnknownProtocolException) {
                // Distinguish protocol analysis exception
                LOGGER.info("Disconnect from client[" + socketAddress.toString() + "], caused by: ", t);
            } else {
                LOGGER.error("Disconnect from client[" + socketAddress.toString() + "], caused by: ", t);
            }
        }
        ctx.close();
    }

    /**
     * close the channel if no read and write event in 3*heartbeat-internal
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            SocketAddress socketAddress = ctx.channel().remoteAddress();
            LOGGER.info("Disconnect from client[" + socketAddress + "], caused by IdleStateEvent");
            ctx.close();
        } else if (evt instanceof SslHandshakeCompletionEvent) {
            SslUtils.extractSslPeerCertificate(ctx.channel(), (SslHandshakeCompletionEvent) evt);
        } else {
            //NOP
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DubboMessage request) {
        DubboResponseHolder responseHolder = new DubboResponseHolder(ctx);
        handler.process(request, responseHolder);
    }
}
