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

import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.core.utils.DubboConstants;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.netty.buffer.ByteBuf;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class CodecHelperTest {

    @Test
    public void testToDubboMessage() throws Exception {
        RpcInvocation rpcInvocation = getRpcInvocation();
        DubboMessage requestMessage = ClientCodecHelper.toDubboMessage(rpcInvocation);
        Assert.assertNotNull(requestMessage);
        Assert.assertEquals(rpcInvocation.getRequestId(), requestMessage.getHeader().getRequestId());
        Assert.assertEquals(rpcInvocation.getSeriType(), requestMessage.getHeader().getSeriType());
    }

    @Test
    public void toRpcResultOk() throws SerializationException {

        Assert.assertNull(ClientCodecHelper.toRpcResult(null, ResultValue.class));

        DubboMessage dubboMessage = newDubboResponse(new ResultValue("test"));
        ByteBuf bodyBuf = dubboMessage.getBody();
        bodyBuf.markReaderIndex();

        RpcResult rpcResult = ClientCodecHelper.toRpcResult(dubboMessage, ResultValue.class);
        ResultValue resultValue = (ResultValue) rpcResult.getValue();
        Assert.assertEquals("test", resultValue.getData());

        bodyBuf.resetReaderIndex();
        byte[] body = new byte[bodyBuf.readableBytes()];
        bodyBuf.readBytes(body);

        RpcResult rpcResult2 = ClientCodecHelper.toRpcResult(
                dubboMessage.getHeader(), body, ResultValue.class);
        ResultValue resultValue2 = (ResultValue) rpcResult2.getValue();
        Assert.assertEquals("test", resultValue2.getData());

        DubboMessage nullDubboMessage = newDubboResponse(null);
        RpcResult nullRpcResult = ClientCodecHelper.toRpcResult(nullDubboMessage, ResultValue.class);
        Assert.assertNotNull(nullRpcResult.getAttachments().get("ttfb"));
    }

    @Test
    public void toRpcResultIllegalSeriType() throws SerializationException {

        DubboMessage dubboMessage = newDubboResponse(new ResultValue("test"));
        //illegal type
        dubboMessage.getHeader().setSeriType(Byte.MAX_VALUE);

        ByteBuf bodyBuf = dubboMessage.getBody();
        bodyBuf.markReaderIndex();

        RpcResult rpcResult = ClientCodecHelper.toRpcResult(dubboMessage, ResultValue.class);
        Assert.assertEquals(DubboConstants.RESPONSE_STATUS.CLIENT_ERROR, rpcResult.getStatus());

        bodyBuf.resetReaderIndex();
        byte[] body = new byte[bodyBuf.readableBytes()];
        bodyBuf.readBytes(body);

        RpcResult rpcResult2 = ClientCodecHelper.toRpcResult(
                dubboMessage.getHeader(), body, ResultValue.class);
        Assert.assertEquals(DubboConstants.RESPONSE_STATUS.CLIENT_ERROR, rpcResult2.getStatus());
    }

    @Test
    public void toRpcResultWithException() throws SerializationException {
        //error response
        Throwable timeout = new TimeoutException("timeout");
        DubboMessage dubboMessage = newDubboResponse(timeout);

        ByteBuf bodyBuf = dubboMessage.getBody();
        bodyBuf.markReaderIndex();

        RpcResult rpcResult = ClientCodecHelper.toRpcResult(dubboMessage, ResultValue.class);
        Assert.assertEquals(timeout.getMessage(), rpcResult.getException().getMessage());

        bodyBuf.resetReaderIndex();
        byte[] body = new byte[bodyBuf.readableBytes()];
        bodyBuf.readBytes(body);

        RpcResult rpcResult2 = ClientCodecHelper.toRpcResult(
                dubboMessage.getHeader(), body, ResultValue.class);
        Assert.assertEquals(timeout.getMessage(), rpcResult2.getException().getMessage());
    }

    @Test
    public void toInvocation() throws Exception {
        RpcInvocation rpcInvocation = getRpcInvocation();
        DubboMessage requestMessage = ClientCodecHelper.toDubboMessage(rpcInvocation);
        RpcInvocation invocation = ServerCodecHelper.toRpcInvocation(requestMessage);
        Assert.assertEquals(rpcInvocation.getSeriType(), invocation.getSeriType());
        Assert.assertEquals(rpcInvocation.getRequestId(), invocation.getRequestId());
        Assert.assertEquals(rpcInvocation.getInterfaceName(), invocation.getInterfaceName());
        Assert.assertEquals(rpcInvocation.getArguments()[0], invocation.getArguments()[0]);
    }

    private DubboMessage newDubboResponse(Object value) throws SerializationException {
        RpcResult rpcResult = newRpcResult(value);
        return ServerCodecHelper.toDubboMessage(rpcResult);
    }

    private RpcResult newRpcResult(Object value) {
        RpcResult rpcResult = new RpcResult();
        rpcResult.setSeriType(SerializeConstants.HESSIAN2_SERIALIZATION_ID);
        rpcResult.setRequestId(1L);
        if (value instanceof Throwable) {
            rpcResult.setException((Throwable) value);
        } else {
            rpcResult.setStatus(DubboConstants.RESPONSE_STATUS.OK);
            rpcResult.setValue(value);
        }
        rpcResult.setAttachment("ttfb", System.currentTimeMillis() + "");
        return rpcResult;
    }

    private RpcInvocation getRpcInvocation() {
        Map<String, String> attachments = new HashMap<>();
        attachments.put("tt", System.currentTimeMillis() + "");
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setRequestId(1L);
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new String[]{"dubbo"});
        rpcInvocation.setInterfaceName("org.apache.dubbo.demo.DemoService");
        rpcInvocation.setReturnType(String.class);
        rpcInvocation.setAttachments(attachments);
        rpcInvocation.setSeriType(SerializeConstants.HESSIAN2_SERIALIZATION_ID);
        rpcInvocation.setVersion("0.0.0");
        return rpcInvocation;
    }

    public static class ResultValue {
        private String data;

        public ResultValue(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
