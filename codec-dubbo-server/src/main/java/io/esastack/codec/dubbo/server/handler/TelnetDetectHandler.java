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
import io.esastack.codec.dubbo.server.DubboServerBuilder;
import io.esastack.codec.dubbo.server.handler.telnet.TelnetDecodeHandler;
import io.esastack.codec.dubbo.server.handler.telnet.TelnetHandlerAdapter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.esastack.codec.dubbo.core.DubboConstants.*;

/**
 * Telnet protocol detect handler
 */
public class TelnetDetectHandler extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(TelnetDetectHandler.class);

    private final DubboServerBuilder builder;

    public TelnetDetectHandler(DubboServerBuilder builder) {
        this.builder = builder;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readableBytes = in.readableBytes();
        if (readableBytes >= MAGIC_LENGTH) {
            byte magicHigh = in.getByte(0);
            byte magicLow = in.getByte(1);
            if (magicHigh == MAGIC_HIGH && magicLow == MAGIC_LOW) {
                ctx.pipeline().addLast(new TlsDetectHandler(builder));
                ctx.pipeline().remove(this);
            } else {
                decodeTelnet(ctx);
            }
        } else if (readableBytes == 1 && in.getByte(0) != MAGIC_HIGH) {
            decodeTelnet(ctx);
        }
    }

    private void decodeTelnet(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(new IdleStateHandler(0, 0, 180, TimeUnit.SECONDS));
        ctx.pipeline().addLast(new TelnetDecodeHandler());
        ctx.pipeline().addLast(new TelnetHandlerAdapter());
        ctx.pipeline().remove(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        if (socketAddress == null) {
            logger.error("Exception caught, remote address is null: ", cause);
        } else {
            logger.error("Disconnect from client[" + socketAddress.toString() + "], caused by: ", cause);
        }
        ctx.close();
    }
}
