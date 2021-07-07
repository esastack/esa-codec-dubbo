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
package com.dubbo.lite.test;

import esa.commons.concurrent.ThreadPools;
import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.server.DubboServerBuilder;
import io.esastack.codec.dubbo.server.NettyDubboServer;
import io.esastack.codec.dubbo.server.handler.DubboResponseHolder;
import io.esastack.codec.dubbo.server.handler.DubboServerBizHandler;
import org.junit.Ignore;

import java.util.concurrent.ExecutorService;

@Ignore
public class DubboSDKServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboSDKServer.class);

    static ExecutorService workerThreadPool =
            ThreadPools.builder()
                    .corePoolSize(200)
                    .maximumPoolSize(200)
                    .useSynchronousQueue()
                    .rejectPolicy((r, executor) -> LOGGER.error("rejectedExecution ")).build();

    public static void main(String[] args) {
        // build server
        DubboServerBuilder dubboServerBuilder = new DubboServerBuilder()
                .setPort(20880)
                .setBizHandler(new DubboServerBizHandler() { // handle request and return response
                    @Override
                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                        final RpcInvocation invocation;
                        try {
                            // parse request
                            invocation = ServerCodecHelper.toRpcInvocation(request);
                        } catch (Exception e) {
                            LOGGER.error("Failed to convert request to rpc invocation for {}", e);
                            dubboResponseHolder.end(null);
                            return;
                        }
                        workerThreadPool.execute(() -> {
                            String response = "requestId:" +
                                    invocation.getRequestId() +
                                    " Hello " + invocation.getArguments()[0] +
                                    ", response from provider. seriType:" +
                                    invocation.getSeriType();

                            DubboMessage dubboResponse = null;
                            try {
                                // build response
                                dubboResponse = ServerCodecHelper.toDubboMessage(
                                        RpcResult.success(
                                                invocation.getRequestId(),
                                                invocation.getSeriType(),
                                                response),
                                        request.getBody().alloc());
                            } catch (SerializationException e) {
                                LOGGER.error("Failed to serialize response for {}", e);
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
    }
}
