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

import esa.commons.StringUtils;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.dubbo.client.DubboClientBuilder;
import io.esastack.codec.dubbo.client.DubboResponseCallback;
import io.esastack.codec.dubbo.client.exception.ConnectFailedException;
import io.esastack.codec.dubbo.client.handler.DubboClientHandler;
import io.esastack.codec.dubbo.client.utils.NettyConstants;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.DubboMessageDecoder;
import io.esastack.codec.dubbo.core.codec.DubboMessageEncoder;
import io.esastack.codec.dubbo.core.codec.TTFBLengthFieldBasedFrameDecoder;
import io.esastack.codec.dubbo.core.utils.NettyUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 构建客户端Channel
 */
public class DubboNettyChannel {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboNettyChannel.class);

    /**
     * 连接编号
     */
    private static final AtomicInteger CONNECT_NUMBER = new AtomicInteger(0);
    /**
     * 网络I/O线程池
     */
    private static final EventLoopGroup GROUP;

    static {
        int threads = Math.min(10, Runtime.getRuntime().availableProcessors());
        if (Epoll.isAvailable()) {
            GROUP = new EpollEventLoopGroup(threads,
                    new DefaultThreadFactory("NettyClient-Epoll-I/O", true));
        } else {
            GROUP = new NioEventLoopGroup(threads,
                    new DefaultThreadFactory("NettyClient-Nio-I/O", true));
        }
    }

    /**
     * 请求唯一ID 独立的 不参与共享
     */
    private final AtomicLong requestIdAtomic;
    /**
     * 请求唯一ID和异步回调函数进行映射
     */
    private final Map<Long, DubboResponseCallback> callbackMap;
    /**
     * DubboClient配置参数
     */
    private final DubboClientBuilder builder;

    private final SslContext sslContext;
    /**
     * 连接名称
     */
    private volatile String connectionName;
    /**
     * Netty网络通道
     */
    private volatile Channel channel;
    /**
     * Future of the TSL handshake; This is assigned when the channel is initialized, at this time this channel is not
     * connected, all the listeners are not executed.
     */
    private volatile Future<Channel> tslHandshakeFuture;

    public DubboNettyChannel(DubboClientBuilder builder, SslContext sslContext) {
        this.builder = builder;
        this.sslContext = sslContext;
        // capacity suggest to set small number because easy to get oom and low tps
        this.callbackMap = new ConcurrentHashMap<>(16);
        this.requestIdAtomic = new AtomicLong(0);
    }

    public void connect() {
        this.connectionName = StringUtils.concat("connect#",
                String.valueOf(CONNECT_NUMBER.getAndIncrement()),
                "[",
                builder.getHost(),
                ":",
                String.valueOf(builder.getPort()),
                "]");

        Bootstrap bootstrap = newBootStrap();
        ChannelFuture channelFuture = bootstrap.connect();
        boolean ret = channelFuture.awaitUninterruptibly(builder.getConnectTimeout(), TimeUnit.MILLISECONDS);
        if (ret && channelFuture.isSuccess()) {
            this.channel = channelFuture.channel();
            return;
        }

        Throwable cause = channelFuture.cause();
        if (cause != null) {
            throw new ConnectFailedException(cause);
        }
        throw new ConnectFailedException("Client connect to the " +
                builder.getHost() + ":" + builder.getPort() + " timeout.");
    }

    public ChannelFuture asyncConnect() {
        this.connectionName = StringUtils.concat("connect#",
                String.valueOf(CONNECT_NUMBER.getAndIncrement()),
                "[",
                builder.getHost(),
                ":",
                String.valueOf(builder.getPort()),
                "]");

        Bootstrap bootstrap = newBootStrap();
        ChannelFuture channelFuture = bootstrap.connect();
        this.channel = channelFuture.channel();
        return channelFuture;
    }

    public boolean isActive() {
        return this.channel != null &&
                this.channel.isActive() &&
                (this.tslHandshakeFuture == null || this.tslHandshakeFuture.isSuccess());
    }

    public CompletableFuture<Void> close() {
        if (channel == null) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> closeFuture = new CompletableFuture<>();
        ChannelFuture channelFuture = this.channel.close();
        if (channelFuture.isDone()) {
            if (channelFuture.isSuccess()) {
                closeFuture.complete(null);
            } else {
                closeFuture.completeExceptionally(channelFuture.cause());
            }

            return closeFuture;
        }

        channelFuture.addListener((future -> {
            if (future.isSuccess()) {
                closeFuture.complete(null);
            } else {
                closeFuture.completeExceptionally(future.cause());
            }
        }));

        return closeFuture;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Bootstrap newBootStrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(GROUP);

        if (Epoll.isAvailable() && !StringUtils.isEmpty(builder.getUnixDomainSocketFile())) {
            bootstrap.remoteAddress(new DomainSocketAddress(builder.getUnixDomainSocketFile()));
            bootstrap.channel(EpollDomainSocketChannel.class);
        } else if (Epoll.isAvailable()) {
            bootstrap.remoteAddress(new InetSocketAddress(builder.getHost(), builder.getPort()));
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.remoteAddress(new InetSocketAddress(builder.getHost(), builder.getPort()));
            bootstrap.channel(NioSocketChannel.class);
        }
        for (Map.Entry<ChannelOption, Object> entry : builder.getChannelOptions().entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                if (builder.getWriteBufferHighWaterMark() > 0) {
                    ch.config().setWriteBufferHighWaterMark(builder.getWriteBufferHighWaterMark());
                    ch.config().setWriteBufferLowWaterMark(builder.getWriteBufferHighWaterMark() / 2);
                }

                //将连接名称放到channel附加属性中
                NettyUtils.setChannelAttr(ch, NettyConstants.CHANNEL_ATTR_KEY.CONNECTION_NAME, connectionName);
                //添加SSL Handler
                if (sslContext != null) {
                    SslHandler sslHandler = new SslHandler(sslContext.newEngine(ch.alloc()));
                    sslHandler.setHandshakeTimeoutMillis(Math.min(builder.getConnectTimeout(),
                            builder.getDubboSslContextBuilder().getHandshakeTimeoutMillis()));
                    DubboNettyChannel.this.tslHandshakeFuture = sslHandler.handshakeFuture();
                    ch.pipeline().addLast(sslHandler);
                }
                //添加协议处理Handler
                ch.pipeline().addLast(new DubboMessageEncoder());
                ch.pipeline().addLast(new TTFBLengthFieldBasedFrameDecoder(builder.getPayload(),
                        12, 4, 0, 0));
                ch.pipeline().addLast(new DubboMessageDecoder());
                ch.pipeline().addLast(new IdleStateHandler(builder.getHeartbeatTimeoutSeconds(),
                        0, 0));
                ch.pipeline().addLast(new DubboClientHandler(connectionName, callbackMap));
                ch.pipeline().addLast(builder.getChannelHandlers().toArray(new ChannelHandler[0]));
                builder.getHandlerSuppliers().forEach((supplier -> ch.pipeline().addLast(supplier.get())));

                //打印连接、关闭连接调试信息
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(StringUtils.concat(connectionName, " connected."));
                }
            }
        });

        return bootstrap;
    }

    public boolean isWritable() {
        return channel.isWritable();
    }

    public AtomicLong getRequestIdAtomic() {
        return this.requestIdAtomic;
    }

    public Map<Long, DubboResponseCallback> getDubboCallbackMap() {
        return this.callbackMap;
    }

    public ChannelFuture writeAndFlush(DubboMessage request) {
        return channel.writeAndFlush(request);
    }

    public String getName() {
        return this.connectionName;
    }

    public Future<Channel> getTslHandshakeFuture() {
        return tslHandshakeFuture;
    }

    public Channel getChannel() {
        return channel;
    }

    void setTslHandshakeFuture(final Future<Channel> tslHandshakeFuture) {
        this.tslHandshakeFuture = tslHandshakeFuture;
    }

    void setChannel(final Channel channel) {
        this.channel = channel;
    }
}
