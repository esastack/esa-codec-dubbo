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

import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.core.utils.NettyUtils;
import io.esastack.codec.dubbo.server.handler.BaseServerBizHandlerAdapter;
import io.esastack.codec.dubbo.server.handler.DubboResponseHolder;
import io.esastack.codec.dubbo.server.handler.DubboServerNettyHandler;
import io.esastack.codec.dubbo.server.handler.telnet.handler.ExceptionHandler;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class DubboServerNettyHandlerTest {

    final BaseServerBizHandlerAdapter serverBizHandlerAdapter = new BaseServerBizHandlerAdapter() {
        @Override
        protected void process0(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
            RpcInvocation invocation = null;
            try {
                invocation = ServerCodecHelper.toRpcInvocation(request);
            } catch (Exception e) {
                fail();
            }
            String str = "hello  world from client " + invocation.getRequestId() + " " +
                    invocation.getSeriType();
            RpcResult rpcResult = RpcResult.success(invocation.getRequestId(),
                    invocation.getSeriType(), str);
            DubboMessage dubboMessage = null;
            try {
                dubboMessage = ServerCodecHelper.toDubboMessage(rpcResult);
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
        //mocKHeartbeat.setBody(NettyUtils.nullValue((byte) 2));
        DubboServerNettyHandler dubboServerNettyHandler = new DubboServerNettyHandler(serverBizHandlerAdapter);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(dubboServerNettyHandler);
        IdleStateEvent idleStateEvent = IdleStateEvent.READER_IDLE_STATE_EVENT;
        SslHandshakeCompletionEvent sslHandshakeCompletionEvent = SslHandshakeCompletionEvent.SUCCESS;
        embeddedChannel.pipeline().fireUserEventTriggered(sslHandshakeCompletionEvent);
        embeddedChannel.writeOneInbound(mockRequestDubboMesage);
        embeddedChannel.writeOneInbound(mocKHeartbeat);
        embeddedChannel.pipeline().fireUserEventTriggered(idleStateEvent);
        boolean finish = embeddedChannel.finish();
        assertTrue(finish);

    }

    @Test
    public void testExceptionCaught() {
        DubboServerNettyHandler dubboServerNettyHandler = new DubboServerNettyHandler(serverBizHandlerAdapter);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new ExceptionHandler(), dubboServerNettyHandler);
        try {
            assertFalse(embeddedChannel.writeInbound(Unpooled.EMPTY_BUFFER));
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }
    }
}
