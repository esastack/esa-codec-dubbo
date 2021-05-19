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

import java.io.ByteArrayInputStream;
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

    private DubboSslContextBuilder getBuilder() {
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

    private InputStream loadPrivateKeyInputStream() {
        return new ByteArrayInputStream(privateKey.getBytes());
    }

    private InputStream loadCertificateInputStream() {
        return new ByteArrayInputStream(certificate.getBytes());
    }

    private InputStream loadTrustCertificatesInputStream() {
        return new ByteArrayInputStream(trustCertificates.getBytes());
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

    final String certificate = "-----BEGIN CERTIFICATE-----\n" +
            "MIICXDCCAcUCFDQtOIFzTqTwj8AVNZMTDZ/o0ZBNMA0GCSqGSIb3DQEBCwUAMG0x\n" +
            "CzAJBgNVBAYTAkNOMQ0wCwYDVQQIDARURVNUMQ0wCwYDVQQHDARURVNUMQ0wCwYD\n" +
            "VQQKDARURVNUMQ0wCwYDVQQLDARURVNUMQ0wCwYDVQQDDARURVNUMRMwEQYJKoZI\n" +
            "hvcNAQkBFgRURVNUMB4XDTIxMDUwNzA3MjkxNVoXDTMxMDUwNTA3MjkxNVowbTEL\n" +
            "MAkGA1UEBhMCQ04xDTALBgNVBAgMBFRFU1QxDTALBgNVBAcMBFRFU1QxDTALBgNV\n" +
            "BAoMBFRFU1QxDTALBgNVBAsMBFRFU1QxDTALBgNVBAMMBFRFU1QxEzARBgkqhkiG\n" +
            "9w0BCQEWBFRFU1QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANyNnyULV1sa\n" +
            "GltmQi8UVUnDWEWvO6FDJD1qzd64WXvUjUTghWZZliUdYBdZ+B3Jr4Cy5x42fzcI\n" +
            "wFz6rdVCZ9VHkbsdABPOgQJVinV7ppDOQj/pTm/Cs60x4IGrp0D1SakRV5bpIa/6\n" +
            "MiBzTLSqwbWlGlYJrq+ljjR1i88k4t2vAgMBAAEwDQYJKoZIhvcNAQELBQADgYEA\n" +
            "q9COH+Lg/5sXmBIHC9i2ueyLSBTzOz6kcyM1TbpFvaVSkh58jMuhySF9sj0Qsn+M\n" +
            "XIYnK+eUQKBljNsg0gkCXsdnLCo6yYXQ541bTDLrNgdTHS2O0DpXuj2qLZJSBYSG\n" +
            "nYOjlmcEMwnUKIcJyv84jHLP2+ccDeX2El+7yTZ64vE=\n" +
            "-----END CERTIFICATE-----";

    final String privateKey = "-----BEGIN PRIVATE KEY-----\n" +
            "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBANyNnyULV1saGltm\n" +
            "Qi8UVUnDWEWvO6FDJD1qzd64WXvUjUTghWZZliUdYBdZ+B3Jr4Cy5x42fzcIwFz6\n" +
            "rdVCZ9VHkbsdABPOgQJVinV7ppDOQj/pTm/Cs60x4IGrp0D1SakRV5bpIa/6MiBz\n" +
            "TLSqwbWlGlYJrq+ljjR1i88k4t2vAgMBAAECgYBFOKm/Pa0AKdQl5ZVWI2KVURsu\n" +
            "W84yUdlY8WGFyoRDSjXAbVtRAUMPiQW0rociCj/r+7pwEBijVDrTs9XFPh9KCwo1\n" +
            "fQUafecyax/fXV7Gt21saUEVitN1ULar6UGlV1++BD60631QHwFHDaK1V4J36kx/\n" +
            "ibn0uy2eNJJkPOTUCQJBAPJPdfcowXJItCSLK2xCNAWYT7BJJePWBDJcgicvjOhf\n" +
            "U9BX3Xn4LHPMpAVLU7Y3t/5JKOKo3KDYFEX7no0Bm0MCQQDpA3yBH+AB0RM0yiOf\n" +
            "xnwlH24HsPuDIISRGHuHiOiYy0Au4ZDbrNkP0bo5iVAzR/9PUTJzm9d1vanF49dX\n" +
            "D48lAkAPSN/iFVoOgXOLkpPMomhxqefs8NBJDOj63EcBfchfqBO7Yq9/0B3NuCzo\n" +
            "gJXpOp6KlcbUdV5lbvvoZjTcJCvNAkAs58UYxWHQN9Cxvbr70a6fIN19kfgGnz+t\n" +
            "DsDPr+zTdWgbINFf5IG4cLyo1fOkzl0/lfBZI1F0mWacgno/hvoZAkEAv4KMtSmx\n" +
            "7IL4e0ASniKg1HzitcGvmcodOvcrMI+BKALel1Y6lrU3JzVwPAlvmlEwmmSiqPjS\n" +
            "JqnqEsFCQGVwbg==\n" +
            "-----END PRIVATE KEY-----";

    final String trustCertificates = "-----BEGIN CERTIFICATE-----\n" +
            "MIICXDCCAcUCFDQtOIFzTqTwj8AVNZMTDZ/o0ZBNMA0GCSqGSIb3DQEBCwUAMG0x\n" +
            "CzAJBgNVBAYTAkNOMQ0wCwYDVQQIDARURVNUMQ0wCwYDVQQHDARURVNUMQ0wCwYD\n" +
            "VQQKDARURVNUMQ0wCwYDVQQLDARURVNUMQ0wCwYDVQQDDARURVNUMRMwEQYJKoZI\n" +
            "hvcNAQkBFgRURVNUMB4XDTIxMDUwNzA3MjkxNVoXDTMxMDUwNTA3MjkxNVowbTEL\n" +
            "MAkGA1UEBhMCQ04xDTALBgNVBAgMBFRFU1QxDTALBgNVBAcMBFRFU1QxDTALBgNV\n" +
            "BAoMBFRFU1QxDTALBgNVBAsMBFRFU1QxDTALBgNVBAMMBFRFU1QxEzARBgkqhkiG\n" +
            "9w0BCQEWBFRFU1QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANyNnyULV1sa\n" +
            "GltmQi8UVUnDWEWvO6FDJD1qzd64WXvUjUTghWZZliUdYBdZ+B3Jr4Cy5x42fzcI\n" +
            "wFz6rdVCZ9VHkbsdABPOgQJVinV7ppDOQj/pTm/Cs60x4IGrp0D1SakRV5bpIa/6\n" +
            "MiBzTLSqwbWlGlYJrq+ljjR1i88k4t2vAgMBAAEwDQYJKoZIhvcNAQELBQADgYEA\n" +
            "q9COH+Lg/5sXmBIHC9i2ueyLSBTzOz6kcyM1TbpFvaVSkh58jMuhySF9sj0Qsn+M\n" +
            "XIYnK+eUQKBljNsg0gkCXsdnLCo6yYXQ541bTDLrNgdTHS2O0DpXuj2qLZJSBYSG\n" +
            "nYOjlmcEMwnUKIcJyv84jHLP2+ccDeX2El+7yTZ64vE=\n" +
            "-----END CERTIFICATE-----";
}
