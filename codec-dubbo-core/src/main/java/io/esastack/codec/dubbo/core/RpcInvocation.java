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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * RPC 请求调用封装
 */
public class RpcInvocation implements Serializable {
    private static final long serialVersionUID = -4355285085441097045L;
    /**
     * 附件
     */
    private final Map<String, String> attachments = new HashMap<>();
    /**
     * 序列化类型
     */
    private byte seriType = 2;
    /**
     * 请求ID
     */
    private long requestId = 0;
    /**
     * 接口名
     */
    private String interfaceName;
    /**
     * 方法名
     */
    private String methodName;
    /**
     * 服务路由版本
     */
    private String version;
    /**
     * 参数类型
     */
    private Class<?>[] parameterTypes;
    /**
     * 参数
     */
    private Object[] arguments;
    /**
     * @deprecated this field is not used in future.
     */
    @Deprecated
    private transient Class<?> returnType;

    /**
     * 是否oneWay调用
     */
    private boolean oneWay = false;

    public long getRequestId() {
        return requestId;
    }

    public RpcInvocation setRequestId(long requestId) {
        this.requestId = requestId;
        return this;
    }

    public byte getSeriType() {
        return seriType;
    }

    public RpcInvocation setSeriType(byte seriType) {
        this.seriType = seriType;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public RpcInvocation setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public RpcInvocation setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
        return this;
    }

    public String getMethodName() {
        return methodName;
    }

    public RpcInvocation setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public RpcInvocation setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
        return this;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public RpcInvocation setArguments(Object[] arguments) {
        this.arguments = arguments;
        return this;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public RpcInvocation setAttachments(Map<String, String> attachments) {
        if (attachments != null && !attachments.isEmpty()) {
            this.attachments.putAll(attachments);
        }
        return this;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public RpcInvocation setReturnType(Class<?> returnType) {
        this.returnType = returnType;
        return this;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public void setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
    }
}
