/*
 * Copyright 2022 OPPO ESA Stack Project
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
package io.esastack.codec.common.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class CustomNettyServer extends NettyServer {

    public CustomNettyServer(NettyServerConfig serverConfig) {
        super(serverConfig);
    }

    @Override
    protected void shutdown0() {

    }

    @Override
    protected ServerConnectionInitializer createConnectionInitializer(NettyServerConfig serverConfig) {
        return channel -> {
            channel.pipeline().addLast(new StringDecoder());
            channel.pipeline().addLast(new StringEncoder());
            channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    channel.writeAndFlush(msg);
                }
            });
        };
    }
}
