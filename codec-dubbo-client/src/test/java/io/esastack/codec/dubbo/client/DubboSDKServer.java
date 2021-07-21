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
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.server.NettyDubboServer;
import io.esastack.codec.dubbo.server.handler.DubboResponseHolder;
import io.esastack.codec.dubbo.server.handler.DubboServerBizHandler;

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
        NettyDubboServer dubboServer = NettyDubboServer.newBuilder()
                .setPort(20880)
                .setBizHandler(new DubboServerBizHandler() {
                    @Override
                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                        final RpcInvocation invocation;
                        try {
                            invocation = ServerCodecHelper.toRpcInvocation(request);
                        } catch (Exception e) {
                            //TODO 返回错误
                            e.printStackTrace();
                            dubboResponseHolder.end(null);
                            return;
                        }
                        workerThreadPool.execute(() -> {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                //NOP
                            }
                            String response = (String) invocation.getArguments()[0];
                            DubboMessage dubboResponse = null;
                            try {
                                dubboResponse = ServerCodecHelper.toDubboMessage(
                                        RpcResult.success(
                                                invocation.getRequestId(),
                                                invocation.getSeriType(),
                                                response));
                            } catch (SerializationException e) {
                                LOGGER.error("Failed to serialize response for reason {} and exception: {}",
                                        e.getMessage(), e);
                                dubboResponseHolder.getChannelHandlerContext().channel().close();
                            }
                            dubboResponseHolder.end(dubboResponse);
                        });

                    }

                    @Override
                    public void shutdown() {

                    }
                })
                .build();

        dubboServer.start();
        return dubboServer;
    }
}
