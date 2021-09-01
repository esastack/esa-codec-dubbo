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
package io.esastack.codec.dubbo.server;

import io.esastack.codec.common.exception.SerializationException;
import io.esastack.codec.common.utils.NettyUtils;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.server.handler.BaseServerBizHandlerAdapter;
import io.esastack.codec.dubbo.server.handler.DubboResponseHolder;
import io.esastack.codec.dubbo.server.handler.DubboServerNettyHandler;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BaseServerBizHandlerAdapterTest {

    public static RpcInvocation buildRpcInvocation() {
        Map<String, String> attachments = new HashMap<>();
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new String[]{"dubbo"});
        rpcInvocation.setInterfaceName("org.apache.dubbo.demo.DemoService");
        rpcInvocation.setReturnType(String.class);
        rpcInvocation.setAttachments(attachments);
        return rpcInvocation;
    }

    @Test
    public void testBaseServerBizHandlerAdapter() throws Exception {
        RpcInvocation invocation = buildRpcInvocation();
        invocation.setSeriType(SerializeConstants.HESSIAN2_SERIALIZATION_ID);
        DubboMessage mockRequestDubboMesage = ClientCodecHelper.toDubboMessage(invocation);
        //心跳
        DubboMessage mocKHeartbeat = new DubboMessage();
        DubboHeader header = new DubboHeader();
        header.setSeriType((byte) 2).setHeartbeat(true).setRequest(true);
        mocKHeartbeat.setHeader(header);
        mocKHeartbeat.setBody(NettyUtils.nullValue((byte) 2));

        BaseServerBizHandlerAdapter serverBizHandlerAdapter = new BaseServerBizHandlerAdapter() {
            @Override
            protected void process0(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                assertEquals("embedded", dubboResponseHolder.getLocalAddressString());
                assertEquals("embedded", dubboResponseHolder.getRemoteAddressString());
                assertEquals("embedded", dubboResponseHolder.getLocalSocketAddress().toString());
                assertEquals("embedded", dubboResponseHolder.getRemoteSocketAddress().toString());
                RpcInvocation invocation = null;
                try {
                    invocation = ServerCodecHelper.toRpcInvocation(request);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
                String str = "hello  world from client " + invocation.getRequestId() + " " +
                        invocation.getSeriType();
                DubboRpcResult rpcResult = DubboRpcResult.success(invocation.getRequestId(),
                        invocation.getSeriType(), str);
                DubboMessage dubboMessage = null;
                try {
                    dubboMessage = ServerCodecHelper.toDubboMessage(rpcResult, request.getBody().alloc());
                } catch (SerializationException e) {
                    e.printStackTrace();
                    dubboResponseHolder.getChannelHandlerContext().channel().close();
                }
                dubboResponseHolder.end(dubboMessage);
            }

            @Override
            public void shutdown() {

            }
        };
        DubboServerNettyHandler dubboServerNettyHandler = new DubboServerNettyHandler(serverBizHandlerAdapter);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(dubboServerNettyHandler);
        embeddedChannel.writeOneInbound(mockRequestDubboMesage);
        embeddedChannel.writeOneInbound(mocKHeartbeat);
        boolean finish = embeddedChannel.finish();
        assertTrue(finish);
    }
}
