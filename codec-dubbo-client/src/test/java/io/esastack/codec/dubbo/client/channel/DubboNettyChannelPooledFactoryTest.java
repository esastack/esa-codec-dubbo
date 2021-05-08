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

import io.esastack.codec.dubbo.client.DubboClientBuilder;
import io.esastack.codec.dubbo.client.DubboSDKServer;
import io.esastack.codec.dubbo.client.exception.ConnectFailedException;
import io.esastack.codec.dubbo.client.exception.TslHandshakeFailedException;
import io.esastack.codec.dubbo.core.ssl.DubboSslContextBuilder;
import io.esastack.codec.dubbo.server.NettyDubboServer;
import io.netty.channel.Channel;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

public class DubboNettyChannelPooledFactoryTest {

    private static volatile NettyDubboServer server;

    @BeforeClass
    public static void startServer() {
        try {
            server = DubboSDKServer.start(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void stopServer() {
        //server.shutdown();
    }

    @Test
    public void create() {
        final DubboNettyChannelPooledFactory factory =
                new DubboNettyChannelPooledFactory(createBuilder(20880, 1000), null);
        CompletableFuture<DubboNettyChannel> future = factory.create();
        try {
            final DubboNettyChannel channel = future.get();
            assertTrue(channel.isActive());
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void createRefused() {
        final DubboNettyChannelPooledFactory factory =
                new DubboNettyChannelPooledFactory(createBuilder(20000, 100), null);
        CompletableFuture<DubboNettyChannel> future = factory.create();
        try {
            future.get();
            fail();
        } catch (Throwable e) {
            //e.printStackTrace();
            assertTrue(e.getCause() instanceof ConnectFailedException);
        }
    }

    @Test
    public void connectSyncTest() {
        final DubboNettyChannelPooledFactory factory =
                new DubboNettyChannelPooledFactory(createBuilder(20880, 100), null);
        final DubboNettyChannel dubboNettyChannel =
                new DubboNettyChannel(createBuilder(20880, 100), null);
        final DubboNettyChannel ch1 = factory.connectSync(dubboNettyChannel, new TslHandshakeFailedException(""));
        assertNotEquals(dubboNettyChannel, ch1);

        try {
            factory.connectSync(dubboNettyChannel, new ConnectFailedException(""));
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ConnectFailedException);
        }

        try {
            factory.connectSync(dubboNettyChannel, new RuntimeException(""));
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof ConnectFailedException);
        }

        final DubboNettyChannel ch4 = factory.connectSync(dubboNettyChannel, null);
        assertEquals(dubboNettyChannel, ch4);
    }

    @Test
    public void handleConnectionComplete() {
        try {
            //un-success without ssl
            final DubboNettyChannelPooledFactory factory =
                    new DubboNettyChannelPooledFactory(createBuilder(20000, 100), null);
            final DubboNettyChannel dubboNettyChannel =
                    new DubboNettyChannel(createBuilder(20000, 100), null);
            final CompletableFuture<DubboNettyChannel> future = new CompletableFuture<>();
            final Channel channel = new EmbeddedChannel();
            final DefaultChannelPromise channelPromise = new DefaultChannelPromise(channel);
            factory.handleConnectionComplete(channelPromise, createTimeout(), future, dubboNettyChannel);
            future.get();
            fail();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof ConnectFailedException);
        }

        try {
            //success without ssl
            final DubboNettyChannelPooledFactory factory =
                    new DubboNettyChannelPooledFactory(createBuilder(20000, 100), null);
            final DubboNettyChannel dubboNettyChannel =
                    new DubboNettyChannel(createBuilder(20000, 100), null);
            final CompletableFuture<DubboNettyChannel> future = new CompletableFuture<>();
            final Channel channel = new EmbeddedChannel();
            final DefaultChannelPromise channelPromise = new DefaultChannelPromise(channel);
            channelPromise.setSuccess();
            factory.handleConnectionComplete(channelPromise, createTimeout(), future, dubboNettyChannel);
            future.get();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        try {
            //un-success with ssl
            final SslContext sslContext = buildSSLContext();
            final DubboNettyChannel sslNettyChannel =
                    new DubboNettyChannel(createBuilder(20000, 100), sslContext);
            sslNettyChannel.setTslHandshakeFuture(createTlsHandshakeFuture(false));
            final DubboNettyChannelPooledFactory sslFactory =
                    new DubboNettyChannelPooledFactory(createBuilder(20000, 100), sslContext);
            final DefaultChannelPromise completedChannelPromise = new DefaultChannelPromise(new EmbeddedChannel());
            completedChannelPromise.setSuccess();
            final CompletableFuture<DubboNettyChannel> completedFuture = new CompletableFuture<>();
            sslFactory.handleConnectionComplete(
                    completedChannelPromise, createTimeout(), completedFuture, sslNettyChannel);
            completedFuture.get();
            fail();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof TslHandshakeFailedException);
        }

        try {
            //success with ssl
            final EmbeddedChannel channel = new EmbeddedChannel();
            final SslContext sslContext = buildSSLContext();
            final DubboNettyChannel sslNettyChannel =
                    new DubboNettyChannel(createBuilder(20000, 100), sslContext);
            sslNettyChannel.setChannel(channel);
            sslNettyChannel.setTslHandshakeFuture(createTlsHandshakeFuture(true));
            final DubboNettyChannelPooledFactory sslFactory =
                    new DubboNettyChannelPooledFactory(createBuilder(20000, 100), sslContext);
            final DefaultChannelPromise completedChannelPromise = new DefaultChannelPromise(channel);
            completedChannelPromise.setSuccess();
            final CompletableFuture<DubboNettyChannel> completedFuture = new CompletableFuture<>();
            sslFactory.handleConnectionComplete(
                    completedChannelPromise, createTimeout(), completedFuture, sslNettyChannel);
            completedFuture.get();
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void handleTimeoutTest() {
        final DubboNettyChannelPooledFactory factory =
                new DubboNettyChannelPooledFactory(createBuilder(20000, 100), null);
        final DubboNettyChannel dubboNettyChannel =
                new DubboNettyChannel(createBuilder(20000, 100), null);
        final CompletableFuture<DubboNettyChannel> future = new CompletableFuture<>();
        final Channel channel = new EmbeddedChannel();
        final DefaultChannelPromise channelPromise = new DefaultChannelPromise(channel);
        try {
            factory.handleTimeout(channelPromise, future, dubboNettyChannel);
            future.get();
            fail();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof ConnectFailedException);
        }

        try {
            final SslContext sslContext = buildSSLContext();
            final DubboNettyChannel sslNettyChannel =
                    new DubboNettyChannel(createBuilder(20000, 100), sslContext);
            final DubboNettyChannelPooledFactory sslFactory =
                    new DubboNettyChannelPooledFactory(createBuilder(20000, 100), sslContext);
            final DefaultChannelPromise completedChannelPromise = new DefaultChannelPromise(channel);
            completedChannelPromise.setSuccess();
            final CompletableFuture<DubboNettyChannel> completedFuture = new CompletableFuture<>();
            sslFactory.handleTimeout(completedChannelPromise, completedFuture, sslNettyChannel);
            completedFuture.get();
            fail();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof TslHandshakeFailedException);
        }

    }

    private Future<Channel> createTlsHandshakeFuture(boolean success) {
        DefaultPromise<Channel> defaultPromise = new DefaultPromise<>(new DefaultEventExecutor());
        if (success) {
            defaultPromise.setSuccess(new EmbeddedChannel());
        } else {
            defaultPromise.setFailure(new RuntimeException());
        }
        return defaultPromise;
    }

    private Timeout createTimeout() {
        return new Timeout() {
            @Override
            public Timer timer() {
                return null;
            }

            @Override
            public TimerTask task() {
                return null;
            }

            @Override
            public boolean isExpired() {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean cancel() {
                return false;
            }
        };
    }

    private SslContext buildSSLContext() throws Exception {
        DubboSslContextBuilder builder = getBuilder();
        return builder.buildClient();
    }

    private DubboSslContextBuilder getBuilder()  throws IOException {
        DubboSslContextBuilder builder = new DubboSslContextBuilder();
        builder.setPrivateKey(loadPrivateKeyInputStream());
        builder.setCertificate(loadCertificateInputStream());
        builder.setTrustCertificates(loadTrustCertificatesInputStream());
        builder.setCiphers(new String[0]);
        builder.setEnabledProtocols(new String[0]);
        builder.setHandshakeTimeoutMillis(1000);
        builder.setKeyPassword(null);
        return builder;
    }

    private InputStream loadPrivateKeyInputStream() throws IOException {
        return new FileInputStream("src/test/resources/tls/private-key.pem");
    }

    private InputStream loadCertificateInputStream() throws IOException {
        return new FileInputStream("src/test/resources/tls/certificate.pem");
    }

    private InputStream loadTrustCertificatesInputStream() throws IOException {
        return new FileInputStream("src/test/resources/tls/trust-certificates.pem");
    }

    private static DubboClientBuilder createBuilder(int port, int connectTimeout) {
        final DubboClientBuilder.MultiplexPoolBuilder poolBuilder = DubboClientBuilder
                .MultiplexPoolBuilder
                .newBuilder()
                .setInit(false)
                .setBlockCreateWhenInit(false)
                .setWaitCreateWhenLastTryAcquire(false)
                .setMaxPoolSize(1)
                .setMaxRetryTimes(3);
        return new DubboClientBuilder()
                .setMultiplexPoolBuilder(poolBuilder)
                .setHost("127.0.0.1")
                .setConnectTimeout(connectTimeout)
                .setPort(port)
                .setDubboSslContextBuilder(null)
                .setTlsFallback2Normal(false)
                .setChannelHandlers(new ArrayList<>())
                .setChannelOptions(new HashMap<>())
                .setDefaultRequestTimeout(1000)
                .setHeartbeatTimeoutSeconds(10)
                .setPayload(100000)
                .setReadTimeout(1000)
                .setUnixDomainSocketFile("ddd")
                .setUseNativeTransports(true)
                .setWriteBufferHighWaterMark(32768)
                .setWriteTimeout(1000)
                .setConnectTimeout(1000);
    }
}
