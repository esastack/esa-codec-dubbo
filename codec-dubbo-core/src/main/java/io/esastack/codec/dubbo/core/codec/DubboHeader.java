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
package io.esastack.codec.dubbo.core.codec;

/**
 * 用于描述Dubbo协议Header结构
 */
public class DubboHeader {
    /**
     * 请求ID
     */
    private long requestId;

    /**
     * 序列化类型,默认hessian
     */
    private byte seriType = 2;

    /**
     * 是否心跳请求
     */
    private boolean heartbeat;

    /**
     * 是否双向请求（响应不需要）
     */
    private boolean twoWay = true;

    private transient boolean onewayWaited = false;
    /**
     * 响应状态码（请求不需要）
     */
    private byte status;

    /**
     * request flag
     */
    private boolean isRequest = false;

    public boolean isRequest() {
        return isRequest;
    }

    public DubboHeader setRequest(boolean request) {
        isRequest = request;
        return this;
    }

    public long getRequestId() {
        return requestId;
    }

    public DubboHeader setRequestId(long requestId) {
        this.requestId = requestId;
        return this;
    }

    public byte getSeriType() {
        return seriType;
    }

    public DubboHeader setSeriType(byte seriType) {
        this.seriType = seriType;
        return this;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public DubboHeader setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
        return this;
    }

    public boolean isTwoWay() {
        return twoWay;
    }

    public DubboHeader setTwoWay(boolean twoWay) {
        this.twoWay = twoWay;
        return this;
    }

    public byte getStatus() {
        return status;
    }

    public DubboHeader setStatus(byte status) {
        this.status = status;
        return this;
    }

    public boolean isOnewayWaited() {
        return onewayWaited;
    }

    public void setOnewayWaited(final boolean onewayWaited) {
        this.onewayWaited = onewayWaited;
    }
}
