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
package io.esastack.codec.dubbo.core;

import io.esastack.codec.common.RpcResult;
import io.esastack.codec.common.exception.InternalException;


public class DubboRpcResult extends RpcResult {

    private byte seriType;
    private long requestId = 0;
    private String errorMessage;
    private byte status = DubboConstants.RESPONSE_STATUS.OK;

    public static DubboRpcResult success(long requestId, byte seriType, Object value) {
        DubboRpcResult rpcResult = new DubboRpcResult();
        rpcResult.requestId = requestId;
        rpcResult.seriType = seriType;
        rpcResult.value = value;
        rpcResult.exception = null;
        return rpcResult;
    }

    public static DubboRpcResult error(long requestId, byte seriType, Throwable exception) {
        DubboRpcResult rpcResult = new DubboRpcResult();
        rpcResult.setRequestId(requestId);
        rpcResult.value = null;
        rpcResult.seriType = seriType;
        rpcResult.exception = exception;
        return rpcResult;
    }

    @Override
    public Throwable getException() {
        if (exception == null && errorMessage != null) {
            exception = new InternalException(
                    String.format("RpcResult status: %d, RpcResult error message: %s.", status, errorMessage));
        }
        return exception;
    }

    public byte getSeriType() {
        return seriType;
    }

    public void setSeriType(byte seriType) {
        this.seriType = seriType;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public DubboRpcResult setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public byte getStatus() {
        return status;
    }

    public DubboRpcResult setStatus(byte status) {
        this.status = status;
        return this;
    }

    @SuppressWarnings("TypeName")
    public static final class RESPONSE_FLAG {
        public static final byte RESPONSE_WITH_EXCEPTION = 0;
        public static final byte RESPONSE_VALUE = 1;
        public static final byte RESPONSE_NULL_VALUE = 2;
        public static final byte RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS = 3;
        public static final byte RESPONSE_VALUE_WITH_ATTACHMENTS = 4;
        public static final byte RESPONSE_NULL_VALUE_WITH_ATTACHMENTS = 5;
    }
}
