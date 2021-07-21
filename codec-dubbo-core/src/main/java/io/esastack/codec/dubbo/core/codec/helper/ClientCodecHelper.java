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
package io.esastack.codec.dubbo.core.codec.helper;

import esa.commons.io.IOUtils;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.core.utils.DubboConstants;
import io.esastack.codec.dubbo.core.utils.ReflectUtils;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.api.SerializeFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 针对客户端收到DubboMessage包进行解析工具类
 */
public class ClientCodecHelper {

    public static DubboMessage toDubboMessage(final RpcInvocation invocation) throws Exception {
        DubboMessage request = new DubboMessage();

        DubboHeader header = new DubboHeader()
                .setSeriType(invocation.getSeriType())
                .setRequestId(invocation.getRequestId());

        header.setTwoWay(!invocation.isOneWay());

        request.setHeader(header);

        ByteArrayOutputStream byteArrayOutputStream = null;
        DataOutputStream out = null;
        try {
            Serialization serialization = SerializeFactory.getSerialization(invocation.getSeriType());
            if (serialization == null) {
                throw new SerializationException("Unsupported serialization type:" + request.getHeader().getSeriType());
            }

            byteArrayOutputStream = new ByteArrayOutputStream();
            out = serialization.serialize(byteArrayOutputStream);

            out.writeUTF(DubboConstants.DUBBO_VERSION);
            out.writeUTF(invocation.getInterfaceName());
            out.writeUTF(invocation.getVersion());
            out.writeUTF(invocation.getMethodName());
            out.writeUTF(ReflectUtils.getDesc(invocation.getParameterTypes()));

            if (invocation.getArguments() != null) {
                for (int i = 0; i < invocation.getArguments().length; i++) {
                    Object object = invocation.getArguments()[i];
                    out.writeObject(object);
                }
            }

            out.writeMap(invocation.getAttachments());
            out.flush();

            request.setBody(byteArrayOutputStream.toByteArray());
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(byteArrayOutputStream);
        }

        return request;
    }

    public static RpcResult toRpcResult(final DubboHeader header,
                                        final byte[] body) {
        return toRpcResult(header, body, null);
    }

    public static RpcResult toRpcResult(final DubboHeader header,
                                        final byte[] body,
                                        final Map<String, String> attachments) {
        if (header == null || body == null) {
            return null;
        }
        DubboMessage dubboMessage = new DubboMessage().setHeader(header).setBody(body);
        return toRpcResult(dubboMessage, attachments);
    }

    public static RpcResult toRpcResult(final DubboMessage response) {
        return toRpcResult(response, null);
    }

    public static RpcResult toRpcResult(final DubboMessage response,
                                        final Map<String, String> attachments) {
        if (response == null || response.getHeader() == null) {
            return null;
        }

        RpcResult rpcResult = new RpcResult();
        rpcResult.setSeriType(response.getHeader().getSeriType());
        rpcResult.setRequestId(response.getHeader().getRequestId());
        rpcResult.setStatus(response.getHeader().getStatus());
        rpcResult.setValue(response.getBody());
        if (attachments != null && !attachments.isEmpty()) {
            rpcResult.getAttachments().putAll(attachments);
        }
        return rpcResult;
    }

    public static void deserializeBody(final RpcResult rpcResult,
                                       final byte[] body,
                                       final Class<?> returnType,
                                       final Type genericReturnType) {
        if (body == null) {
            return;
        }
        final long startAt = System.currentTimeMillis();
        rpcResult.setAttachment(DubboConstants.TRACE.TIME_OF_RSP_DESERIALIZE_BEGIN_KEY, startAt + "");
        ByteArrayInputStream inputStream = null;
        DataInputStream in = null;
        try {
            Serialization serialization = SerializeFactory.getSerialization(rpcResult.getSeriType());
            if (serialization == null) {
                throw new SerializationException("unsupported serialization type:" + rpcResult.getSeriType());
            }

            inputStream = new ByteArrayInputStream(body);
            in = serialization.deserialize(inputStream);

            if (DubboConstants.RESPONSE_STATUS.OK == rpcResult.getStatus()) {
                deserialize(rpcResult, in, returnType, genericReturnType);
            } else {
                rpcResult.setErrorMessage(in.readUTF());
            }
        } catch (Throwable t) {
            rpcResult.setStatus(DubboConstants.RESPONSE_STATUS.CLIENT_ERROR);
            rpcResult.setException(t);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(inputStream);
        }
        rpcResult.setAttachment(DubboConstants.TRACE.TIME_OF_RSP_DESERIALIZE_COST_KEY,
                String.valueOf(System.currentTimeMillis() - startAt));
    }

    @SuppressWarnings("unchecked")
    private static void deserialize(final RpcResult rpcResult,
                                    final DataInputStream in,
                                    final Class<?> returnType,
                                    final Type genericReturnType) throws Exception {
        byte flag = in.readByte();
        switch (flag) {
            case RpcResult.RESPONSE_FLAG.RESPONSE_NULL_VALUE:
                break;
            case RpcResult.RESPONSE_FLAG.RESPONSE_VALUE:
                rpcResult.setValue(in.readObject(returnType, genericReturnType));
                break;
            case RpcResult.RESPONSE_FLAG.RESPONSE_WITH_EXCEPTION:
                rpcResult.setException(in.readThrowable());
                break;
            case RpcResult.RESPONSE_FLAG.RESPONSE_NULL_VALUE_WITH_ATTACHMENTS:
                rpcResult.setAttachments(in.readMap());
                break;
            case RpcResult.RESPONSE_FLAG.RESPONSE_VALUE_WITH_ATTACHMENTS:
                rpcResult.setValue(in.readObject(returnType, genericReturnType));
                rpcResult.setAttachments(in.readMap());
                break;
            case RpcResult.RESPONSE_FLAG.RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS:
                rpcResult.setException(in.readObject(Throwable.class));
                rpcResult.setAttachments(in.readMap());
                break;
            default:
                throw new IOException("Unknown result flag, expect '0' '1' '2' '3' '4' '5', but received: " + flag);
        }
    }
}
