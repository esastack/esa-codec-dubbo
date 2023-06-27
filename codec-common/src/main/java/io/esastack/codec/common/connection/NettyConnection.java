package io.esastack.codec.common.connection;

import esa.commons.StringUtils;
import esa.commons.concurrent.ThreadFactories;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.common.constant.Constants;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.exception.TslHandshakeFailedException;
import io.esastack.codec.common.ssl.SslUtils;
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
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Connection based on netty Channel
 */
public class NettyConnection {

    private static final EventLoopGroup GROUP;
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyConnection.class);
    private static final AtomicInteger CONNECT_NUMBER = new AtomicInteger(0);
    private static final ThreadFactory THREAD_FACTORY =
            ThreadFactories.namedThreadFactory("DubboConnect-Timer-", true);
    private static final Timer INSTANCE = new HashedWheelTimer(THREAD_FACTORY);

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
    private final CompletableFuture<Boolean> completedFuture;
    private volatile Channel channel;
    private volatile String connectionName;
    private volatile Timeout connectTimeout;
    /**
     * Future of the TLS handshake; This is assigned when the channel is initialized, at this time this channel is not
     * connected, all the listeners are not executed.
     */
    private volatile Future<Channel> tlsHandshakeFuture;

    public NettyConnection(NettyConnectionConfig connectionConfig, SslContext sslContext) {
        this.connectionConfig = connectionConfig;
        this.sslContext = sslContext;
        // capacity suggest to set small number because easy to get oom and low tps
        this.callbackMap = new ConcurrentHashMap<>(16);
        this.requestIdAtomic = new AtomicLong(0);
        this.completedFuture = new CompletableFuture<>();
    }

    public void connectSync() {
        final CompletableFuture<Boolean> f = connect();
        try {
            f.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ConnectFailedException(cause);
        } catch (Throwable e) {
            throw new ConnectFailedException(e);
        }
    }

    public CompletableFuture<Boolean> connect() {
        LOGGER.info("Connecting to: " + connectionConfig.getAddress());
        this.connectionName = StringUtils.concat("connect#",
                String.valueOf(CONNECT_NUMBER.getAndIncrement()),
                "[",
                connectionConfig.getAddress(),
                "]");

        final Bootstrap bootstrap = newBootStrap();
        final ChannelFuture connectFuture = bootstrap.connect();
        this.channel = connectFuture.channel();
        this.connectTimeout = INSTANCE.newTimeout(
                to -> handleTimeout(connectFuture), connectionConfig.getConnectTimeout(), TimeUnit.MILLISECONDS);
        //The listener is executed before the ChannelHandlers, so the attr set by the listener would been seen by
        //all the handlers.
        connectFuture.addListener(future -> {
            if (!future.isSuccess()) {
                handleConnectFailure(future);
            } else {
                //NO active event handling, success should be handled in Last ChannelHandler.
                //Just set the attr, to make it been seen at channel handler.
                ConnUtil.setConnectionAttr(channel, this);
            }
        });
        return this.completedFuture;
    }

    public void handleTimeout(final ChannelFuture channelFuture) {
        if (completedFuture.isDone()) {
            return;
        }

        final String address = connectionConfig.getAddress();
        if (!channelFuture.isDone()) {
            final String errMsg = "Client connect to the " + address + " timeout.";
            LOGGER.info(errMsg);
            completedFuture.completeExceptionally(new ConnectFailedException(errMsg));
            //This is executed in another thread, tlsHandshakeFuture may be null at the critical time
        } else if (sslContext != null && (tlsHandshakeFuture == null || !tlsHandshakeFuture.isDone())) {
            final String errMsg = "Client TSL handshake with " + address + " timeout.";
            LOGGER.info(errMsg);
            completedFuture.completeExceptionally(new TslHandshakeFailedException(errMsg));
        }
        close();
    }

    public void handleConnectFailure(final Future future) {
        close();
        connectTimeout.cancel();
        final String errMsg = "Client connect to the " +
                connectionConfig.getHost() +
                ":" +
                connectionConfig.getPort() +
                " failure.";
        LOGGER.info(errMsg);
        completedFuture.completeExceptionally(new ConnectFailedException(errMsg, future.cause()));
    }

    public void handleConnectActive() {
        if (completedFuture.isDone()) {
            //already timeout yet
            return;
        }

        if (tlsHandshakeFuture != null) {
            /*
             * TSL handshake future is set when init the Channel; because the init processing is async,
             * so it may be null at the create time, and because the connect processing is after the init
             * processing, so when the listeners are called, the init processing is completed, so the tls
             * handshake future is definitely assigned.
             */
            tlsHandshakeFuture.addListener(f -> handleTlsComplete());
        } else {
            connectTimeout.cancel();
            completedFuture.complete(true);
            LOGGER.info("Client connect to the " + connectionConfig.getAddress() + " success.");
        }
    }

    void handleTlsComplete() {
        connectTimeout.cancel();
        if (tlsHandshakeFuture.isSuccess()) {
            //save TLS certificate
            SslUtils.extractSslPeerCertificate(channel);
            completedFuture.complete(true);
            LOGGER.info("Client TSL handshake with the " + connectionConfig.getHost() + ":" +
                    connectionConfig.getPort() + " success.");
        } else {
            close();
            final String errMsg = "Client TSL handshake with the " + connectionConfig.getHost() + ":" +
                    connectionConfig.getPort() + " failure.";
            LOGGER.info(errMsg);
            completedFuture.completeExceptionally(new TslHandshakeFailedException(errMsg, tlsHandshakeFuture.cause()));
        }
    }

    public boolean isActive() {
        return this.channel != null &&
                this.channel.isActive() &&
                (this.tlsHandshakeFuture == null || this.tlsHandshakeFuture.isSuccess());
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
                    NettyConnection.this.tlsHandshakeFuture = sslHandler.handshakeFuture();
                    ch.pipeline().addLast(sslHandler);
                }
                ch.pipeline().addLast(connectionConfig.getChannelHandlers().toArray(new ChannelHandler[0]));
                if (connectionConfig.getConnectionInitializer() != null) {
                    connectionConfig.getConnectionInitializer().initialize(ch, connectionName, callbackMap);
                } else {
                    LOGGER.warn("No connectionInitializer configured for " + connectionName);
                }
                ch.pipeline().addLast(ConnectionActiveHandler.INSTANCE);

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

    public Future<Channel> getTlsHandshakeFuture() {
        return tlsHandshakeFuture;
    }

    public void setTlsHandshakeFuture(final Future<Channel> tlsHandshakeFuture) {
        this.tlsHandshakeFuture = tlsHandshakeFuture;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(final Channel channel) {
        this.channel = channel;
    }

    public CompletableFuture<Boolean> getCompletedFuture() {
        return completedFuture;
    }
}
