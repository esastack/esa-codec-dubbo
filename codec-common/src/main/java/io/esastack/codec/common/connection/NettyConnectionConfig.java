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

import io.esastack.codec.common.ssl.SslContextBuilder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 客户端构建参数
 */
public class NettyConnectionConfig {

    private String host;
    private int port;
    private boolean tlsFallback2Normal;
    private SslContextBuilder sslContextBuilder;
    private MultiplexPoolBuilder multiplexPoolBuilder;
    //Prototype handlers
    private ConnectionInitializer connectionInitializer;
    //Sharable handlers
    private List<ChannelHandler> channelHandlers = Collections.emptyList();
    private Map<ChannelOption, Object> channelOptions = Collections.emptyMap();
    private int connectTimeout = 1000;
    private int payload = 8 * 1024 * 1024;
    private String unixDomainSocketFile;
    private int writeBufferHighWaterMark;
    private boolean useNativeTransports = false;
    private int heartbeatTimeoutSeconds = 60;
    private int defaultRequestTimeout = 1000;

    public int getWriteBufferHighWaterMark() {
        return writeBufferHighWaterMark;
    }

    public NettyConnectionConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        this.writeBufferHighWaterMark = writeBufferHighWaterMark;
        return this;
    }

    public String getHost() {
        return host;
    }

    public NettyConnectionConfig setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public NettyConnectionConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public NettyConnectionConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getPayload() {
        return payload;
    }

    public NettyConnectionConfig setPayload(int payload) {
        this.payload = payload;
        return this;
    }

    public String getUnixDomainSocketFile() {
        return unixDomainSocketFile;
    }

    public NettyConnectionConfig setUnixDomainSocketFile(String unixDomainSocketFile) {
        this.unixDomainSocketFile = unixDomainSocketFile;
        return this;
    }

    public boolean isUseNativeTransports() {
        return useNativeTransports;
    }

    public NettyConnectionConfig setUseNativeTransports(boolean useNativeTransports) {
        this.useNativeTransports = useNativeTransports;
        return this;
    }

    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public NettyConnectionConfig setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        return this;
    }

    public int getDefaultRequestTimeout() {
        return defaultRequestTimeout;
    }

    public NettyConnectionConfig setDefaultRequestTimeout(int defaultRequestTimeout) {
        this.defaultRequestTimeout = defaultRequestTimeout;
        return this;
    }

    public Map<ChannelOption, Object> getChannelOptions() {
        return channelOptions;
    }

    public NettyConnectionConfig setChannelOptions(Map<ChannelOption, Object> channelOptions) {
        this.channelOptions = channelOptions;
        return this;
    }

    public List<ChannelHandler> getChannelHandlers() {
        return channelHandlers;
    }

    public NettyConnectionConfig setChannelHandlers(List<ChannelHandler> channelHandlers) {
        this.channelHandlers = channelHandlers;
        return this;
    }

    public ConnectionInitializer getConnectionInitializer() {
        return connectionInitializer;
    }

    public NettyConnectionConfig setConnectionInitializer(final ConnectionInitializer connectionInitializer) {
        this.connectionInitializer = connectionInitializer;
        return this;
    }

    public MultiplexPoolBuilder getMultiplexPoolBuilder() {
        return multiplexPoolBuilder;
    }

    public NettyConnectionConfig setMultiplexPoolBuilder(MultiplexPoolBuilder multiplexPoolBuilder) {
        this.multiplexPoolBuilder = multiplexPoolBuilder;
        return this;
    }

    public SslContextBuilder getSslContextBuilder() {
        return sslContextBuilder;
    }

    public NettyConnectionConfig setSslContextBuilder(SslContextBuilder sslContextBuilder) {
        this.sslContextBuilder = sslContextBuilder;
        return this;
    }

    public boolean isTlsFallback2Normal() {
        return tlsFallback2Normal;
    }

    public NettyConnectionConfig setTlsFallback2Normal(boolean tlsFallback2Normal) {
        this.tlsFallback2Normal = tlsFallback2Normal;
        return this;
    }

    public static final class MultiplexPoolBuilder {

        private static final int MAX_TIMES = 5;

        private int maxPoolSize = 64;

        private boolean init = false;

        private boolean blockCreateWhenInit = true;

        private int maxRetryTimes = MAX_TIMES;

        private boolean waitCreateWhenLastTryAcquire = true;

        private MultiplexPoolBuilder() {
        }

        public static MultiplexPoolBuilder newBuilder() {
            return new MultiplexPoolBuilder();
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public MultiplexPoolBuilder setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public boolean isInit() {
            return init;
        }

        public MultiplexPoolBuilder setInit(boolean init) {
            this.init = init;
            return this;
        }

        public boolean isBlockCreateWhenInit() {
            return blockCreateWhenInit;
        }

        public MultiplexPoolBuilder setBlockCreateWhenInit(boolean blockCreateWhenInit) {
            this.blockCreateWhenInit = blockCreateWhenInit;
            return this;
        }

        public int getMaxRetryTimes() {
            return maxRetryTimes;
        }

        public MultiplexPoolBuilder setMaxRetryTimes(int maxRetryTimes) {
            this.maxRetryTimes = maxRetryTimes;
            return this;
        }

        public boolean isWaitCreateWhenLastTryAcquire() {
            return waitCreateWhenLastTryAcquire;
        }

        public MultiplexPoolBuilder setWaitCreateWhenLastTryAcquire(boolean waitCreateWhenLastTryAcquire) {
            this.waitCreateWhenLastTryAcquire = waitCreateWhenLastTryAcquire;
            return this;
        }

    }
}
