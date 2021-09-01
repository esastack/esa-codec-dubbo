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
import io.esastack.codec.common.utils.NettyUtils;
import io.esastack.codec.dubbo.core.DubboConstants;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;

public abstract class BaseServerBizHandlerAdapter implements DubboServerBizHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseServerBizHandlerAdapter.class);

    @Override
    public void process(DubboMessage request, DubboResponseHolder responseHolder) {
        if (request.getHeader().isHeartbeat()) {
            final String channelInfo = getChannelInfo(responseHolder.getChannelHandlerContext());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("received heart beat request, " + channelInfo);
            }
            DubboMessage serverResponse = new DubboMessage();
            DubboHeader header = new DubboHeader();
            header.setSeriType(request.getHeader().getSeriType())
                    .setRequestId(request.getHeader().getRequestId())
                    .setHeartbeat(true)
                    .setStatus(DubboConstants.RESPONSE_STATUS.OK);
            serverResponse.setHeader(header);
            try {
                serverResponse.setBody(NettyUtils.nullValue(request.getHeader().getSeriType()));
            } catch (Exception e) {
                LOGGER.error("received bad heart request NettyUtils nullValue error, " + channelInfo, e);
                return;
            }
            responseHolder.end(serverResponse);
            return;
        }
        this.process0(request, responseHolder);
    }

    private String getChannelInfo(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        SocketAddress localAddress = channel.localAddress();
        SocketAddress remoteAddress = channel.remoteAddress();
        return "[" + localAddress + " -> " + remoteAddress + "]";
    }

    protected abstract void process0(DubboMessage request, DubboResponseHolder dubboResponseHolder);

    @Override
    public abstract void shutdown();
}
