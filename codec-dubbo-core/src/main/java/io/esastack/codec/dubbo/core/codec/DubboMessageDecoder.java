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

import io.esastack.codec.dubbo.core.exception.UnknownProtocolException;
import io.esastack.codec.dubbo.core.utils.DubboConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * Dubbo protocol
 * <p>
 * Header一共是16个字节（byte[16] ）
 * <ul>
 * <li>2byte magic:类似java字节码文件里的魔数，用来判断是不是dubbo协议的数据包。魔数是常量0xdabb</li>
 * <li>1byte 的消息标志位（分为高4位和底四位，通过|符号进行连接）:16-20 序列化id, 21 event, 22 two way,
 * 23请求或响应标识</li>
 * <li>1byte 状态，当消息类型为响应时，设置响应状态。24-31位。状态位, 设置请求响应状态</li>
 * <li>8byte 消息ID,long类型，32-95位。每一个请求的唯一识别id（由于采用异步通讯的方式，
 * 用来把请求request和返回的response对应上）</li>
 * <li>4byte 消息长度，96-127位。消息体 body 长度, int 类型，即记录Body Content有多少个字节。</li>
 * </ul>
 */
public class DubboMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf frame, List<Object> out) {
        Object decoded = callDecode(ctx, frame);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    protected Object callDecode(ChannelHandlerContext ctx, ByteBuf frame) {
        DubboMessage dubboMessage = new DubboMessage();

        //读取header
        DubboHeader header = decodeHeader(frame, ctx);
        dubboMessage.setHeader(header);

        //读取Body
        int len = frame.readableBytes() - DubboConstants.HEADER_LENGTH;
        if (len > 0) {
            ByteBuf byteBuf = frame.slice(DubboConstants.HEADER_LENGTH, len).retain();
            dubboMessage.setBody(byteBuf);
        }

        //Dubbo协议收包完成事件时间 纳秒
        Channel channel = ctx.channel();
        channel.attr(DubboConstants.DECODE_TTFB_COMPLETE_KEY).set(System.currentTimeMillis());
        return dubboMessage;
    }

    protected DubboHeader decodeHeader(ByteBuf frame, ChannelHandlerContext ctx) {
        DubboHeader header = new DubboHeader();
        //Magic 魔术验证
        int readableBytesLength = frame.readableBytes();
        if (readableBytesLength >= DubboConstants.MAGIC_LENGTH) {
            byte magicHigh = frame.getByte(0);
            byte magicLow = frame.getByte(1);
            if (magicLow != DubboConstants.MAGIC_LOW || magicHigh != DubboConstants.MAGIC_HIGH) {
                throw new UnknownProtocolException(
                        "protocol Decoder error, magic number is not match (short) 0xdabb," +
                                " please check protocol header magic number !");
            }
        } else {
            throw new UnknownProtocolException("protocol Decoder error,frame readableBytes should not be zero ");
        }

        byte flag = frame.getByte(2);
        if ((flag & DubboConstants.HEADER_FLAG.FLAG_REQUEST) != 0) {
            header.setRequest(true);
        }
        //读取序列化类型
        byte seriType = (byte) (flag & DubboConstants.SERIALIZATION_MASK);
        header.setSeriType(seriType);

        //读取请求ID
        header.setRequestId(frame.getLong(4));

        //读取是否two way请求
        header.setTwoWay((flag & DubboConstants.HEADER_FLAG.FLAG_TWOWAY) != 0);

        //读取是否心跳请求
        header.setHeartbeat((flag & DubboConstants.HEADER_FLAG.FLAG_HEARTBEAT) != 0);

        //读取响应状态码
        byte status = frame.getByte(3);
        header.setStatus(status);

        return header;
    }

}
