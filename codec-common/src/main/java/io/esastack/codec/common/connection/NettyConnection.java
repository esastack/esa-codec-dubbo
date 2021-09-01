package io.esastack.codec.common.connection;

import esa.commons.StringUtils;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.common.constant.Constants;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.utils.NettyUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
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
public class NettyConnection {

    private static final EventLoopGroup GROUP;
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyConnection.class);
    private static final AtomicInteger CONNECT_NUMBER = new AtomicInteger(0);

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

    private final SslContext sslContext;
    private final AtomicLong requestIdAtomic;
    private final NettyConnectionConfig connectionConfig;
    private final Map<Long, ResponseCallback> callbackMap;
    private volatile Channel channel;
    private volatile String connectionName;
    /**
     * Future of the TSL handshake; This is assigned when the channel is initialized, at this time this channel is not
     * connected, all the listeners are not executed.
     */
    private volatile Future<Channel> tslHandshakeFuture;

    public NettyConnection(NettyConnectionConfig connectionConfig, SslContext sslContext) {
        this.connectionConfig = connectionConfig;
        this.sslContext = sslContext;
        // capacity suggest to set small number because easy to get oom and low tps
        this.callbackMap = new ConcurrentHashMap<>(16);
        this.requestIdAtomic = new AtomicLong(0);
    }

    public void connect() {
        this.connectionName = StringUtils.concat("connect#",
                String.valueOf(CONNECT_NUMBER.getAndIncrement()),
                "[",
                connectionConfig.getHost(),
                ":",
                String.valueOf(connectionConfig.getPort()),
                "]");

        Bootstrap bootstrap = newBootStrap();
        ChannelFuture channelFuture = bootstrap.connect();
        boolean ret = channelFuture.awaitUninterruptibly(connectionConfig.getConnectTimeout(), TimeUnit.MILLISECONDS);
        if (ret && channelFuture.isSuccess()) {
            this.channel = channelFuture.channel();
            return;
        }

        Throwable cause = channelFuture.cause();
        if (cause != null) {
            throw new ConnectFailedException(cause);
        }
        throw new ConnectFailedException("Client connect to the " +
                connectionConfig.getHost() + ":" + connectionConfig.getPort() + " timeout.");
    }

    public ChannelFuture asyncConnect() {
        this.connectionName = StringUtils.concat("connect#",
                String.valueOf(CONNECT_NUMBER.getAndIncrement()),
                "[",
                connectionConfig.getHost(),
                ":",
                String.valueOf(connectionConfig.getPort()),
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

        if (Epoll.isAvailable() && !StringUtils.isEmpty(connectionConfig.getUnixDomainSocketFile())) {
            bootstrap.remoteAddress(new DomainSocketAddress(connectionConfig.getUnixDomainSocketFile()));
            bootstrap.channel(EpollDomainSocketChannel.class);
        } else if (Epoll.isAvailable()) {
            bootstrap.remoteAddress(new InetSocketAddress(connectionConfig.getHost(), connectionConfig.getPort()));
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.remoteAddress(new InetSocketAddress(connectionConfig.getHost(), connectionConfig.getPort()));
            bootstrap.channel(NioSocketChannel.class);
        }
        for (Map.Entry<ChannelOption, Object> entry : connectionConfig.getChannelOptions().entrySet()) {
            bootstrap.option(entry.getKey(), entry.getValue());
        }
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                if (connectionConfig.getWriteBufferHighWaterMark() > 0) {
                    ch.config().setWriteBufferHighWaterMark(connectionConfig.getWriteBufferHighWaterMark());
                    ch.config().setWriteBufferLowWaterMark(connectionConfig.getWriteBufferHighWaterMark() / 2);
                }

                //将连接名称放到channel附加属性中
                NettyUtils.setChannelAttr(ch, Constants.CHANNEL_ATTR_KEY.CONNECTION_NAME, connectionName);
                //添加SSL Handler
                if (sslContext != null) {
                    SslHandler sslHandler = new SslHandler(sslContext.newEngine(ch.alloc()));
                    sslHandler.setHandshakeTimeoutMillis(Math.min(connectionConfig.getConnectTimeout(),
                            connectionConfig.getSslContextBuilder().getHandshakeTimeoutMillis()));
                    NettyConnection.this.tslHandshakeFuture = sslHandler.handshakeFuture();
                    ch.pipeline().addLast(sslHandler);
                }
                ch.pipeline().addLast(connectionConfig.getChannelHandlers().toArray(new ChannelHandler[0]));
                if (connectionConfig.getConnectionInitializer() != null) {
                    connectionConfig.getConnectionInitializer().initialize(ch, connectionName, callbackMap);
                } else {
                    LOGGER.warn("No connectionInitializer configured for " + connectionName);
                }

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

    public Map<Long, ResponseCallback> getCallbackMap() {
        return this.callbackMap;
    }

    public ChannelFuture writeAndFlush(Object request) {
        return channel.writeAndFlush(request);
    }

    public String getName() {
        return this.connectionName;
    }

    public Future<Channel> getTslHandshakeFuture() {
        return tslHandshakeFuture;
    }

    public void setTslHandshakeFuture(final Future<Channel> tslHandshakeFuture) {
        this.tslHandshakeFuture = tslHandshakeFuture;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }
}
