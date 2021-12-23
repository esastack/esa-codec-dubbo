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
import io.esastack.codec.common.exception.SerializationException;
import io.esastack.codec.dubbo.core.DubboConstants;
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.utils.ReflectUtils;
import io.esastack.codec.serialization.api.*;
import io.netty.buffer.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * 针对客户端收到DubboMessage包进行解析工具类
 */
public class ClientCodecHelper {

    public static DubboMessage toDubboMessage(final RpcInvocation invocation) throws Exception {
        return toDubboMessage(invocation, UnpooledByteBufAllocator.DEFAULT);
    }

    public static DubboMessage toDubboMessage(final RpcInvocation invocation,
                                              final ByteBufAllocator alloc) throws Exception {
        DubboMessage request = new DubboMessage();

        DubboHeader header = new DubboHeader()
                .setSeriType(invocation.getSeriType())
                .setRequestId(invocation.getRequestId());

        header.setTwoWay(!invocation.isOneWay());

        request.setHeader(header);

        ByteBufOutputStream byteBufOutputStream = null;
        DataOutputStream out = null;
        try {
            Serialization serialization = SerializeFactory.getSerialization(invocation.getSeriType());
            if (serialization == null) {
                String msg = "Unsupported serialization type, id=" + request.getHeader().getSeriType() + ", name=" +
                        SerializeConstants.seriNames.get(request.getHeader().getSeriType()) +
                        ", maybe it not included in the classpath, please check your (maven/gradle) dependencies!";
                throw new SerializationException(msg);
            }

            byteBufOutputStream = new ByteBufOutputStream(alloc.buffer());
            out = serialization.serialize(byteBufOutputStream);

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

            request.setBody(byteBufOutputStream.buffer());
        } finally {
            IOUtils.closeQuietly(byteBufOutputStream);
            IOUtils.closeQuietly(out);
        }

        return request;
    }

    public static DubboRpcResult toRpcResult(final DubboMessage response,
                                             final Class<?> returnType) {
        return toRpcResult(response, returnType, returnType, null);
    }

    public static DubboRpcResult toRpcResult(final DubboMessage response,
                                             final Class<?> returnType,
                                             final Type genericReturnType) {
        return toRpcResult(response, returnType, genericReturnType, null);
    }

    public static DubboRpcResult toRpcResult(final DubboMessage response,
                                             final Class<?> returnType,
                                             final Map<String, String> attachments) {
        return toRpcResult(response, returnType, returnType, attachments);
    }

    public static DubboRpcResult toRpcResult(final DubboMessage response,
                                             final Class<?> returnType,
                                             final Type genericReturnType,
                                             final Map<String, String> attachments) {
        if (response == null || response.getHeader() == null) {
            return null;
        }

        DubboRpcResult rpcResult = new DubboRpcResult();
        rpcResult.setSeriType(response.getHeader().getSeriType());
        rpcResult.setRequestId(response.getHeader().getRequestId());
        rpcResult.setStatus(response.getHeader().getStatus());
        ByteBuf byteBuf = response.getBody();
        ByteBufInputStream byteBufInputStream = null;
        DataInputStream in = null;
        try {
            Serialization serialization = SerializeFactory.getSerialization(response.getHeader().getSeriType());
            if (serialization == null) {
                String msg = "Unsupported serialization type, id=" + response.getHeader().getSeriType() + ", name=" +
                        SerializeConstants.seriNames.get(response.getHeader().getSeriType()) +
                        ", maybe it not included in the classpath, please check your (maven/gradle) dependencies!";
                throw new SerializationException(msg);
            }

            byteBufInputStream = new ByteBufInputStream(byteBuf);
            in = serialization.deserialize(byteBufInputStream);

            if (DubboConstants.RESPONSE_STATUS.OK == response.getHeader().getStatus()) {
                deserialize(rpcResult, in, returnType, genericReturnType);
            } else {
                // Compatible with dubbo, only exception information, no exception stack information
                rpcResult.setStatus(DubboConstants.RESPONSE_STATUS.SERVER_ERROR);
                rpcResult.setErrorMessage(in.readUTF());
            }
            if (attachments != null && !attachments.isEmpty()) {
                rpcResult.getAttachments().putAll(attachments);
            }
        } catch (Throwable t) {
            rpcResult.setStatus(DubboConstants.RESPONSE_STATUS.CLIENT_ERROR);
            rpcResult.setErrorMessage(t.toString());
            rpcResult.setException(t);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(byteBufInputStream);
        }
        return rpcResult;
    }

    public static DubboRpcResult toRpcResult(final DubboHeader header,
                                             final byte[] body,
                                             final Class<?> returnType) {
        return toRpcResult(header, body, returnType, returnType);
    }

    public static DubboRpcResult toRpcResult(final DubboHeader header,
                                             final byte[] body,
                                             final Class<?> returnType,
                                             final Type genericReturnType) {
        if (header == null || body == null) {
            return null;
        }

        DubboRpcResult rpcResult = new DubboRpcResult();
        rpcResult.setSeriType(header.getSeriType());
        rpcResult.setRequestId(header.getRequestId());
        rpcResult.setStatus(header.getStatus());
        ByteArrayInputStream inputStream = null;
        DataInputStream in = null;
        try {
            Serialization serialization = SerializeFactory.getSerialization(header.getSeriType());
            if (serialization == null) {
                String msg = "Unsupported serialization type, id=" + header.getSeriType() + ", name=" +
                        SerializeConstants.seriNames.get(header.getSeriType()) +
                        ", maybe it not included in the classpath, please check your (maven/gradle) dependencies!";
                throw new SerializationException(msg);
            }

            inputStream = new ByteArrayInputStream(body);
            in = serialization.deserialize(inputStream);

            if (DubboConstants.RESPONSE_STATUS.OK == header.getStatus()) {
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
        return rpcResult;
    }

    @SuppressWarnings("unchecked")
    private static void deserialize(final DubboRpcResult rpcResult,
                                    final DataInputStream in,
                                    final Class<?> returnType,
                                    final Type genericReturnType) throws Exception {
        byte flag = in.readByte();
        switch (flag) {
            case DubboRpcResult.RESPONSE_FLAG.RESPONSE_NULL_VALUE:
                break;
            case DubboRpcResult.RESPONSE_FLAG.RESPONSE_VALUE:
                rpcResult.setValue(in.readObject(returnType, genericReturnType));
                break;
            case DubboRpcResult.RESPONSE_FLAG.RESPONSE_WITH_EXCEPTION:
                rpcResult.setException(in.readThrowable());
                break;
            case DubboRpcResult.RESPONSE_FLAG.RESPONSE_NULL_VALUE_WITH_ATTACHMENTS:
                rpcResult.setAttachments(in.readMap());
                break;
            case DubboRpcResult.RESPONSE_FLAG.RESPONSE_VALUE_WITH_ATTACHMENTS:
                rpcResult.setValue(in.readObject(returnType, genericReturnType));
                rpcResult.setAttachments(in.readMap());
                break;
            case DubboRpcResult.RESPONSE_FLAG.RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS:
                rpcResult.setException(in.readObject(Throwable.class));
                rpcResult.setAttachments(in.readMap());
                break;
            default:
                throw new IOException("Unknown result flag, expect '0' '1' '2' '3' '4' '5', but received: " + flag);
        }
    }
}
