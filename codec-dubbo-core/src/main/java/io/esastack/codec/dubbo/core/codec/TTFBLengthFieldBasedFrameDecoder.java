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
package io.esastack.codec.dubbo.core.codec;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.dubbo.core.exception.UnknownProtocolException;
import io.esastack.codec.dubbo.core.utils.DubboConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.net.InetSocketAddress;

/**
 * TTFB 首字节收包处理 继承与 LengthFieldBasedFrameDecoder
 */
public class TTFBLengthFieldBasedFrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger logger = LoggerFactory.getLogger(TTFBLengthFieldBasedFrameDecoder.class);

    private boolean protoValidated = false;

    private boolean inDecodeProcess = false;

    public TTFBLengthFieldBasedFrameDecoder(int maxFrameLength,
                                            int lengthFieldOffset,
                                            int lengthFieldLength,
                                            int lengthAdjustment,
                                            int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    /**
     * 重写decode()方法 用于Trace监控首字节收包
     * <p>
     * 上层抽象类ByteToMessageDecoder中 callDecode会循环多次调用decode(), 直到满足LengthFieldBasedFrameDecoder 条件为止
     * <p>
     * Attention: the first readable byte of frame maybe greater than 0, it's frame.readerIndex(); Only check the first
     * two bytes of this connection to validate the protocol;
     *
     * @param ctx   ctx
     * @param frame data
     * @return byteFrame
     */
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf frame) throws Exception {

        if (!protoValidated) {
            if (frame.readableBytes() >= DubboConstants.MAGIC_LENGTH && isDubboMagic(frame)) {
                protoValidated = true;
            } else {
                logger.info("Received message which is not dubbo protocol, check the remote protocol please!");
                final InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                final InetSocketAddress localAddress = (InetSocketAddress) ctx.channel().localAddress();
                final String reqMsg = remoteAddress.getHostString() + ":" + remoteAddress.getPort() + " --> " +
                        localAddress.getHostString() + ":" + localAddress.getPort();
                throw new UnknownProtocolException(
                        "Protocol decode error, magic number mismatched(first two bytes is not 0xdabb), " +
                                "please check if the client/server uses dubbo protocol; " +
                                "The request flow is: " + reqMsg);
            }
        }

        //Dubbo协议收到首字节后，保证每一次请求只执行一次标记
        if (!inDecodeProcess && frame.readableBytes() >= DubboConstants.MAGIC_LENGTH && isDubboMagic(frame)) {
            ctx.channel().attr(DubboConstants.DECODE_TTFB_KEY).set(System.currentTimeMillis());
            inDecodeProcess = true;
        }

        final Object decoded;
        try {
            decoded = super.decode(ctx, frame);
            //如返回值非空，表示一个DubboMessage被decode完毕，标志位执false，下一个字节为下一个request的首字节
            if (decoded != null) {
                inDecodeProcess = false;
            }
        } catch (Throwable t) {
            // decoding error
            inDecodeProcess = false;
            throw t;
        }

        return decoded;
    }

    private boolean isDubboMagic(ByteBuf frame) {
        assert frame.readableBytes() >= 2;
        int readerIndex = frame.readerIndex();
        return frame.getByte(readerIndex) == DubboConstants.MAGIC_HIGH &&
                frame.getByte(readerIndex + 1) == DubboConstants.MAGIC_LOW;
    }
}

