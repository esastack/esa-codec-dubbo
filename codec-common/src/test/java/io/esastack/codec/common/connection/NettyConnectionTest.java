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

import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.server.CustomNettyServer;
import io.esastack.codec.common.server.NettyServerConfig;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.ApplicationProtocolNegotiator;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NettyConnectionTest {

    private static volatile CustomNettyServer server;

    @BeforeClass
    public static void init() {
        server = new CustomNettyServer(createServerConfig());
        server.start();
    }

    @AfterClass
    public static void close() {
        server.shutdown();
    }

    static NettyServerConfig createServerConfig() {
        NettyServerConfig config = new NettyServerConfig();
        config.setBindIp("127.0.0.1");
        config.setPort(20880);
        return config;
    }

    @Test
    public void testConnectFailed() {
        NettyConnectionConfig config = new NettyConnectionConfig();
        config.setHost("127.0.0.1");
        config.setPort(20000);
        NettyConnection connection = new NettyConnection(config, null);
        assertThrows(ConnectFailedException.class, connection::connect);
        ChannelFuture future = connection.asyncConnect();
        assertThrows(ExecutionException.class, future::get);

        assertFalse(connection.isActive());
        connection.isWritable();
        assertEquals(0L, connection.getRequestIdAtomic().get());
        assertNotNull(connection.getChannel());
        assertEquals(0, connection.getCallbackMap().size());
        connection.setChannel(null);
        assertNull(connection.getChannel());

        Future<Channel> future1 = new TestFuture<>();
        assertNull(connection.getTslHandshakeFuture());
        connection.setTslHandshakeFuture(future1);
        assertEquals(future1, connection.getTslHandshakeFuture());

        connection.close();
    }

    @Test
    public void testConnectSuccessful() throws ExecutionException, InterruptedException {
        DefaultPromise<String> promise = new DefaultPromise<>(new NioEventLoopGroup().next());
        NettyConnectionConfig config = new NettyConnectionConfig();
        config.setPort(20880);
        config.setHost("127.0.0.1");
        config.setWriteBufferHighWaterMark(64 * 1024 * 1024);
        config.setChannelHandlers(Collections.singletonList(new StringDecoder()));
        config.setConnectionInitializer((channel, connectionName, callbackMap) -> {
            channel.pipeline().addLast(new StringEncoder());
            channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    promise.setSuccess((String) msg);
                }
            });
        });
        config.setTlsFallback2Normal(true);
        NettyConnection nettyConnection = new NettyConnection(config, null);
        nettyConnection.connect();
        assertTrue(nettyConnection.isActive());
        assertTrue(nettyConnection.isWritable());
        assertNotNull(nettyConnection.getName());
        nettyConnection.writeAndFlush("test");
        assertEquals("test", promise.get());

        nettyConnection.close();
    }

    static class TestFuture<T> implements Future<T> {

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isCancellable() {
            return false;
        }

        @Override
        public Throwable cause() {
            return null;
        }

        @Override
        public Future<T> addListener(GenericFutureListener<? extends Future<? super T>> listener) {
            return null;
        }

        @Override
        public Future<T> addListeners(GenericFutureListener<? extends Future<? super T>>... listeners) {
            return null;
        }

        @Override
        public Future<T> removeListener(GenericFutureListener<? extends Future<? super T>> listener) {
            return null;
        }

        @Override
        public Future<T> removeListeners(GenericFutureListener<? extends Future<? super T>>... listeners) {
            return null;
        }

        @Override
        public Future<T> sync() throws InterruptedException {
            return null;
        }

        @Override
        public Future<T> syncUninterruptibly() {
            return null;
        }

        @Override
        public Future<T> await() throws InterruptedException {
            return null;
        }

        @Override
        public Future<T> awaitUninterruptibly() {
            return null;
        }

        @Override
        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public boolean await(long timeoutMillis) throws InterruptedException {
            return false;
        }

        @Override
        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public boolean awaitUninterruptibly(long timeoutMillis) {
            return false;
        }

        @Override
        public T getNow() {
            return null;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }

    static class TestSslContext extends SslContext {

        @Override
        public boolean isClient() {
            return true;
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
    }
}
