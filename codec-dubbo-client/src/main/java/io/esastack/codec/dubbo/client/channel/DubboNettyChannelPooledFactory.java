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

import esa.commons.concurrent.ThreadFactories;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.commons.pool.PooledObjectFactory;
import io.esastack.codec.dubbo.client.DubboClientBuilder;
import io.esastack.codec.dubbo.client.exception.ConnectFailedException;
import io.esastack.codec.dubbo.client.exception.TslHandshakeFailedException;
import io.esastack.codec.dubbo.core.utils.SslUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.ssl.SslContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 客户端连接池构建Factory
 */
public class DubboNettyChannelPooledFactory implements PooledObjectFactory<DubboNettyChannel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboNettyChannelPooledFactory.class);

    private static final ThreadFactory THREAD_FACTORY =
            ThreadFactories.namedThreadFactory("DubboConnect-Timer-", true);

    private static final Timer INSTANCE = new HashedWheelTimer(THREAD_FACTORY);

    private final DubboClientBuilder builder;

    private final SslContext sslContext;

    public DubboNettyChannelPooledFactory(DubboClientBuilder builder, SslContext sslContext) {
        this.builder = builder;
        this.sslContext = sslContext;
    }

    void handleTimeout(final ChannelFuture channelConnectFuture,
                       final CompletableFuture<DubboNettyChannel> connectionFinishedFuture,
                       final DubboNettyChannel dubboNettyChannel) {
        if (!channelConnectFuture.isDone()) {
            dubboNettyChannel.close();
            final String errMsg =
                    "Client connect to the " + builder.getHost() + ":" + builder.getPort() + " timeout.";
            connectionFinishedFuture.completeExceptionally(new ConnectFailedException(errMsg));
        } else if (sslContext != null) {
            Future<Channel> tlsHandshakeFuture = dubboNettyChannel.getTslHandshakeFuture();
            //This is executed in another thread, tlsHandshakeFuture may be null at the critical time
            if (tlsHandshakeFuture == null || !tlsHandshakeFuture.isDone()) {
                dubboNettyChannel.close();
                final String errMsg =
                        "Client TSL handshake with " + builder.getHost() + ":" + builder.getPort() + " timeout.";
                connectionFinishedFuture.completeExceptionally(new TslHandshakeFailedException(errMsg));
            }
        }
    }

    void handleConnectionComplete(final Future future,
                                  final Timeout timeout,
                                  final CompletableFuture<DubboNettyChannel> connectionFinishedFuture,
                                  final DubboNettyChannel dubboNettyChannel) {
        if (connectionFinishedFuture.isDone()) {
            //already timeout yet
            return;
        }

        if (!future.isSuccess()) {
            dubboNettyChannel.close();
            final String errMsg =
                    "Client connect to the " + builder.getHost() + ":" + builder.getPort() + " failure.";
            connectionFinishedFuture.completeExceptionally(new ConnectFailedException(errMsg, future.cause()));
            //LOGGER.warn(errMsg, future.cause());
        } else if (sslContext != null) {
            /*
             * TSL handshake future is set when init the Channel; because the init processing is async,
             * so it may be null at the create time, and because the connect processing is after the init
             * processing, so when the listeners are called, the init processing is completed, so the tls
             * handshake future is definitely assigned.
             */
            Future<Channel> tlsHandshakeFuture = dubboNettyChannel.getTslHandshakeFuture();
            tlsHandshakeFuture.addListener(handshakeFuture ->
                    handleTlsComplete(handshakeFuture, timeout, connectionFinishedFuture, dubboNettyChannel));
        } else {
            timeout.cancel();
            connectionFinishedFuture.complete(dubboNettyChannel);
            LOGGER.info("Client connect to the " + builder.getHost() + ":" + builder.getPort() + " success.");
        }
    }

    void handleTlsComplete(final Future<?> tlsHandshakeFuture,
                           final Timeout timeout,
                           final CompletableFuture<DubboNettyChannel> connectionFinishedFuture,
                           final DubboNettyChannel dubboNettyChannel) {
        timeout.cancel();
        if (tlsHandshakeFuture.isSuccess()) {
            //save TLS certificate
            SslUtils.extractSslPeerCertificate(dubboNettyChannel.getChannel());
            connectionFinishedFuture.complete(dubboNettyChannel);
            LOGGER.info("Client TSL handshake with the " + builder.getHost() + ":" +
                    builder.getPort() + " success.");
        } else {
            dubboNettyChannel.close();
            final String errMsg = "Client TSL handshake with the " + builder.getHost() + ":" +
                    builder.getPort() + " failure.";
            connectionFinishedFuture.completeExceptionally(
                    new TslHandshakeFailedException(errMsg, tlsHandshakeFuture.cause()));
        }
    }

    DubboNettyChannel connectSync(final DubboNettyChannel channel, final Throwable throwable) {
        if (throwable instanceof TslHandshakeFailedException) {
            LOGGER.error("TLS handle shake failed, retry connecting to "
                    + builder.getHost() + ":" + builder.getPort() + " without tls.", throwable);
            DubboNettyChannel ch = new DubboNettyChannel(builder, null);
            ch.connect();
            return ch;
        } else if (throwable instanceof ConnectFailedException) {
            throw (ConnectFailedException) throwable;
        } else if (throwable != null) {
            throw new ConnectFailedException(throwable);
        } else {
            return channel;
        }
    }

    @Override
    public CompletableFuture<DubboNettyChannel> create() {
        final CompletableFuture<DubboNettyChannel> connectionFinishedFuture = new CompletableFuture<>();
        final DubboNettyChannel dubboNettyChannel = new DubboNettyChannel(this.builder, this.sslContext);
        final ChannelFuture connectFuture;
        try {
            connectFuture = dubboNettyChannel.asyncConnect();
        } catch (Exception e) {
            //LOGGER.warn("Failed to connected to " + builder.getHost() + ":" + builder.getPort(), e);
            throw e;
        }

        final Timeout timeout = INSTANCE.newTimeout(to ->
                        handleTimeout(connectFuture, connectionFinishedFuture, dubboNettyChannel),
                builder.getConnectTimeout(), TimeUnit.MILLISECONDS);

        connectFuture.addListener(future ->
                handleConnectionComplete(future, timeout, connectionFinishedFuture, dubboNettyChannel));

        if (!builder.isTlsFallback2Normal()) {
            return connectionFinishedFuture;
        } else {
            //Fallback to normal connection
            return connectionFinishedFuture.handle(this::connectSync);
        }
    }

    @Override
    public CompletableFuture<Void> destroy(DubboNettyChannel connection) {
        return connection.close();
    }

    @Override
    public Boolean validate(DubboNettyChannel connection) {
        return connection != null && connection.isActive();
    }
}
