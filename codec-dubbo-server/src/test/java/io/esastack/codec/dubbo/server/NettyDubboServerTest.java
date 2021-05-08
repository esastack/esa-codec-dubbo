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

import esa.commons.concurrent.ThreadPools;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.server.handler.BaseServerBizHandlerAdapter;
import io.esastack.codec.dubbo.server.handler.DubboResponseHolder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.fail;

public class NettyDubboServerTest {

    private static ExecutorService workerThreadPool = ThreadPools.builder()
            .corePoolSize(200)
            .maximumPoolSize(200)
            .useArrayBlockingQueue(5000)
            .rejectPolicy((r, executor) -> System.out.println(" task rejected ")).build();

    @Test
    public void testNettyDubboServer() {

        Map<ChannelOption, Object> options = new HashMap<>(16);
        Map<ChannelOption, Object> childOptions = new HashMap<>(16);
        options.put(ChannelOption.SO_BACKLOG, 2048);
        options.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        childOptions.put(ChannelOption.SO_REUSEADDR, true);
        childOptions.put(ChannelOption.SO_KEEPALIVE, true);
        childOptions.put(ChannelOption.TCP_NODELAY, true);
        NettyDubboServer dubboServer = NettyDubboServer.newBuilder()
                .setPort(20888)
                .setChildChannelOptions(childOptions)
                .setChannelOptions(options)
                .setBizHandler(new BaseServerBizHandlerAdapter() {
                    @Override
                    public void process0(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                        final RpcInvocation invocation;
                        try {
                            invocation = ServerCodecHelper.toRpcInvocation(request);
                        } catch (Exception e) {
                            fail();
                            RpcResult rpcResult = RpcResult.error(
                                    request.getHeader().getRequestId(), request.getHeader().getSeriType(), e);
                            DubboMessage errorResponse = null;
                            try {
                                errorResponse = ServerCodecHelper.toDubboMessage(rpcResult);
                            } catch (SerializationException ex) {
                                ex.printStackTrace();
                                dubboResponseHolder.getChannelHandlerContext().channel().close();
                            }
                            dubboResponseHolder.end(errorResponse);
                            return;
                        }
                        workerThreadPool.execute(() -> {
                            String str = "hello  world from client " + invocation.getRequestId() + " " +
                                    invocation.getSeriType();
                            RpcResult rpcResult = RpcResult.success(invocation.getRequestId(),
                                    invocation.getSeriType(), str);
                            DubboMessage dubboMessage = null;
                            try {
                                dubboMessage = ServerCodecHelper.toDubboMessage(rpcResult, request.getBody().alloc());
                            } catch (SerializationException e) {
                                e.printStackTrace();
                                dubboResponseHolder.getChannelHandlerContext().channel().close();
                            }
                            dubboResponseHolder.end(dubboMessage);
                        });

                    }

                    @Override
                    public void shutdown() {

                    }
                })
                .build();
        dubboServer.start();
        dubboServer.shutdown();
    }
}
