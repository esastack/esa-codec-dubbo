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
package io.esastack.codec.dubbo.client;

import esa.commons.StringUtils;
import esa.commons.concurrent.ThreadFactories;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.commons.pool.DefaultMultiplexPool;
import io.esastack.codec.commons.pool.MultiplexPool;
import io.esastack.codec.commons.pool.exception.AcquireFailedException;
import io.esastack.codec.dubbo.client.channel.DubboNettyChannel;
import io.esastack.codec.dubbo.client.channel.DubboNettyChannelPooledFactory;
import io.esastack.codec.dubbo.client.exception.ConnectFailedException;
import io.esastack.codec.dubbo.client.exception.RequestTimeoutException;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.DubboMessageWrapper;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.core.utils.DubboConstants;
import io.netty.channel.ChannelFuture;
import io.netty.handler.ssl.SslContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.Timer;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Netty客户端 支持异步和同步调用 使用FixedChannelPool通道连接池
 */
public class NettyDubboClient implements DubboClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyDubboClient.class);

    private static final Timer TIME_OUT_TIMER =
            new HashedWheelTimer(ThreadFactories.namedThreadFactory("esa-timeout-checker-"),
                    30, TimeUnit.MILLISECONDS);

    private final MultiplexPool<DubboNettyChannel> connectionPool;

    private final DubboClientBuilder builder;

    public NettyDubboClient(DubboClientBuilder builder) {
        this.builder = builder;
        SslContext sslContext = createSslContext();
        DubboClientBuilder.MultiplexPoolBuilder multiplexPoolBuilder = builder.getMultiplexPoolBuilder();
        //构建异步连接池
        this.connectionPool = new DefaultMultiplexPool.Builder<DubboNettyChannel>()
                .maxPoolSize(multiplexPoolBuilder.getMaxPoolSize())
                .blockCreateWhenInit(multiplexPoolBuilder.isBlockCreateWhenInit())
                .waitCreateWhenLastTryAcquire(multiplexPoolBuilder.isWaitCreateWhenLastTryAcquire())
                .maxRetryTimes(multiplexPoolBuilder.getMaxRetryTimes())
                .factory(new DubboNettyChannelPooledFactory(this.builder, sslContext))
                .init(multiplexPoolBuilder.isInit())
                .build();
    }

    @Override
    public CompletableFuture<RpcResult> sendRequestAsync(final DubboMessage request,
                                                         final Class<?> returnType) {
        return sendRequestAsync(request, returnType, returnType);
    }

    @Override
    public CompletableFuture<RpcResult> sendRequestAsync(final DubboMessage request,
                                                         final Class<?> returnType,
                                                         final Type genericReturnType) {
        return sendRequestAsync(request, returnType, genericReturnType, builder.getReadTimeout());
    }

    @Override
    public CompletableFuture<RpcResult> sendRequestAsync(final DubboMessage request,
                                                         final Class<?> returnType,
                                                         final long timeout) {
        return sendRequestAsync(request, returnType, returnType, timeout);
    }

    @Override
    public CompletableFuture<RpcResult> sendRequestAsync(final DubboMessage request,
                                                         final Class<?> returnType,
                                                         final Type genericReturnType,
                                                         final long timeout) {
        final CompletableFuture<RpcResult> cf = new CompletableFuture<>();
        sendRequest(request, new AsyncDubboResponseCallback() {

            private volatile long invocationFlushTime;

            @Override
            public void onGotConnection(boolean isSuccess, String errMsg) {

            }

            @Override
            public void onWriteToNetwork(boolean isSuccess, String errMsg) {
                invocationFlushTime = System.currentTimeMillis();
            }

            @Override
            public void onError(Throwable e) {
                cf.completeExceptionally(e);
            }

            @Override
            public void onResponse(RpcResult rpcResult) {
                rpcResult.setAttachment(
                        DubboConstants.TRACE.TIME_OF_REQ_FLUSH_KEY, String.valueOf(invocationFlushTime));
                cf.complete(rpcResult);
            }

            @Override
            public Class<?> getReturnType() {
                return returnType;
            }

            @Override
            public Type getGenericReturnType() {
                return genericReturnType;
            }
        }, timeout);

        return cf;
    }

    @Override
    public CompletableFuture<DubboMessageWrapper> sendRequestSync(DubboMessage request,
                                                                  Class<?> returnType,
                                                                  long timeout) {
        final CompletableFuture<DubboMessageWrapper> cf = new CompletableFuture<>();
        sendRequest(request, new SyncDubboResponseCallback() {

            private volatile long invocationFlushTime;

            @Override
            public void onResponse(DubboMessageWrapper messageWrapper) {
                messageWrapper.addAttachment(
                        DubboConstants.TRACE.TIME_OF_REQ_FLUSH_KEY, String.valueOf(invocationFlushTime));
                cf.complete(messageWrapper);
            }

            @Override
            public void onError(Throwable e) {
                cf.completeExceptionally(e);
            }

            @Override
            public void onGotConnection(boolean b, String errMsg) {

            }

            @Override
            public void onWriteToNetwork(boolean isSuccess, String errMsg) {
                this.invocationFlushTime = System.currentTimeMillis();
            }

            @Override
            public Class<?> getReturnType() {
                return returnType;
            }
        }, timeout);

        return cf;
    }

    private void sendRequest(DubboMessage request, DubboResponseCallback callback, long timeout) {
        try {
            CompletableFuture<DubboNettyChannel> future = this.connectionPool.acquire();
            future.whenComplete((channel, throwable) -> {
                if (throwable != null) {
                    handleRequestWhenAcquiredFailed(throwable, request, callback);
                } else {
                    handleRequestWhenAcquiredSuccess(channel, request, callback, timeout);
                }
            });
        } catch (Throwable t) {
            handleRequestWhenAcquiredFailed(t, request, callback);
        }
    }

    @Override
    public boolean isActive() {
        return connectionPool.canAcquire();
    }

    @Override
    public void close() {
        this.connectionPool.closeAll();
    }

    private void handleRequestWhenAcquiredFailed(final Throwable cause,
                                                 final DubboMessage request,
                                                 final DubboResponseCallback callback) {
        if (cause instanceof AcquireFailedException) {
            onError(request, callback, new ConnectFailedException(cause));
        } else {
            onError(request, callback, cause);
        }
    }

    private void handleRequestWhenAcquiredSuccess(final DubboNettyChannel connection,
                                                  final DubboMessage request,
                                                  final DubboResponseCallback callback,
                                                  final long timeout) {
        try {
            //回调获取到连接
            callback.onGotConnection(true, null);

            //return the connect failed cause to upper user
            if (!connection.isActive()) {
                connectionPool.close(connection);
                onError(request, callback, new ConnectFailedException("connection inactive"));
                return;
            }

            //write buffer满了，不再继续写入，否则会堆积Task导致OOM
            if (!connection.isWritable()) {
                onError(request, callback, new ConnectFailedException("Got connection which has a full write buffer"));
                return;
            }

            sendRequest(callback, connection, request, timeout);
        } catch (Throwable t) {
            onError(request, callback, t);
        }

    }

    private void sendRequest(final DubboResponseCallback callback,
                             final DubboNettyChannel connection,
                             final DubboMessage request,
                             final long timeout) {
        final long requestId = connection.getRequestIdAtomic().getAndIncrement();
        final DubboHeader header = request.getHeader();
        header.setRequestId(requestId);
        header.setRequest(true);

        if (!header.isTwoWay()) {
            // If it is oneway mode and there is no need to wait, set the callback function to success,
            // else you need to wait and send data successfully within timeout, set the callback function to success,
            // otherwise set to error
            final ChannelFuture channelFuture = connection.writeAndFlush(request);
            if (!header.isOnewayWaited()) {
                onResponseDefaultValue(callback, header);
                return;
            }
            channelFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
            if (channelFuture.isDone()) {
                if (channelFuture.isSuccess()) {
                    onResponseDefaultValue(callback, header);
                } else if (channelFuture.cause() != null) {
                    callback.onError(new ConnectFailedException("Failed to send data cause "
                            + channelFuture.cause().getMessage() + " and the exception is " + channelFuture.cause()));
                } else {
                    callback.onError(
                            new ConnectFailedException("Failed to send data because the sending was cancelled."));
                }
            } else {
                callback.onError(new RequestTimeoutException("Client sends data timeout: " + timeout + " ms."));
            }
        } else {
            final Map<Long, DubboResponseCallback> callbackMap = connection.getDubboCallbackMap();
            callbackMap.put(requestId, callback);
            final ChannelFuture channelFuture = connection.writeAndFlush(request);
            channelFuture.addListener((ChannelFuture future) ->
                    notifyWriteDone(channelFuture, requestId, callback, connection));
            TIME_OUT_TIMER.newTimeout(new ReadTimeoutListener(timeout, requestId, callbackMap, channelFuture),
                    timeout, TimeUnit.MILLISECONDS);
        }
    }

    private void notifyWriteDone(final ChannelFuture channelFuture,
                                 final long requestId,
                                 final DubboResponseCallback callback,
                                 final DubboNettyChannel connection) {
        //通知网络写入事件
        if (channelFuture.isSuccess() && callback != null) {
            callback.onWriteToNetwork(true, null);
            return;
        }

        //发送失败
        String errMsg = StringUtils.concat("write request to ", connection.getName(), " error.");
        logger.error(errMsg, channelFuture.cause());

        connection.getDubboCallbackMap().remove(requestId);
        if (callback == null) {
            return;
        }

        try {
            callback.onWriteToNetwork(false, channelFuture.cause().toString());
        } finally {
            callback.onError(new ConnectFailedException(errMsg, channelFuture.cause()));
        }
    }

    private void onError(final DubboMessage request, final DubboResponseCallback callback, final Throwable throwable) {
        callback.onError(throwable);
        ReferenceCountUtil.release(request);
    }

    private SslContext createSslContext() {
        if (builder.getDubboSslContextBuilder() != null) {
            try {
                return builder.getDubboSslContextBuilder().buildClient();
            } catch (Exception e) {
                logger.error("build tls sslContext error", e);
            }
        }
        return null;
    }

    private void onResponseDefaultValue(final DubboResponseCallback callback,
                                        final DubboHeader header) {
        if (callback instanceof SyncDubboResponseCallback) {
            RpcResult result = RpcResult.success(header.getRequestId(),
                    header.getSeriType(),
                    getDefaultValue(callback.getReturnType()));
            try {
                DubboMessage dubboMessage = ServerCodecHelper.toDubboMessage(result);
                ((SyncDubboResponseCallback) callback).onResponse(new DubboMessageWrapper(dubboMessage));
            } catch (SerializationException e) {
                logger.error("Failed to serialize default value response", e);
                callback.onError(e);
            }
        } else {
            ((AsyncDubboResponseCallback) callback).onResponse(
                    RpcResult.success(header.getRequestId(), header.getSeriType(),
                            getDefaultValue(callback.getReturnType())));
        }
    }

    public static Object getDefaultValue(final Class<?> returnType) {
        if (returnType != null && returnType.isPrimitive()) {
            if (returnType == byte.class) {
                return (byte) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == char.class) {
                return Character.MIN_VALUE;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == float.class) {
                return 0.0f;
            }
            if (returnType == double.class) {
                return 0.0d;
            }
        }
        return null;
    }
}
