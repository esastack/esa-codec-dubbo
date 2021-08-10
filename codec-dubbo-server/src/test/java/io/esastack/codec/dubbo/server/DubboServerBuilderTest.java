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

import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.ssl.DubboSslContextBuilder;
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
        Map<ChannelOption, Object> options = new HashMap<>(16);
        Map<ChannelOption, Object> childOptions = new HashMap<>(16);
        options.put(ChannelOption.SO_BACKLOG, 2048);
        options.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        childOptions.put(ChannelOption.SO_REUSEADDR, true);
        childOptions.put(ChannelOption.SO_KEEPALIVE, true);
        childOptions.put(ChannelOption.TCP_NODELAY, true);
        DubboServerBuilder serverBuilder = new DubboServerBuilder();
        final int defaultIoThreads = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);
        //16 M
        serverBuilder.setPayload(16 * 1024 * 1024);
        serverBuilder.setPort(20888)
                .setChannelHandlers(null)
                .setSoBacklogSize(1025)
                .setChildChannelOptions(childOptions)
                .setChannelOptions(options)
                .setBindIp("localhost")
                .setHeartbeatTimeoutSeconds(66)
                .setIoThreads(defaultIoThreads)
                .setBossThreads(1)
                .setUnixDomainSocketFile("demo")
                .setBizHandler(new DubboServerBizHandler() {
                    @Override
                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {

                    }

                    @Override
                    public void shutdown() {

                    }
                });

        DubboSslContextBuilder dubboSslContextBuilder  = new DubboSslContextBuilder();
        SelfSignedCertificate ssCertificate  = new SelfSignedCertificate();
        File certificateFile = ssCertificate.certificate();
        File privateKeyFile = ssCertificate.privateKey();
        //X509Certificate x509Certificate = ssCertificate.cert();
        dubboSslContextBuilder.setCertificate(new FileInputStream(certificateFile));
        dubboSslContextBuilder.setPrivateKey(new FileInputStream(privateKeyFile));
        serverBuilder.setDubboSslContextBuilder(dubboSslContextBuilder);
        //System.out.println(serverBuilder.toString());
        assertEquals(66, serverBuilder.getHeartbeatTimeoutSeconds());
        assertEquals(1025, serverBuilder.getSoBacklogSize());
        assertEquals("localhost", serverBuilder.getBindIp());
        assertTrue(serverBuilder.getChildChannelOptions().containsKey(ChannelOption.SO_REUSEADDR));
        assertTrue(serverBuilder.getChannelOptions().containsKey(ChannelOption.SO_BACKLOG));
        assertEquals(defaultIoThreads, serverBuilder.getIoThreads());
        assertEquals(1, serverBuilder.getBossThreads());
        assertEquals("demo", serverBuilder.getUnixDomainSocketFile());
    }
}
