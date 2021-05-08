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

import esa.commons.StringUtils;
import io.esastack.codec.dubbo.core.utils.DubboConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC 调用结果
 */
public class RpcResult {
    /**
     * 响应状态码
     */
    private byte status = DubboConstants.RESPONSE_STATUS.OK;

    /**
     * 序列化方式
     */
    private byte seriType;

    /**
     * 请求ID
     */
    private long requestId = 0;

    /**
     * RPC响应结果为对象
     */
    private Object value;

    /**
     * RPC响应结果为异常
     */
    private Throwable exception;

    /**
     * 响应状态非OK时，以文本方式响应错误内容
     */
    private String errorMessage;

    /**
     * 附件
     */
    private final Map<String, String> attachments = new HashMap<>(16);

    public static RpcResult success(long requestId, byte seriType, Object value) {
        RpcResult rpcResult = new RpcResult();
        rpcResult.requestId = requestId;
        rpcResult.seriType = seriType;
        rpcResult.value = value;
        rpcResult.exception = null;
        return rpcResult;
    }

    public static RpcResult error(long requestId, byte seriType, Throwable exception) {
        RpcResult rpcResult = new RpcResult();
        rpcResult.setRequestId(requestId);
        rpcResult.value = null;
        rpcResult.seriType = seriType;
        rpcResult.exception = exception;
        return rpcResult;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments.clear();
        this.attachments.putAll(attachments);
    }

    public void setAttachment(String key, String value) {
        if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
            return;
        }

        this.attachments.put(key, value);
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

    public RpcResult setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public byte getStatus() {
        return status;
    }

    public RpcResult setStatus(byte status) {
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
