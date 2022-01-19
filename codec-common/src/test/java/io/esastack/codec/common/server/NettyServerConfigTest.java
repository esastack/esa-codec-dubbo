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

import io.esastack.codec.common.ssl.SslContextBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.Test;

import java.nio.channels.SocketChannel;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NettyServerConfigTest {
    @Test
    public void test() {
        NettyServerConfig config = new NettyServerConfig();
        assertEquals(1024, config.getSoBacklogSize());
        assertEquals(8 * 1024 * 1024, config.getPayload());
        assertEquals(65, config.getHeartbeatTimeoutSeconds());
        assertEquals("0.0.0.0", config.getBindIp());
        assertEquals(0, config.getChannelOptions().size());
        assertEquals(0, config.getChildChannelOptions().size());
        assertEquals(0, config.getChannelHandlers().size());

        SslContextBuilder builder = new SslContextBuilder();
        ChannelOption<SocketChannel> channelOption = ChannelOption.valueOf("test");
        StringEncoder encoder = new StringEncoder();
        ServerConnectionInitializer initializer = new ServerConnectionInitializer() {
            @Override
            public void initialize(Channel channel) {
            }
        };
        config.setPort(8080);
        config.setBindIp("localhost");
        config.setIoThreads(4);
        config.setBossThreads(1);
        config.setSoBacklogSize(2048);
        config.setPayload(10 * 1024 * 1024);
        config.setHeartbeatTimeoutSeconds(70);
        config.setUnixDomainSocketFile("test");
        config.setSslContextBuilder(builder);
        config.setConnectionInitializer(initializer);
        config.setChannelOptions(Collections.singletonMap(channelOption, "v"));
        config.setChildChannelOptions(Collections.singletonMap(channelOption, "v"));
        config.setChannelHandlers(Collections.singletonList(encoder));

        assertEquals(8080, config.getPort());
        assertEquals("localhost", config.getBindIp());
        assertEquals(4, config.getIoThreads());
        assertEquals(1, config.getBossThreads());
        assertEquals(2048, config.getSoBacklogSize());
        assertEquals(10 * 1024 * 1024, config.getPayload());
        assertEquals(70, config.getHeartbeatTimeoutSeconds());
        assertEquals("test", config.getUnixDomainSocketFile());
        assertEquals(builder, config.getSslContextBuilder());
        assertEquals(initializer, config.getConnectionInitializer());
        assertEquals("v", config.getChannelOptions().get(channelOption));
        assertEquals("v", config.getChildChannelOptions().get(channelOption));
        assertEquals(1, config.getChannelHandlers().size());
        assertEquals(encoder, config.getChannelHandlers().get(0));
        assertNotNull(config.toString());
    }
}
