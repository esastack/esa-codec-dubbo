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
package io.esastack.codec.common.client;

import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.common.connection.ConnectionInitializer;
import io.esastack.codec.common.connection.NettyConnection;
import io.esastack.codec.common.connection.NettyConnectionConfig;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.server.CustomNettyServer;
import io.esastack.codec.common.server.NettyServer;
import io.esastack.codec.common.server.NettyServerConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyClientTest {

    private static volatile NettyServer server;

    @BeforeClass
    public static void init() {
        try {
            server = new CustomNettyServer(createServerConfig());
            server.start();
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void close() {
        server.shutdown();
    }

    private static NettyServerConfig createServerConfig() {
        NettyServerConfig config = new NettyServerConfig();
        config.setBindIp("127.0.0.1");
        config.setPort(20880);
        return config;
    }

    private static NettyConnectionConfig createConnectionConfig(int port) {
        final NettyConnectionConfig.MultiplexPoolBuilder poolBuilder = NettyConnectionConfig
                .MultiplexPoolBuilder
                .newBuilder()
                .setInit(true)
                .setBlockCreateWhenInit(true)
                .setWaitCreateWhenLastTryAcquire(true)
                .setMaxPoolSize(10);
        return new NettyConnectionConfig()
                .setMultiplexPoolBuilder(poolBuilder)
                .setHost("127.0.0.1")
                .setPort(port);
    }

    @Test
    public void testClient() throws ExecutionException, InterruptedException {
        assertThrows(ConnectFailedException.class,
                () -> new NettyConnection(createConnectionConfig(20000), null).connectSync());
        TestNettyClient client = new TestNettyClient(createConnectionConfig(20880));
        assertTrue(client.isActive());
        client.sendMsg("test");
        assertEquals("test", client.getPromise().get());

        client.close();
    }

    static class TestNettyClient extends NettyClient {

        private final Promise<String> promise = new DefaultPromise<>(new NioEventLoopGroup().next());

        public TestNettyClient(NettyConnectionConfig connectionConfig) {
            super(connectionConfig);
        }

        public Promise<String> getPromise() {
            return promise;
        }

        @Override
        protected ConnectionInitializer createConnectionInitializer(NettyConnectionConfig connectionConfig) {
            return (channel, connectionName, callbackMap) -> {
                channel.pipeline().addLast(new StringEncoder());
                channel.pipeline().addLast(new StringDecoder());
                channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        promise.setSuccess((String) msg);
                    }
                });
            };
        }

        public void sendMsg(String msg) throws ExecutionException, InterruptedException {
            NettyConnection connection = connectionPool.acquire().get();
            ChannelFuture future = connection.writeAndFlush(msg);
            final TestCallback callback = new TestCallback(promise);
            addTimeoutTask(new ReadTimeoutListener(1000,
                            connection.getRequestIdAtomic().get(),
                            Collections.singletonMap(connection.getRequestIdAtomic().get(), callback), future),
                    1000);
            future.addListener(listener -> {
                notifyWriteDone(future, connection.getRequestIdAtomic().get(), callback, connection);
            });
        }
    }

    static class TestCallback implements ResponseCallback {

        private final Promise<String> promise;

        public TestCallback(Promise<String> promise) {
            this.promise = promise;
        }

        public Promise<String> getPromise() {
            return promise;
        }

        @Override
        public void onResponse(Object result) {
            promise.setSuccess((String) result);
        }

        @Override
        public void onError(Throwable e) {
            promise.setFailure(e);
        }
    }
}
