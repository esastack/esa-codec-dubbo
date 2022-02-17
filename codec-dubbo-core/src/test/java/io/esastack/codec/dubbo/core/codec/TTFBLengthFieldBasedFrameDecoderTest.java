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

import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.esastack.codec.serialization.api.SerializeFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.concurrent.DefaultPromise;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TTFBLengthFieldBasedFrameDecoderTest {

    @Test
    public void testDecode() {
        EmbeddedChannel channel = new EmbeddedChannel(new TTFBLengthFieldBasedFrameDecoder(8 * 1024 * 1024,
                12, 4, 0, 0));

        assertThrows(DecoderException.class, () -> channel.writeInbound(Unpooled.wrappedBuffer("1".getBytes())));
    }

    @Test
    public void test() throws InterruptedException, IOException, ExecutionException {
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new TTFBLengthFieldBasedFrameDecoder(8 * 1024 * 1024, 12, 4, 0, 0));
                        pipeline.addLast(new DubboMessageEncoder());
                        pipeline.addLast(new DubboMessageDecoder());
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ctx.writeAndFlush(msg);
                            }
                        });
                    }
                })
                .bind(new InetSocketAddress("127.0.0.1", 20800))
                .sync()
                .channel();

        final Serialization serialization =
                SerializeFactory.getSerialization(SerializeConstants.HESSIAN2_SERIALIZATION_ID);

        DefaultPromise<String> promise = new DefaultPromise<>(new NioEventLoopGroup().next());
        final Channel channel = new Bootstrap().group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new DubboMessageEncoder());
                        pipeline.addLast(new TTFBLengthFieldBasedFrameDecoder(8 * 1024 * 1024, 12, 4, 0, 0));
                        pipeline.addLast(new DubboMessageDecoder());
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                DubboMessage message = (DubboMessage) msg;
                                ByteBuf body = message.getBody();
                                byte[] bytes = new byte[body.readableBytes()];
                                body.readBytes(bytes);
                                ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                                DataInputStream deserialize = serialization.deserialize(stream);
                                String s = deserialize.readUTF();
                                promise.setSuccess(s);
                                deserialize.close();
                                message.release();
                            }
                        });
                    }
                })
                .connect(new InetSocketAddress("127.0.0.1", 20800))
                .sync()
                .channel();

        DubboHeader header = new DubboHeader();
        header.setSeriType(SerializeConstants.HESSIAN2_SERIALIZATION_ID);
        header.setRequestId(1L);
        header.setRequest(true);
        header.setHeartbeat(true);
        header.setTwoWay(true);
        DubboMessage message = new DubboMessage();
        message.setHeader(header);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream serialize = serialization.serialize(byteArrayOutputStream);
        serialize.writeUTF("Test");
        serialize.flush();
        byte[] bytes = byteArrayOutputStream.toByteArray();
        message.setBody(channel.alloc().buffer().writeBytes(bytes));
        channel.writeAndFlush(message);
        assertEquals("Test", promise.get());
        serialize.close();
        byteArrayOutputStream.close();
    }
}
