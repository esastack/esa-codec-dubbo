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
import io.esastack.codec.dubbo.core.utils.DubboConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import static io.esastack.codec.dubbo.core.utils.DubboConstants.HEADER_LENGTH;

/**
 * Dubbo protocol
 * <p>
 * Header一共是16个字节（byte[16] ）
 * <ul>
 * <li>2byte magic:类似java字节码文件里的魔数，用来判断是不是dubbo协议的数据包。魔数是常量0xdabb</li>
 * <li>1byte 的消息标志位（分为高4位和底四位，通过|符号进行连接）:16-20 序列化id, 21 event, 22 two way, 23请求或响应标识</li>
 * <li>1byte 状态，当消息类型为响应时，设置响应状态。24-31位。状态位, 设置请求响应状态</li>
 * <li>8byte 消息ID,long类型，32-95位。每一个请求的唯一识别id（由于采用异步通讯的方式，用来把请求request和返回的response对应上）</li>
 * <li>4byte 消息长度，96-127位。消息体 body 长度, int 类型，即记录Body Content有多少个字节。</li>
 * </ul>
 */
public class DubboMessageEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DubboMessageEncoder.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof DubboMessage) {
            DubboMessage dubboMessage = (DubboMessage) msg;
            int bodyLength = (dubboMessage.getBody() == null ? 0 : dubboMessage.getBody().length);

            ByteBuf buffer = ctx.alloc().directBuffer(HEADER_LENGTH + bodyLength);

            encodeHeader(buffer, dubboMessage.getHeader(), bodyLength);
            buffer.writeBytes(dubboMessage.getBody() == null ? new byte[0] : dubboMessage.getBody());

            ctx.writeAndFlush(buffer, promise);
        } else {
            ctx.writeAndFlush(msg, promise);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Encode dubbo message complete!");
        }

    }

    private void encodeHeader(ByteBuf frame, DubboHeader header, int bodyLength) {
        //设置魔数，用于区分是否Dubbo协议
        frame.writeShort(DubboConstants.MAGIC);

        //请求flag，多个flag共享1个字节
        byte flag = header.getSeriType();

        //设置是否two way请求
        if (header.isTwoWay()) {
            // alibaba dubbo must to set the flag request or response
            if (header.isRequest()) {
                flag |= (byte) (DubboConstants.HEADER_FLAG.FLAG_REQUEST | header.getSeriType());
            }
            flag |= DubboConstants.HEADER_FLAG.FLAG_TWOWAY;
        }

        //设置是否心跳请求
        if (header.isHeartbeat()) {
            flag |= DubboConstants.HEADER_FLAG.FLAG_HEARTBEAT;
        }
        //设置请求flag
        frame.writeByte(flag);

        //设置响应状态
        byte status = header.getStatus();
        frame.writeByte(status);

        //设置请求ID
        frame.writeLong(header.getRequestId());
        frame.writeInt(bodyLength);
    }
}
