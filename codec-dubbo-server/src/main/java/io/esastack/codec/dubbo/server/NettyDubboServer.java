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
package io.esastack.codec.dubbo.server;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.common.connection.ConnectionInitializer;
import io.esastack.codec.common.server.NettyServer;
import io.esastack.codec.common.server.NettyServerConfig;
import io.esastack.codec.common.server.ServerConnectionInitializer;
import io.esastack.codec.dubbo.server.handler.DubboServerBizHandler;
import io.esastack.codec.dubbo.server.handler.TelnetDetectHandler;
import io.esastack.codec.dubbo.server.handler.TlsDetectHandler;
import io.netty.handler.ssl.SslContext;

import java.io.IOException;

public class NettyDubboServer extends NettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyDubboServer.class);

    private final DubboServerBuilder builder;

    public NettyDubboServer(final DubboServerBuilder builder) {
        super(builder.getServerConfig());
        this.builder = builder;
    }

    public static DubboServerBuilder newBuilder() {
        return new DubboServerBuilder();
    }

    @Override
    protected ServerConnectionInitializer createConnectionInitializer(final NettyServerConfig serverConfig) {
        final SslContext sslContext;
        try {
            sslContext = createSslContext(serverConfig);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to create ssl context.", e);
        }
        return channel -> {
            //if tls enabled, telnet is not supported, user can not construct encrypted message, and we
            //cannot distinguish tls and telnet protocol
            if (sslContext != null) {
                channel.pipeline().addLast(new TlsDetectHandler(builder, sslContext));
            } else {
                channel.pipeline().addLast(new TelnetDetectHandler(builder));
            }
        };
    }

    @Override
    protected void shutdown0() {
        //阻塞等待业务线程停止
        final DubboServerBizHandler handler = builder.getBizHandler();
        if (handler != null) {
            LOGGER.info("*********************Netty Dubbo Server[" + builder.getServerConfig().getBindIp() + ":" +
                    builder.getServerConfig().getPort() + "] stopHandler***********************************");
            handler.shutdown();
        }
    }
}
