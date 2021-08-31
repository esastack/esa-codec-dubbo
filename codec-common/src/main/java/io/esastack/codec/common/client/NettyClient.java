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
package io.esastack.codec.common.client;

import esa.commons.StringUtils;
import esa.commons.concurrent.ThreadFactories;
import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.common.connection.ConnectionInitializer;
import io.esastack.codec.common.connection.NettyConnection;
import io.esastack.codec.common.connection.NettyConnectionConfig;
import io.esastack.codec.common.connection.NettyConnectionConfig.MultiplexPoolBuilder;
import io.esastack.codec.common.connection.PooledNettyConnectionFactory;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.commons.pool.DefaultMultiplexPool;
import io.esastack.codec.commons.pool.MultiplexPool;
import io.netty.channel.ChannelFuture;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

public abstract class NettyClient {

    private static final Timer TIME_OUT_TIMER =
            new HashedWheelTimer(ThreadFactories.namedThreadFactory("esa-timeout-checker-"),
                    30, TimeUnit.MILLISECONDS);

    protected final NettyConnectionConfig connectionConfig;
    protected final MultiplexPool<NettyConnection> connectionPool;

    protected static void addTimeoutTask(TimerTask task, long delayMillis) {
        TIME_OUT_TIMER.newTimeout(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    public NettyClient(final NettyConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
        this.connectionConfig.setConnectionInitializer(createConnectionInitializer(connectionConfig));
        final MultiplexPoolBuilder multiplexPoolBuilder = connectionConfig.getMultiplexPoolBuilder();
        //构建异步连接池
        this.connectionPool = new DefaultMultiplexPool.Builder<NettyConnection>()
                .maxPoolSize(multiplexPoolBuilder.getMaxPoolSize())
                .blockCreateWhenInit(multiplexPoolBuilder.isBlockCreateWhenInit())
                .waitCreateWhenLastTryAcquire(multiplexPoolBuilder.isWaitCreateWhenLastTryAcquire())
                .maxRetryTimes(multiplexPoolBuilder.getMaxRetryTimes())
                .factory(new PooledNettyConnectionFactory(connectionConfig))
                .init(multiplexPoolBuilder.isInit())
                .build();
    }

    protected void notifyWriteDone(final ChannelFuture channelFuture,
                                   final long requestId,
                                   final ResponseCallback callback,
                                   final NettyConnection connection) {
        if (callback == null) {
            return;
        }

        //通知网络写入事件
        if (channelFuture.isSuccess()) {
            callback.onWriteToNetwork(true, null);
            return;
        }

        //发送失败
        final String errMsg = StringUtils.concat("write request to ", connection.getName(), " error.");
        connection.getCallbackMap().remove(requestId);
        try {
            callback.onWriteToNetwork(false, channelFuture.cause().toString());
        } finally {
            callback.onError(new ConnectFailedException(errMsg, channelFuture.cause()));
        }
    }

    protected abstract ConnectionInitializer createConnectionInitializer(final NettyConnectionConfig connectionConfig);

    public boolean isActive() {
        return connectionPool.canAcquire();
    }

    public void close() {
        this.connectionPool.closeAll();
    }


}
