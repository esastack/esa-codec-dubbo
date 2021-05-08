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

import esa.commons.StringUtils;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.dubbo.server.handler.DubboServerBizHandler;
import io.esastack.codec.dubbo.server.handler.TelnetDetectHandler;
import io.esastack.codec.dubbo.server.handler.TlsDetectHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

public class NettyDubboServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyDubboServer.class);

    private final EventLoopGroup bossEventLoopGroup;

    private final EventLoopGroup ioEventLoopGroup;

    private final DubboServerBuilder builder;

    public NettyDubboServer(DubboServerBuilder builder) {
        this.builder = builder;
        final int bossThreadCount = builder.getBossThreads() > 0 ? builder.getBossThreads() : 1;
        final int workerThreadCount = builder.getIoThreads() > 0 ? builder.getIoThreads()
                : Runtime.getRuntime().availableProcessors();
        bossEventLoopGroup = Epoll.isAvailable()
                ? new EpollEventLoopGroup(bossThreadCount, new DefaultThreadFactory("Netty-Epoll-Boss")) :
                new NioEventLoopGroup(bossThreadCount, new DefaultThreadFactory("Netty-Nio-Boss"));
        ioEventLoopGroup = Epoll.isAvailable()
                ? new EpollEventLoopGroup(workerThreadCount, new DefaultThreadFactory("Netty-Epoll-I/O")) :
                new NioEventLoopGroup(workerThreadCount, new DefaultThreadFactory("Netty-Nio-I/O"));
    }

    public static DubboServerBuilder newBuilder() {
        return new DubboServerBuilder();
    }

    @SuppressWarnings("unchecked")
    public void start() {
        logger.info("Starting netty dubbo server,settings:" + this.builder);
        final SocketAddress socketAddress;
        final Class<? extends ServerChannel> serverChannelClass;
        if (!StringUtils.isEmpty(builder.getUnixDomainSocketFile())) {
            serverChannelClass = EpollServerDomainSocketChannel.class;
            socketAddress = new DomainSocketAddress(builder.getUnixDomainSocketFile());
        } else if (Epoll.isAvailable()) {
            serverChannelClass = EpollServerSocketChannel.class;
            socketAddress = new InetSocketAddress(builder.getBindIp(), builder.getPort());
        } else {
            serverChannelClass = NioServerSocketChannel.class;
            socketAddress = new InetSocketAddress(builder.getBindIp(), builder.getPort());
        }

        String bindAddr = builder.getBindIp() + ":" + builder.getPort();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossEventLoopGroup, ioEventLoopGroup)
                    .channel(serverChannelClass);
            for (Map.Entry<ChannelOption, Object> entry : builder.getChannelOptions().entrySet()) {
                serverBootstrap.option(entry.getKey(), entry.getValue());
            }
            //option主要是针对boss线程组，childOption主要是针对worker线程组
            for (Map.Entry<ChannelOption, Object> entry : builder.getChildChannelOptions().entrySet()) {
                serverBootstrap.childOption(entry.getKey(), entry.getValue());
            }

            SslContext sslContext;
            if (builder.getDubboSslContextBuilder() == null) {
                sslContext = null;
                logger.info("Dubbo server does not enable SSL encryption");
            } else {
                sslContext = builder.getDubboSslContextBuilder().buildServer();
                logger.info("Dubbo server enabled SSL encryption");
            }

            serverBootstrap.childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel sh) {
                    //if tls enabled, telnet is not supported, user can not construct encrypted message, and we
                    //cannot distinguish tls and telnet protocol
                    if (sslContext != null) {
                        sh.pipeline().addLast(new TlsDetectHandler(builder, sslContext));
                    } else {
                        sh.pipeline().addLast(new TelnetDetectHandler(builder));
                    }
                }
            }).bind(socketAddress).sync().channel().closeFuture().addListener(f ->
                            logger.info("Dubbo server is closed: " + bindAddr));
            logger.info("Dubbo server is listening on: " + bindAddr);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to start dubbo server on: " + bindAddr, t);
        }
    }

    public void shutdown() {
        //停止接受新的连接
        stopAcceptNewConnection();

        //阻塞等待业务线程停止
        DubboServerBizHandler handler = builder.getBizHandler();
        if (handler != null) {
            logger.info("*********************Netty Dubbo Server[" + builder.getBindIp() + ":" +
                    builder.getPort() + "] stopHandler***********************************");
            handler.shutdown();
        }

        //停止网络读写
        stopNetworkReadAndWrite();

        logger.info("*********************Netty Dubbo Server[" + builder.getBindIp() + ":" +
                builder.getPort() + "] closed!***************************************");
    }

    private void stopAcceptNewConnection() {
        logger.info("*********************Netty Http Server[" + builder.getBindIp() + ":" +
                builder.getPort() + "] stopAcceptNewConnection***********************");
        if (bossEventLoopGroup != null) {
            bossEventLoopGroup.shutdownGracefully();
        }
    }

    private void stopNetworkReadAndWrite() {
        logger.info("*********************Netty Http Server[" + builder.getBindIp() + ":" +
                builder.getPort() + "] stopNetworkReadAndWrite***********************");
        if (ioEventLoopGroup != null) {
            ioEventLoopGroup.shutdownGracefully();
        }
    }
}
