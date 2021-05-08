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

import io.esastack.codec.dubbo.core.ssl.DubboSslContextBuilder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 客户端构建参数
 */
public class DubboClientBuilder {

    private String host;

    private int port;

    private int connectTimeout = 1000;

    private int readTimeout = 6000;

    private int writeTimeout = 1000;

    private int payload = 8 * 1024 * 1024;

    private String unixDomainSocketFile;

    private int writeBufferHighWaterMark;

    private boolean useNativeTransports = false;

    private int heartbeatTimeoutSeconds = 60;

    private int defaultRequestTimeout = 1000;

    private Map<ChannelOption, Object> channelOptions = Collections.emptyMap();

    //Sharable handlers
    private List<ChannelHandler> channelHandlers = Collections.emptyList();

    //Prototype handlers
    private List<Supplier<ChannelHandler>> handlerSuppliers = Collections.emptyList();

    private MultiplexPoolBuilder multiplexPoolBuilder;

    private DubboSslContextBuilder dubboSslContextBuilder;

    private boolean tlsFallback2Normal;

    public NettyDubboClient build() {
        return new NettyDubboClient(this);
    }

    public int getWriteBufferHighWaterMark() {
        return writeBufferHighWaterMark;
    }

    public DubboClientBuilder setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        this.writeBufferHighWaterMark = writeBufferHighWaterMark;
        return this;
    }

    public String getHost() {
        return host;
    }

    public DubboClientBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public DubboClientBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public DubboClientBuilder setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public DubboClientBuilder setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    public int getPayload() {
        return payload;
    }

    public DubboClientBuilder setPayload(int payload) {
        this.payload = payload;
        return this;
    }

    public String getUnixDomainSocketFile() {
        return unixDomainSocketFile;
    }

    public DubboClientBuilder setUnixDomainSocketFile(String unixDomainSocketFile) {
        this.unixDomainSocketFile = unixDomainSocketFile;
        return this;
    }

    public boolean isUseNativeTransports() {
        return useNativeTransports;
    }

    public DubboClientBuilder setUseNativeTransports(boolean useNativeTransports) {
        this.useNativeTransports = useNativeTransports;
        return this;
    }

    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public DubboClientBuilder setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        return this;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public DubboClientBuilder setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public int getDefaultRequestTimeout() {
        return defaultRequestTimeout;
    }

    public DubboClientBuilder setDefaultRequestTimeout(int defaultRequestTimeout) {
        this.defaultRequestTimeout = defaultRequestTimeout;
        return this;
    }

    public Map<ChannelOption, Object> getChannelOptions() {
        return channelOptions;
    }

    public DubboClientBuilder setChannelOptions(Map<ChannelOption, Object> channelOptions) {
        this.channelOptions = channelOptions;
        return this;
    }

    public List<ChannelHandler> getChannelHandlers() {
        return channelHandlers;
    }

    public DubboClientBuilder setChannelHandlers(List<ChannelHandler> channelHandlers) {
        this.channelHandlers = channelHandlers;
        return this;
    }

    public List<Supplier<ChannelHandler>> getHandlerSuppliers() {
        return handlerSuppliers;
    }

    public DubboClientBuilder setHandlerSuppliers(List<Supplier<ChannelHandler>> channelHandlers) {
        this.handlerSuppliers = channelHandlers;
        return this;
    }

    public MultiplexPoolBuilder getMultiplexPoolBuilder() {
        return multiplexPoolBuilder;
    }

    public DubboClientBuilder setMultiplexPoolBuilder(MultiplexPoolBuilder multiplexPoolBuilder) {
        this.multiplexPoolBuilder = multiplexPoolBuilder;
        return this;
    }

    public DubboSslContextBuilder getDubboSslContextBuilder() {
        return dubboSslContextBuilder;
    }

    public DubboClientBuilder setDubboSslContextBuilder(DubboSslContextBuilder dubboSslContextBuilder) {
        this.dubboSslContextBuilder = dubboSslContextBuilder;
        return this;
    }

    public boolean isTlsFallback2Normal() {
        return tlsFallback2Normal;
    }

    public DubboClientBuilder setTlsFallback2Normal(boolean tlsFallback2Normal) {
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
