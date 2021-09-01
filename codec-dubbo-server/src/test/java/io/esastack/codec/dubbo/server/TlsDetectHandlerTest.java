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
package io.esastack.codec.dubbo.server;

import io.esastack.codec.common.exception.SerializationException;
import io.esastack.codec.common.server.NettyServerConfig;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.DubboMessageEncoder;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.server.handler.DubboResponseHolder;
import io.esastack.codec.dubbo.server.handler.DubboServerBizHandler;
import io.esastack.codec.dubbo.server.handler.TelnetDetectHandler;
import io.esastack.codec.dubbo.server.handler.TlsDetectHandler;
import io.esastack.codec.dubbo.server.handler.telnet.handler.ExceptionHandler;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TlsDetectHandlerTest {

    static DubboServerBuilder serverBuilder;

    static {
        serverBuilder = NettyDubboServer.newBuilder()
                .setServerConfig(new NettyServerConfig()
                        .setPayload(16 * 1024 * 1024)
                        .setPort(20888)
                        .setSoBacklogSize(1025)
                        .setBindIp("localhost")
                        .setHeartbeatTimeoutSeconds(66))
                .setBizHandler(new DubboServerBizHandler() {
                    @Override
                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                        RpcInvocation invocation = null;
                        try {
                            invocation = ServerCodecHelper.toRpcInvocation(request);
                        } catch (Exception e) {
                            fail();
                        }
                        String str = "hello  world from client " + invocation.getRequestId() + " " +
                                invocation.getSeriType();
                        DubboRpcResult rpcResult = DubboRpcResult.success(invocation.getRequestId(),
                                invocation.getSeriType(), str);
                        DubboMessage dubboMessage = null;
                        try {
                            dubboMessage = ServerCodecHelper.toDubboMessage(rpcResult, request.getBody().alloc());
                        } catch (SerializationException e) {
                            e.printStackTrace();
                            dubboResponseHolder.getChannelHandlerContext().channel().close();
                        }
                        dubboResponseHolder.end(dubboMessage);
                    }

                    @Override
                    public void shutdown() {

                    }
                });
    }

    @Test
    public void testProtocolDetectHandler() throws Exception {
        Map<String, String> attachments = new HashMap<>();
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new String[]{"dubbo"});
        rpcInvocation.setInterfaceName("org.apache.dubbo.demo.DemoService");
        rpcInvocation.setReturnType(String.class);
        rpcInvocation.setAttachments(attachments);
        rpcInvocation.setSeriType(SerializeConstants.HESSIAN2_SERIALIZATION_ID);
        DubboMessage mockRequestDubboMesage = ClientCodecHelper.toDubboMessage(rpcInvocation);
        List<ChannelHandler> channelHandlers = new ArrayList<>();
        serverBuilder.getServerConfig().setChannelHandlers(channelHandlers);
        EmbeddedChannel embeddedChannelWrite = new EmbeddedChannel(new DubboMessageEncoder());
        Assert.assertTrue(embeddedChannelWrite.writeOutbound(mockRequestDubboMesage));
        Assert.assertTrue(embeddedChannelWrite.finish());
        ByteBuf byteBufMessage = embeddedChannelWrite.readOutbound();
        TlsDetectHandler detectHandler =
                new TlsDetectHandler(serverBuilder);
        //EmbeddedChannel embeddedChannelRead = new EmbeddedChannel(detectHandler);
        EmbeddedChannel embeddedChannelRead = new EmbeddedChannel(detectHandler);
        assertFalse(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("test".getBytes())));
        EmbeddedChannel exceptionChannel = new EmbeddedChannel(new ExceptionHandler(),
                new TlsDetectHandler(serverBuilder));
        assertTrue(exceptionChannel.isActive());
        assertFalse(exceptionChannel.writeInbound(Unpooled.wrappedBuffer("test".getBytes())));
        assertFalse(exceptionChannel.isActive());
        boolean writeInbound = embeddedChannelRead.writeInbound(byteBufMessage);
        boolean finish = embeddedChannelRead.finish();
        DubboMessage resultMessage = embeddedChannelRead.readInbound();
        RpcInvocation response = ServerCodecHelper.toRpcInvocation(resultMessage);
    }

    @Test
    public void testExceptionCaught() {
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new ExceptionHandler(),
                new TelnetDetectHandler(serverBuilder));
        try {
            embeddedChannel.writeInbound(Unpooled.EMPTY_BUFFER);
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }
    }

    @Test
    public void testSsl() {
        SslContext sslContext = new SslContext() {
            @Override
            public boolean isClient() {
                return false;
            }

            @Override
            public List<String> cipherSuites() {
                return null;
            }

            @Override
            public long sessionCacheSize() {
                return 0;
            }

            @Override
            public long sessionTimeout() {
                return 0;
            }

            @Override
            public ApplicationProtocolNegotiator applicationProtocolNegotiator() {
                return null;
            }

            @Override
            public SSLEngine newEngine(ByteBufAllocator alloc) {
                return null;
            }

            @Override
            public SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort) {
                return null;
            }

            @Override
            public SSLSessionContext sessionContext() {
                return null;
            }
        };
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new TlsDetectHandler(serverBuilder, sslContext));

    }

    @Test
    public void testDecode() {
        // 测试入栈消息
        EmbeddedChannel readSingleCharacter = new EmbeddedChannel(new TelnetDetectHandler(serverBuilder));
        assertFalse(readSingleCharacter.writeInbound(Unpooled.wrappedBuffer("l".getBytes())));
        EmbeddedChannel readMultiCharacter = new EmbeddedChannel(new TelnetDetectHandler(serverBuilder));
        assertFalse(readMultiCharacter.writeInbound(Unpooled.wrappedBuffer("ls\r\n".getBytes())));
    }

}
