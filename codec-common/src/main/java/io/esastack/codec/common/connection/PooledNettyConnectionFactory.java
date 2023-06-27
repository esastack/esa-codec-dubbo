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

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.exception.TslHandshakeFailedException;
import io.esastack.codec.commons.pool.PooledObjectFactory;
import io.netty.handler.ssl.SslContext;

import java.util.concurrent.CompletableFuture;

public class PooledNettyConnectionFactory implements PooledObjectFactory<NettyConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PooledNettyConnectionFactory.class);

    private final SslContext sslContext;
    private final NettyConnectionConfig connectionConfig;

    public PooledNettyConnectionFactory(NettyConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
        this.sslContext = createSslContext(connectionConfig);
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

    public NettyConnection fallback2Normal(final NettyConnection connection, final Throwable throwable) {
        if (throwable instanceof TslHandshakeFailedException) {
            LOGGER.error("TLS handle shake failed, retry connecting to "
                    + connectionConfig.getHost() + ":" + connectionConfig.getPort() + " without tls.", throwable);
            final NettyConnection ch = new NettyConnection(connectionConfig, null);
            ch.connectSync();
            return ch;
        } else if (throwable instanceof ConnectFailedException) {
            throw (ConnectFailedException) throwable;
        } else if (throwable != null) {
            throw new ConnectFailedException(throwable);
        } else {
            return connection;
        }
    }

    @Override
    public CompletableFuture<NettyConnection> create() {
        final NettyConnection connection = new NettyConnection(this.connectionConfig, this.sslContext);
        final CompletableFuture<NettyConnection> future = connection.connect().thenApply(aBoolean -> connection);
        if (!connectionConfig.isTlsFallback2Normal()) {
            return future;
        } else {
            //Fallback to normal connection
            return future.handle(this::fallback2Normal);
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

    @Override
    public String identity() {
        return connectionConfig.getAddress();
    }
}
