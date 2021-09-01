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

import io.esastack.codec.common.server.NettyServerConfig;
import io.esastack.codec.common.ssl.SslContextBuilder;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.server.handler.DubboResponseHolder;
import io.esastack.codec.dubbo.server.handler.DubboServerBizHandler;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DubboServerBuilderTest {

    @Test
    public void testDubboServerBuilder() throws Exception {
        final int defaultIoThreads = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);
        Map<ChannelOption, Object> options = new HashMap<>(16);
        Map<ChannelOption, Object> childOptions = new HashMap<>(16);
        options.put(ChannelOption.SO_BACKLOG, 2048);
        options.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        childOptions.put(ChannelOption.SO_REUSEADDR, true);
        childOptions.put(ChannelOption.SO_KEEPALIVE, true);
        childOptions.put(ChannelOption.TCP_NODELAY, true);
        DubboServerBuilder serverBuilder = NettyDubboServer.newBuilder()
                .setServerConfig(new NettyServerConfig()
                        .setPayload(16 * 1024 * 1024)
                        .setPort(20888)
                        .setChannelHandlers(null)
                        .setSoBacklogSize(1025)
                        .setChildChannelOptions(childOptions)
                        .setChannelOptions(options)
                        .setBindIp("localhost")
                        .setHeartbeatTimeoutSeconds(66)
                        .setIoThreads(defaultIoThreads)
                        .setBossThreads(1)
                        .setUnixDomainSocketFile("demo"))
                .setBizHandler(new DubboServerBizHandler() {
                    @Override
                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {

                    }

                    @Override
                    public void shutdown() {

                    }
                });

        SslContextBuilder sslContextBuilder = new SslContextBuilder();
        SelfSignedCertificate ssCertificate = new SelfSignedCertificate();
        File certificateFile = ssCertificate.certificate();
        File privateKeyFile = ssCertificate.privateKey();
        //X509Certificate x509Certificate = ssCertificate.cert();
        sslContextBuilder.setCertificate(new FileInputStream(certificateFile));
        sslContextBuilder.setPrivateKey(new FileInputStream(privateKeyFile));
        serverBuilder.getServerConfig().setSslContextBuilder(sslContextBuilder);
        //System.out.println(serverBuilder.toString());
        assertEquals(66, serverBuilder.getServerConfig().getHeartbeatTimeoutSeconds());
        assertEquals(1025, serverBuilder.getServerConfig().getSoBacklogSize());
        assertEquals("localhost", serverBuilder.getServerConfig().getBindIp());
        assertTrue(serverBuilder.getServerConfig().getChildChannelOptions().containsKey(ChannelOption.SO_REUSEADDR));
        assertTrue(serverBuilder.getServerConfig().getChannelOptions().containsKey(ChannelOption.SO_BACKLOG));
        assertEquals(defaultIoThreads, serverBuilder.getServerConfig().getIoThreads());
        assertEquals(1, serverBuilder.getServerConfig().getBossThreads());
        assertEquals("demo", serverBuilder.getServerConfig().getUnixDomainSocketFile());
    }
}
