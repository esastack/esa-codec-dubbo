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
package io.esastack.codec.common.server;

import esa.commons.StringUtils;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

public abstract class NettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);

    private final EventLoopGroup bossEventLoopGroup;
    private final EventLoopGroup ioEventLoopGroup;
    private final NettyServerConfig serverConfig;

    public NettyServer(final NettyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.serverConfig.setConnectionInitializer(createConnectionInitializer(serverConfig));
        final int bossThreadCount = serverConfig.getBossThreads() > 0 ? serverConfig.getBossThreads() : 1;
        final int workerThreadCount = serverConfig.getIoThreads() > 0
                ? serverConfig.getIoThreads() : Runtime.getRuntime().availableProcessors();
        bossEventLoopGroup = Epoll.isAvailable()
                ? new EpollEventLoopGroup(bossThreadCount, new DefaultThreadFactory("Netty-Epoll-Boss")) :
                new NioEventLoopGroup(bossThreadCount, new DefaultThreadFactory("Netty-Nio-Boss"));
        ioEventLoopGroup = Epoll.isAvailable()
                ? new EpollEventLoopGroup(workerThreadCount, new DefaultThreadFactory("Netty-Epoll-I/O")) :
                new NioEventLoopGroup(workerThreadCount, new DefaultThreadFactory("Netty-Nio-I/O"));
    }

    @SuppressWarnings("unchecked")
    public void start() {
        LOGGER.info("Starting netty dubbo server,settings:" + this.serverConfig);
        final SocketAddress socketAddress;
        final Class<? extends ServerChannel> serverChannelClass;
        if (!StringUtils.isEmpty(serverConfig.getUnixDomainSocketFile())) {
            serverChannelClass = EpollServerDomainSocketChannel.class;
            socketAddress = new DomainSocketAddress(serverConfig.getUnixDomainSocketFile());
        } else if (Epoll.isAvailable()) {
            serverChannelClass = EpollServerSocketChannel.class;
            socketAddress = new InetSocketAddress(serverConfig.getBindIp(), serverConfig.getPort());
        } else {
            serverChannelClass = NioServerSocketChannel.class;
            socketAddress = new InetSocketAddress(serverConfig.getBindIp(), serverConfig.getPort());
        }

        final String bindAddr = serverConfig.getBindIp() + ":" + serverConfig.getPort();
        try {
            final ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossEventLoopGroup, ioEventLoopGroup)
                    .channel(serverChannelClass);
            for (Map.Entry<ChannelOption, Object> entry : serverConfig.getChannelOptions().entrySet()) {
                serverBootstrap.option(entry.getKey(), entry.getValue());
            }
            //option主要是针对boss线程组，childOption主要是针对worker线程组
            for (Map.Entry<ChannelOption, Object> entry : serverConfig.getChildChannelOptions().entrySet()) {
                serverBootstrap.childOption(entry.getKey(), entry.getValue());
            }
            serverBootstrap.childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel sh) {
                    sh.pipeline().addLast(serverConfig.getChannelHandlers().toArray(new ChannelHandler[0]));
                    serverConfig.getConnectionInitializer().initialize(sh);
                }
            }).bind(socketAddress).sync().channel().closeFuture().addListener(f ->
                    LOGGER.info("Dubbo server is closed: " + bindAddr));
            LOGGER.info("Dubbo server is listening on: " + bindAddr);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to start dubbo server on: " + bindAddr, t);
        }
    }

    public void shutdown() {
        //停止接受新的连接
        stopAcceptNewConnection();
        //关闭线程池
        shutdown0();
        //停止网络读写
        stopNetworkReadAndWrite();
        LOGGER.info("*********************Netty Dubbo Server[" + serverConfig.getBindIp() + ":" +
                serverConfig.getPort() + "] closed!***************************************");
    }

    protected SslContext createSslContext(final NettyServerConfig serverConfig) throws IOException {
        if (serverConfig.getSslContextBuilder() == null) {
            LOGGER.info("Dubbo server does not enable SSL encryption");
            return null;
        }
        LOGGER.info("Dubbo server enabled SSL encryption");
        return serverConfig.getSslContextBuilder().buildServer();
    }

    protected abstract void shutdown0();

    protected abstract ServerConnectionInitializer createConnectionInitializer(final NettyServerConfig serverConfig);

    private void stopAcceptNewConnection() {
        LOGGER.info("*********************Netty Http Server[" + serverConfig.getBindIp() + ":" +
                serverConfig.getPort() + "] stopAcceptNewConnection***********************");
        if (bossEventLoopGroup != null) {
            bossEventLoopGroup.shutdownGracefully();
        }
    }

    private void stopNetworkReadAndWrite() {
        LOGGER.info("*********************Netty Http Server[" + serverConfig.getBindIp() + ":" +
                serverConfig.getPort() + "] stopNetworkReadAndWrite***********************");
        if (ioEventLoopGroup != null) {
            ioEventLoopGroup.shutdownGracefully();
        }
    }
}
