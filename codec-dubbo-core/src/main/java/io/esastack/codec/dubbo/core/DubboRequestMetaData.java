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

import java.util.HashMap;
import java.util.Map;

/**
 * Dubbo协议元数据信息
 */
public class DubboRequestMetaData {
    private String dubboVersion;

    private byte seriType;

    private String interfaceName;

    private String methodName;

    private String version;

    private Map<String, String> attachments = new HashMap<>(16);

    public String getDubboVersion() {
        return dubboVersion;
    }

    public DubboRequestMetaData setDubboVersion(String dubboVersion) {
        this.dubboVersion = dubboVersion;
        return this;
    }

    public byte getSeriType() {
        return seriType;
    }

    public void setSeriType(byte seriType) {
        this.seriType = seriType;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }
}
