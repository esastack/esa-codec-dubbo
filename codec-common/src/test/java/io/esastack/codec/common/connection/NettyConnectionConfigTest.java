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
package io.esastack.codec.common.connection;

import io.esastack.codec.common.ssl.SslContextBuilder;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.string.StringDecoder;
import org.junit.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyConnectionConfigTest {

    @Test
    public void testNettyConnectionConfig() {
        NettyConnectionConfig config = new NettyConnectionConfig();
        assertEquals(1000, config.getConnectTimeout());
        assertEquals(8 * 1024 * 1024, config.getPayload());
        assertEquals(60, config.getHeartbeatTimeoutSeconds());
        assertEquals(1000, config.getDefaultRequestTimeout());
        assertEquals(0, config.getChannelHandlers().size());
        assertEquals(0, config.getChannelOptions().size());

        StringDecoder decoder = new StringDecoder();
        ConnectionInitializer initializer = (channel, connectionName, callbackMap) -> {
        };
        NettyConnectionConfig.MultiplexPoolBuilder builder = NettyConnectionConfig.MultiplexPoolBuilder.newBuilder();
        SslContextBuilder sslContextBuilder = new SslContextBuilder();
        config.setHost("localhost");
        config.setPort(8080);
        config.setTlsFallback2Normal(true);
        config.setConnectTimeout(2000);
        config.setPayload(10 * 1024 * 1024);
        config.setHeartbeatTimeoutSeconds(65);
        config.setDefaultRequestTimeout(2000);
        config.setChannelHandlers(Collections.singletonList(decoder));
        config.setChannelOptions(Collections.singletonMap(ChannelOption.SO_BACKLOG, 128));
        config.setConnectionInitializer(initializer);
        config.setMultiplexPoolBuilder(builder);
        config.setSslContextBuilder(sslContextBuilder);
        config.setUseNativeTransports(false);
        config.setWriteBufferHighWaterMark(64 * 1024 * 1024);
        config.setUnixDomainSocketFile("unix");
        builder.setInit(false);
        builder.setMaxPoolSize(200);
        builder.setBlockCreateWhenInit(false);
        builder.setMaxRetryTimes(2);
        builder.setWaitCreateWhenLastTryAcquire(false);

        assertEquals("localhost", config.getHost());
        assertEquals(8080, config.getPort());
        assertTrue(config.isTlsFallback2Normal());
        assertFalse(config.isUseNativeTransports());
        assertEquals(2000, config.getConnectTimeout());
        assertEquals(10 * 1024 * 1024, config.getPayload());
        assertEquals(65, config.getHeartbeatTimeoutSeconds());
        assertEquals(2000, config.getDefaultRequestTimeout());
        assertEquals(1, config.getChannelHandlers().size());
        assertEquals(decoder, config.getChannelHandlers().get(0));
        assertEquals(1, config.getChannelOptions().size());
        assertEquals(128, config.getChannelOptions().get(ChannelOption.SO_BACKLOG));
        assertEquals(initializer, config.getConnectionInitializer());
        assertEquals(builder, config.getMultiplexPoolBuilder());
        assertEquals(sslContextBuilder, config.getSslContextBuilder());
        assertEquals(64 * 1024 * 1024, config.getWriteBufferHighWaterMark());
        assertEquals("unix", config.getUnixDomainSocketFile());
        assertFalse(builder.isBlockCreateWhenInit());
        assertFalse(builder.isInit());
        assertFalse(builder.isWaitCreateWhenLastTryAcquire());
        assertEquals(200, builder.getMaxPoolSize());
        assertEquals(2, builder.getMaxRetryTimes());
    }
}
