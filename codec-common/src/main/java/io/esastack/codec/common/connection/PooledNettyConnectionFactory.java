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
package io.esastack.codec.common.connection;

import esa.commons.concurrent.ThreadFactories;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.exception.TslHandshakeFailedException;
import io.esastack.codec.common.ssl.SslUtils;
import io.esastack.codec.commons.pool.PooledObjectFactory;
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
public class PooledNettyConnectionFactory implements PooledObjectFactory<NettyConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PooledNettyConnectionFactory.class);
    private static final ThreadFactory THREAD_FACTORY =
            ThreadFactories.namedThreadFactory("DubboConnect-Timer-", true);
    private static final Timer INSTANCE = new HashedWheelTimer(THREAD_FACTORY);

    private final SslContext sslContext;
    private final NettyConnectionConfig builder;

    public PooledNettyConnectionFactory(NettyConnectionConfig builder) {
        this.builder = builder;
        this.sslContext = createSslContext(builder);
    }

    private SslContext createSslContext(final NettyConnectionConfig builder) {
        if (builder.getSslContextBuilder() != null) {
            try {
                return builder.getSslContextBuilder().buildClient();
            } catch (Exception e) {
                LOGGER.error("build tls sslContext error", e);
            }
        }
        return null;
    }

    public void handleTimeout(final ChannelFuture channelConnectFuture,
                              final CompletableFuture<NettyConnection> connectionFinishedFuture,
                              final NettyConnection nettyConnection) {
        if (!channelConnectFuture.isDone()) {
            nettyConnection.close();
            final String errMsg =
                    "Client connect to the " + builder.getHost() + ":" + builder.getPort() + " timeout.";
            connectionFinishedFuture.completeExceptionally(new ConnectFailedException(errMsg));
        } else if (sslContext != null) {
            Future<Channel> tlsHandshakeFuture = nettyConnection.getTslHandshakeFuture();
            //This is executed in another thread, tlsHandshakeFuture may be null at the critical time
            if (tlsHandshakeFuture == null || !tlsHandshakeFuture.isDone()) {
                nettyConnection.close();
                final String errMsg =
                        "Client TSL handshake with " + builder.getHost() + ":" + builder.getPort() + " timeout.";
                connectionFinishedFuture.completeExceptionally(new TslHandshakeFailedException(errMsg));
            }
        }
    }

    public void handleConnectionComplete(final Future future,
                                         final Timeout timeout,
                                         final CompletableFuture<NettyConnection> connectionFinishedFuture,
                                         final NettyConnection nettyConnection) {
        if (connectionFinishedFuture.isDone()) {
            //already timeout yet
            return;
        }

        if (!future.isSuccess()) {
            nettyConnection.close();
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
            Future<Channel> tlsHandshakeFuture = nettyConnection.getTslHandshakeFuture();
            tlsHandshakeFuture.addListener(handshakeFuture ->
                    handleTlsComplete(handshakeFuture, timeout, connectionFinishedFuture, nettyConnection));
        } else {
            timeout.cancel();
            connectionFinishedFuture.complete(nettyConnection);
            LOGGER.info("Client connect to the " + builder.getHost() + ":" + builder.getPort() + " success.");
        }
    }

    void handleTlsComplete(final Future<?> tlsHandshakeFuture,
                           final Timeout timeout,
                           final CompletableFuture<NettyConnection> connectionFinishedFuture,
                           final NettyConnection nettyConnection) {
        timeout.cancel();
        if (tlsHandshakeFuture.isSuccess()) {
            //save TLS certificate
            SslUtils.extractSslPeerCertificate(nettyConnection.getChannel());
            connectionFinishedFuture.complete(nettyConnection);
            LOGGER.info("Client TSL handshake with the " + builder.getHost() + ":" +
                    builder.getPort() + " success.");
        } else {
            nettyConnection.close();
            final String errMsg = "Client TSL handshake with the " + builder.getHost() + ":" +
                    builder.getPort() + " failure.";
            connectionFinishedFuture.completeExceptionally(
                    new TslHandshakeFailedException(errMsg, tlsHandshakeFuture.cause()));
        }
    }

    public NettyConnection connectSync(final NettyConnection channel, final Throwable throwable) {
        if (throwable instanceof TslHandshakeFailedException) {
            LOGGER.error("TLS handle shake failed, retry connecting to "
                    + builder.getHost() + ":" + builder.getPort() + " without tls.", throwable);
            NettyConnection ch = new NettyConnection(builder, null);
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
    public CompletableFuture<NettyConnection> create() {
        final CompletableFuture<NettyConnection> connectionFinishedFuture = new CompletableFuture<>();
        final NettyConnection nettyConnection = new NettyConnection(this.builder, this.sslContext);
        final ChannelFuture connectFuture = nettyConnection.asyncConnect();
        final Timeout timeout = INSTANCE.newTimeout(to ->
                        handleTimeout(connectFuture, connectionFinishedFuture, nettyConnection),
                builder.getConnectTimeout(), TimeUnit.MILLISECONDS);

        connectFuture.addListener(future ->
                handleConnectionComplete(future, timeout, connectionFinishedFuture, nettyConnection));

        if (!builder.isTlsFallback2Normal()) {
            return connectionFinishedFuture;
        } else {
            //Fallback to normal connection
            return connectionFinishedFuture.handle(this::connectSync);
        }
    }

    @Override
    public CompletableFuture<Void> destroy(NettyConnection connection) {
        return connection.close();
    }

    @Override
    public Boolean validate(NettyConnection connection) {
        return connection != null && connection.isActive();
    }
}
