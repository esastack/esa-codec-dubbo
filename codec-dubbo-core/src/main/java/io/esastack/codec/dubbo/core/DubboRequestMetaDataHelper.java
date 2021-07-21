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

import esa.commons.io.IOUtils;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.core.utils.ReflectUtils;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.esastack.codec.serialization.api.SerializeFactory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Dubbo协议元数据信息
 */
public class DubboRequestMetaDataHelper {

    private static final Logger logger = LoggerFactory.getLogger(DubboRequestMetaDataHelper.class);

    /**
     * 从请求Request中读取元数据信息(排除请求的方法参数)
     */
    public static DubboRequestMetaData readRequestMetaData(DubboMessage request) {
        DataInputStream in = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            byte seriType = request.getHeader().getSeriType();
            Serialization serialization = SerializeFactory.getSerialization(seriType);
            if (serialization == null) {
                throw new SerializationException("unsupported serialization type:" + seriType);
            }
            byteArrayInputStream = new ByteArrayInputStream(request.getBody());
            in = serialization.deserialize(byteArrayInputStream);

            String dubboVersion = in.readUTF();
            String interfaceName = in.readUTF();
            String version = in.readUTF();
            String method = in.readUTF();

            DubboRequestMetaData metaData = new DubboRequestMetaData();
            metaData.setDubboVersion(dubboVersion);
            metaData.setInterfaceName(interfaceName);
            metaData.setMethodName(method);
            metaData.setSeriType(seriType);
            metaData.setVersion(version);

            //方法参数类型描述
            String parameterTypeDesc = in.readUTF();

            //方法参数类型
            Class<?>[] parameterTypes;
            if (parameterTypeDesc.length() > 0) {
                //方法的参数值
                Object[] args;

                if (seriType == SerializeConstants.HESSIAN2_SERIALIZATION_ID
                        || seriType == SerializeConstants.KRYO_SERIALIZATION_ID
                        || seriType == SerializeConstants.PROTOSTUFF_SERIALIZATION_ID) {
                    List<Class<?>> cs = new ArrayList<Class<?>>();
                    Matcher m = ReflectUtils.DESC_PATTERN.matcher(parameterTypeDesc);
                    while (m.find()) {
                        cs.add(Object.class);
                    }
                    args = new Object[cs.size()];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = in.readObject(Object.class);
                    }
                } else {
                    parameterTypes = ReflectUtils.desc2classArray(parameterTypeDesc);
                    args = new Object[parameterTypes.length];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = in.readObject(Map.class);
                    }
                }
            }

            //传递的附件信息
            @SuppressWarnings("unchecked")
            Map<String, String> attachmentTmp = (Map<String, String>) in.readObject(Map.class);
            if (attachmentTmp != null && attachmentTmp.size() > 0) {
                metaData.setAttachments(attachmentTmp);
            }

            return metaData;
        } catch (Exception e) {
            logger.error("Failed to get metadata of request for the reason {}, cause : {}", e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(byteArrayInputStream);
            IOUtils.closeQuietly(in);
        }
        return null;
    }
}
