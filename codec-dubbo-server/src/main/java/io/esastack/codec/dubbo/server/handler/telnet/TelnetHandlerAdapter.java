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
package io.esastack.codec.dubbo.server.handler.telnet;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import esa.commons.spi.SpiLoader;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.Optional;

public class TelnetHandlerAdapter extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TelnetHandlerAdapter.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = (String) msg;
        StringBuilder buf = new StringBuilder();
        message = message.trim();
        String command;
        if (message.length() > 0) {
            int i = message.indexOf(' ');
            if (i > 0) {
                command = message.substring(0, i).trim();
                message = message.substring(i + 1).trim();
            } else {
                command = message;
                message = "";
            }
        } else {
            command = "";
        }
        if (command.length() > 0) {
            Optional<TelnetHandler> handler = SpiLoader.getByName(TelnetHandler.class, command);
            if (handler.isPresent()) {
                String result = handler.get().telnet(ctx, message);
                if (result != null) {
                    buf.append(result);
                }
            } else {
                buf.append("Unsupported command: ");
                buf.append(command);
            }
        }
        if (buf.length() > 0) {
            buf.append("\r\n").append("dubbo>");
            ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(buf.toString().getBytes()));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent event = (IdleStateEvent) evt;
        if (event.state() == IdleState.ALL_IDLE) {
            ctx.channel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Exception occur: ", cause);
        super.exceptionCaught(ctx, cause);
    }
}
