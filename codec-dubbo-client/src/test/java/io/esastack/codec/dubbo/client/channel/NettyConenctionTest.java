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
package io.esastack.codec.dubbo.client.channel;

import io.esastack.codec.common.connection.NettyConnection;
import io.esastack.codec.common.connection.NettyConnectionConfig;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.dubbo.client.DubboSDKServer;
import io.esastack.codec.dubbo.server.NettyDubboServer;
import io.netty.channel.ChannelFuture;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.ConnectException;

public class NettyConenctionTest {

    private static volatile NettyDubboServer server;

    @BeforeClass
    public static void startServer() {
        try {
            server = DubboSDKServer.start(new String[0]);
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void stopServer() {
        //server.shutdown();
    }

    private static NettyConnectionConfig createConnectionConfig(int port) {
        final NettyConnectionConfig.MultiplexPoolBuilder poolBuilder = NettyConnectionConfig
                .MultiplexPoolBuilder
                .newBuilder()
                .setInit(false)
                .setBlockCreateWhenInit(false)
                .setWaitCreateWhenLastTryAcquire(false)
                .setMaxPoolSize(1);
        return new NettyConnectionConfig()
                .setMultiplexPoolBuilder(poolBuilder)
                .setHost("127.0.0.1")
                .setPort(port);
    }

    @Test(expected = ConnectFailedException.class)
    public void connectSyncFailed() {
        final NettyConnectionConfig connectionConfig = createConnectionConfig(20000);
        final NettyConnection channel = new NettyConnection(connectionConfig, null);
        channel.connect();
    }

    @Test
    public void connectSyncSuccess() {
        final NettyConnectionConfig connectionConfig = createConnectionConfig(20880);
        final NettyConnection channel = new NettyConnection(connectionConfig, null);
        channel.connect();
        Assert.assertTrue(channel.isActive());
        Assert.assertTrue(channel.isWritable());
        Assert.assertTrue(channel.getDubboCallbackMap().isEmpty());
        Assert.assertTrue(channel.getName().startsWith("connect"));
        Assert.assertEquals(0, channel.getRequestIdAtomic().get());
    }

    @Test
    public void connectAsyncFailed() {
        final NettyConnectionConfig connectionConfig = createConnectionConfig(20000);
        final NettyConnection channel = new NettyConnection(connectionConfig, null);
        final ChannelFuture future = channel.asyncConnect();
        try {
            future.get();
            Assert.fail();
        } catch (Throwable ex) {
            //ex.printStackTrace();
            Assert.assertTrue(ex.getCause() instanceof ConnectException);
        }
    }

    @Test
    public void connectAsyncSuccess() {
        final NettyConnectionConfig connectionConfig = createConnectionConfig(20880);
        final NettyConnection channel = new NettyConnection(connectionConfig, null);
        final ChannelFuture future = channel.asyncConnect();
        try {
            future.get();
            Assert.assertTrue(channel.isActive());
        } catch (Throwable ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }
}

