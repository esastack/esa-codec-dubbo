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
package io.esastack.codec.dubbo.client;

import esa.commons.concurrent.ThreadPools;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.common.exception.SerializationException;
import io.esastack.codec.common.server.NettyServerConfig;
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.server.DubboServerBuilder;
import io.esastack.codec.dubbo.server.NettyDubboServer;
import io.esastack.codec.dubbo.server.handler.DubboResponseHolder;
import io.esastack.codec.dubbo.server.handler.DubboServerBizHandler;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class DubboSDKServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboSDKServer.class);

    static ExecutorService workerThreadPool =
            ThreadPools.builder()
                    .corePoolSize(200)
                    .maximumPoolSize(200)
                    .useSynchronousQueue()
                    .rejectPolicy((r, executor) -> LOGGER.error("rejectedExecution ")).build();

    public static NettyDubboServer start(String[] args) {
        // build server config
        final Map<ChannelOption, Object> options = new HashMap<>();
        options.put(ChannelOption.SO_BACKLOG, 128);
        options.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final Map<ChannelOption, Object> childOptions = new HashMap<>();
        childOptions.put(ChannelOption.SO_REUSEADDR, true);
        childOptions.put(ChannelOption.SO_KEEPALIVE, true);
        childOptions.put(ChannelOption.TCP_NODELAY, true);
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setBindIp("localhost")
                .setPort(20880)
                .setIoThreads(1)
                .setBossThreads(4)
                .setChannelOptions(options)
                .setChildChannelOptions(childOptions)
                .setHeartbeatTimeoutSeconds(60);
        // build server
        DubboServerBuilder dubboServerBuilder = new DubboServerBuilder()
                .setServerConfig(nettyServerConfig)
                .setBizHandler(new DubboServerBizHandler() { // handle request and return response
                    @Override

                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                        final RpcInvocation invocation;
                        try {
                            // parse request
                            invocation = ServerCodecHelper.toRpcInvocation(request);
                        } catch (Exception e) {
                            dubboResponseHolder.end(null);
                            return;
                        }
                        workerThreadPool.execute(() -> {
                            try {
                                Thread.sleep(200);
                            } catch (Exception ignore) {
                            }
                            String response = (String) invocation.getArguments()[0];

                            DubboMessage dubboResponse = null;
                            try {
                                // build response
                                dubboResponse = ServerCodecHelper.toDubboMessage(
                                        DubboRpcResult.success(
                                                invocation.getRequestId(),
                                                invocation.getSeriType(),
                                                response),
                                        request.getBody().alloc());
                            } catch (SerializationException e) {
                                dubboResponseHolder.getChannelHandlerContext().channel().close();
                            }
                            // send response
                            dubboResponseHolder.end(dubboResponse);
                        });
                    }

                    @Override
                    public void shutdown() {

                    }
                });
        NettyDubboServer nettyDubboServer = new NettyDubboServer(dubboServerBuilder);
        // start server
        nettyDubboServer.start();
        return nettyDubboServer;
    }
}
