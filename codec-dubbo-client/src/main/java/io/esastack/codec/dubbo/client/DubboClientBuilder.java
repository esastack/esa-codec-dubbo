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

import io.esastack.codec.common.connection.NettyConnectionConfig;

public class DubboClientBuilder {

    private int readTimeout = 6000;
    private int writeTimeout = 1000;
    private NettyConnectionConfig connectionConfig;

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public DubboClientBuilder setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public DubboClientBuilder setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public NettyConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public DubboClientBuilder setConnectionConfig(final NettyConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
        return this;
    }

    public NettyDubboClient build() {
        return new NettyDubboClient(this);
    }
}
