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

import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.common.client.NettyClient;
import io.esastack.codec.common.client.ReadTimeoutListener;
import io.esastack.codec.common.connection.ConnectionInitializer;
import io.esastack.codec.common.connection.NettyConnection;
import io.esastack.codec.common.connection.NettyConnectionConfig;
import io.esastack.codec.common.constant.Constants;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.exception.RequestTimeoutException;
import io.esastack.codec.commons.pool.exception.AcquireFailedException;
import io.esastack.codec.dubbo.client.handler.DubboClientHandler;
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.DubboMessageDecoder;
import io.esastack.codec.dubbo.core.codec.DubboMessageEncoder;
import io.esastack.codec.dubbo.core.codec.DubboMessageWrapper;
import io.esastack.codec.dubbo.core.codec.TTFBLengthFieldBasedFrameDecoder;
import io.netty.channel.ChannelFuture;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NettyDubboClient extends NettyClient implements DubboClient {

    private final DubboClientBuilder builder;

    public NettyDubboClient(final DubboClientBuilder builder) {
        super(builder.getConnectionConfig());
        this.builder = builder;
    }

    public static DubboClientBuilder newBuilder() {
        return new DubboClientBuilder();
    }

    @Override
    protected ConnectionInitializer createConnectionInitializer(final NettyConnectionConfig connectionConfig) {
        return (channel, connectionName, callbackMap) -> {
            channel.pipeline().addLast(new DubboMessageEncoder());
            channel.pipeline().addLast(new TTFBLengthFieldBasedFrameDecoder(
                    connectionConfig.getPayload(), 12, 4, 0, 0));
            channel.pipeline().addLast(new DubboMessageDecoder());
            channel.pipeline().addLast(new IdleStateHandler(
                    connectionConfig.getHeartbeatTimeoutSeconds(), 0, 0));
            channel.pipeline().addLast(new DubboClientHandler(connectionName, callbackMap));
        };
    }

    @Override
    public CompletableFuture<DubboRpcResult> sendRequest(final DubboMessage request,
                                                         final Class<?> returnType) {
        return sendRequest(request, returnType, returnType);
    }

    @Override
    public CompletableFuture<DubboRpcResult> sendRequest(final DubboMessage request,
                                                         final Class<?> returnType,
                                                         final Type genericReturnType) {
        return sendRequest(request, returnType, genericReturnType, builder.getReadTimeout());
    }

    @Override
    public CompletableFuture<DubboRpcResult> sendRequest(final DubboMessage request,
                                                         final Class<?> returnType,
                                                         final long timeout) {
        return sendRequest(request, returnType, returnType, timeout);
    }

    @Override
    public CompletableFuture<DubboRpcResult> sendRequest(final DubboMessage request,
                                                         final Class<?> returnType,
                                                         final Type genericReturnType,
                                                         final long timeout) {
        final CompletableFuture<DubboRpcResult> cf = new CompletableFuture<>();
        sendRequest(request, new ResponseCallback() {

            private volatile long invocationFlushTime;

            @Override
            public boolean deserialized() {
                return true;
            }

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
            public void onResponse(Object result) {
                final DubboRpcResult rpcResult = (DubboRpcResult) result;
                rpcResult.setAttachment(
                        Constants.TRACE.TIME_OF_REQ_FLUSH_KEY, String.valueOf(invocationFlushTime));
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
    public CompletableFuture<DubboMessageWrapper> sendReqWithoutRespDeserialize(DubboMessage request,
                                                                                Class<?> returnType,
                                                                                long timeout) {
        final CompletableFuture<DubboMessageWrapper> cf = new CompletableFuture<>();
        sendRequest(request, new ResponseCallback() {

            private volatile long invocationFlushTime;

            @Override
            public boolean deserialized() {
                return false;
            }

            @Override
            public void onResponse(Object result) {
                final DubboMessageWrapper messageWrapper = (DubboMessageWrapper) result;
                messageWrapper.addAttachment(
                        Constants.TRACE.TIME_OF_REQ_FLUSH_KEY, String.valueOf(invocationFlushTime));
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

    private void sendRequest(DubboMessage request, ResponseCallback callback, long timeout) {
        try {
            CompletableFuture<NettyConnection> future = this.connectionPool.acquire();
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
                                                 final ResponseCallback callback) {
        if (cause instanceof AcquireFailedException) {
            onError(request, callback, new ConnectFailedException(cause));
        } else {
            onError(request, callback, cause);
        }
    }

    private void handleRequestWhenAcquiredSuccess(final NettyConnection connection,
                                                  final DubboMessage request,
                                                  final ResponseCallback callback,
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

    private void sendRequest(final ResponseCallback callback,
                             final NettyConnection connection,
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
                onResponseNullValue(callback, header);
                return;
            }
            channelFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
            if (channelFuture.isDone()) {
                if (channelFuture.isSuccess()) {
                    onResponseNullValue(callback, header);
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
            final Map<Long, ResponseCallback> callbackMap = connection.getCallbackMap();
            callbackMap.put(requestId, callback);
            final ChannelFuture channelFuture = connection.writeAndFlush(request);
            channelFuture.addListener((ChannelFuture future) ->
                    notifyWriteDone(channelFuture, requestId, callback, connection));
            addTimeoutTask(new ReadTimeoutListener(timeout, requestId, callbackMap, channelFuture), timeout);
        }
    }

    private void onError(final DubboMessage request, final ResponseCallback callback, final Throwable throwable) {
        callback.onError(throwable);
        ReferenceCountUtil.release(request);
    }

    private void onResponseNullValue(final ResponseCallback callback, final DubboHeader header) {
        callback.onResponse(DubboRpcResult.success(header.getRequestId(), header.getSeriType(), null));
    }
}
