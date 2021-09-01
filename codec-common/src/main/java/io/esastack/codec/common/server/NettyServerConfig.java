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

import io.esastack.codec.common.ssl.SslContextBuilder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NettyServerConfig {

    private int port;
    private int ioThreads;
    private int bossThreads;
    private int soBacklogSize = 1024;
    private int payload = 8 * 1024 * 1024;
    private int heartbeatTimeoutSeconds = 65;
    private String bindIp = "0.0.0.0";
    private String unixDomainSocketFile;
    private SslContextBuilder sslContextBuilder;
    private ServerConnectionInitializer connectionInitializer;
    private Map<ChannelOption, Object> channelOptions = Collections.emptyMap();
    private Map<ChannelOption, Object> childChannelOptions = Collections.emptyMap();
    private List<ChannelHandler> channelHandlers = Collections.emptyList();

    public Integer getPort() {
        return port;
    }

    public NettyServerConfig setPort(Integer port) {
        this.port = port;
        return this;
    }

    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public NettyServerConfig setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        return this;
    }

    public String getBindIp() {
        return bindIp;
    }

    public NettyServerConfig setBindIp(String bindIp) {
        this.bindIp = bindIp;
        return this;
    }

    public String getUnixDomainSocketFile() {
        return unixDomainSocketFile;
    }

    public NettyServerConfig setUnixDomainSocketFile(String unixDomainSocketFile) {
        this.unixDomainSocketFile = unixDomainSocketFile;
        return this;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public NettyServerConfig setIoThreads(final int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public NettyServerConfig setBossThreads(final int bossThreads) {
        this.bossThreads = bossThreads;
        return this;
    }

    public int getSoBacklogSize() {
        return soBacklogSize;
    }

    public NettyServerConfig setSoBacklogSize(int soBacklogSize) {
        this.soBacklogSize = soBacklogSize;
        return this;
    }

    public int getPayload() {
        return payload;
    }

    public NettyServerConfig setPayload(int payload) {
        this.payload = payload;
        return this;
    }

    public Map<ChannelOption, Object> getChannelOptions() {
        return channelOptions;
    }

    public NettyServerConfig setChannelOptions(Map<ChannelOption, Object> options) {
        this.channelOptions = options;
        return this;
    }

    public Map<ChannelOption, Object> getChildChannelOptions() {
        return childChannelOptions;
    }

    public NettyServerConfig setChildChannelOptions(Map<ChannelOption, Object> childChannelOptions) {
        this.childChannelOptions = childChannelOptions;
        return this;
    }

    public List<ChannelHandler> getChannelHandlers() {
        return channelHandlers;
    }

    public NettyServerConfig setChannelHandlers(List<ChannelHandler> channelHandlers) {
        this.channelHandlers = channelHandlers;
        return this;
    }

    public ServerConnectionInitializer getConnectionInitializer() {
        return connectionInitializer;
    }

    public NettyServerConfig setConnectionInitializer(final ServerConnectionInitializer connectionInitializer) {
        this.connectionInitializer = connectionInitializer;
        return this;
    }

    public SslContextBuilder getSslContextBuilder() {
        return sslContextBuilder;
    }

    public NettyServerConfig setSslContextBuilder(SslContextBuilder sslContextBuilder) {
        this.sslContextBuilder = sslContextBuilder;
        return this;
    }

    @Override
    public String toString() {
        return "NettyServerConfig{" +
                ", port=" + port +
                ", heartbeatTimeoutSeconds=" + heartbeatTimeoutSeconds +
                ", bindIp='" + bindIp + '\'' +
                ", unixDomainSocketFile='" + unixDomainSocketFile + '\'' +
                ", soBacklogSize=" + soBacklogSize +
                ", payload=" + payload +
                ", channelOptions=" + channelOptions +
                ", childChannelOptions=" + childChannelOptions +
                ", channelHandlers=" + channelHandlers +
                ", sslEnabled=" + (sslContextBuilder != null) +
                '}';
    }
}
