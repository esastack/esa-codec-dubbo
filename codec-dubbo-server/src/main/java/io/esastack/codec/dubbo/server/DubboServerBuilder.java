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
package io.esastack.codec.dubbo.server;

import io.esastack.codec.dubbo.core.ssl.DubboSslContextBuilder;
import io.esastack.codec.dubbo.server.handler.DubboServerBizHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 服务器端指定启动参数
 */
public class DubboServerBuilder {

    private Integer port;

    private DubboServerBizHandler bizHandler;

    private int heartbeatTimeoutSeconds = 65;
    /**
     * 如果一个主机有两个IP地址，192.168.1.1 和 10.1.2.1，服务监听的地址是0.0.0.0,通过两个ip地址都能够访问该服务。
     */
    private String bindIp = "0.0.0.0";

    private String unixDomainSocketFile;

    private int soBacklogSize = 1024;

    /**
     * boss and io threads
     */
    private int bossThreads;

    private int ioThreads;

    /**
     * 8M Dubbo defalut playRoad size
     */
    private int payload = 8 * 1024 * 1024;

    private Map<ChannelOption, Object> channelOptions = Collections.emptyMap();

    private Map<ChannelOption, Object> childChannelOptions = Collections.emptyMap();

    private List<ChannelHandler> channelHandlers = Collections.emptyList();

    private DubboSslContextBuilder dubboSslContextBuilder;

    public DubboServerBuilder() {

    }

    public NettyDubboServer build() {
        return new NettyDubboServer(this);
    }

    public Integer getPort() {
        return port;
    }

    public DubboServerBuilder setPort(Integer port) {
        this.port = port;
        return this;
    }

    public DubboServerBizHandler getBizHandler() {
        return bizHandler;
    }

    public DubboServerBuilder setBizHandler(DubboServerBizHandler bizHandler) {
        this.bizHandler = bizHandler;
        return this;
    }

    public int getHeartbeatTimeoutSeconds() {
        return heartbeatTimeoutSeconds;
    }

    public DubboServerBuilder setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        return this;
    }

    public String getBindIp() {
        return bindIp;
    }

    public DubboServerBuilder setBindIp(String bindIp) {
        this.bindIp = bindIp;
        return this;
    }

    public String getUnixDomainSocketFile() {
        return unixDomainSocketFile;
    }

    public DubboServerBuilder setUnixDomainSocketFile(String unixDomainSocketFile) {
        this.unixDomainSocketFile = unixDomainSocketFile;
        return this;
    }

    public int getIoThreads() {
        return ioThreads;
    }

    public DubboServerBuilder setIoThreads(final int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public DubboServerBuilder setBossThreads(final int bossThreads) {
        this.bossThreads = bossThreads;
        return this;
    }

    public int getSoBacklogSize() {
        return soBacklogSize;
    }

    public DubboServerBuilder setSoBacklogSize(int soBacklogSize) {
        this.soBacklogSize = soBacklogSize;
        return this;
    }

    public int getPayload() {
        return payload;
    }

    public DubboServerBuilder setPayload(int payload) {
        this.payload = payload;
        return this;
    }

    public Map<ChannelOption, Object> getChannelOptions() {
        return channelOptions;
    }

    public DubboServerBuilder setChannelOptions(Map<ChannelOption, Object> options) {
        this.channelOptions = options;
        return this;
    }

    public Map<ChannelOption, Object> getChildChannelOptions() {
        return childChannelOptions;
    }

    public DubboServerBuilder setChildChannelOptions(Map<ChannelOption, Object> childChannelOptions) {
        this.childChannelOptions = childChannelOptions;
        return this;
    }

    public List<ChannelHandler> getChannelHandlers() {
        return channelHandlers;
    }

    public DubboServerBuilder setChannelHandlers(List<ChannelHandler> channelHandlers) {
        this.channelHandlers = channelHandlers;
        return this;
    }

    public DubboSslContextBuilder getDubboSslContextBuilder() {
        return dubboSslContextBuilder;
    }

    public DubboServerBuilder setDubboSslContextBuilder(DubboSslContextBuilder dubboSslContextBuilder) {
        this.dubboSslContextBuilder = dubboSslContextBuilder;
        return this;
    }

    @Override
    public String toString() {
        return "DubboServerBuilder{" +
                ", port=" + port +
                ", bizHandler=" + bizHandler +
                ", heartbeatTimeoutSeconds=" + heartbeatTimeoutSeconds +
                ", bindIp='" + bindIp + '\'' +
                ", unixDomainSocketFile='" + unixDomainSocketFile + '\'' +
                ", soBacklogSize=" + soBacklogSize +
                ", payload=" + payload +
                ", channelOptions=" + channelOptions +
                ", childChannelOptions=" + childChannelOptions +
                ", channelHandlers=" + channelHandlers +
                ", sslEnabled=" + (dubboSslContextBuilder != null) +
                '}';
    }
}
