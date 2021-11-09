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

import io.esastack.codec.common.connection.NettyConnectionConfig;
import io.esastack.codec.common.connection.NettyConnectionConfig.MultiplexPoolBuilder;
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DubboSDKClientTest {
    private static final NettyDubboClient dubboNettyClient;

    static {
        Map<ChannelOption, Object> channelOptions = new HashMap<>(16);
        channelOptions.put(ChannelOption.SO_KEEPALIVE, true);
        channelOptions.put(ChannelOption.TCP_NODELAY, true);
        channelOptions.put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        channelOptions.put(ChannelOption.SO_RCVBUF, 1024);
        channelOptions.put(ChannelOption.SO_SNDBUF, 1024);
        List<ChannelHandler> channelHandlers = new ArrayList<>();
        channelHandlers.add(new ChannelHandler() {
            @Override
            public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

            }
        });
        MultiplexPoolBuilder multiplexPoolBuilder =
                MultiplexPoolBuilder.newBuilder()
                        .setInit(true)
                        .setMaxRetryTimes(3)
                        .setBlockCreateWhenInit(true)
                        .setWaitCreateWhenLastTryAcquire(true)
                        .setMaxPoolSize(10);

        NettyConnectionConfig connectionConfig = new NettyConnectionConfig()
                .setHost("localhost")
                .setPort(20880)
                .setMultiplexPoolBuilder(multiplexPoolBuilder)
                .setConnectTimeout(5000)
                .setChannelOptions(channelOptions)
                .setChannelHandlers(channelHandlers);
        DubboClientBuilder builder = new DubboClientBuilder()
                .setConnectionConfig(connectionConfig)
                .setReadTimeout(3000)
                .setWriteTimeout(1000);

        dubboNettyClient = new NettyDubboClient(builder);

    }

    public static void main(String[] args) throws Exception {
        RpcInvocation invocation = buildRpcInvocation();
        invocation.setSeriType(SerializeConstants.HESSIAN2_SERIALIZATION_ID);
        DubboMessage dubboRequest = ClientCodecHelper.toDubboMessage(invocation);

        CompletableFuture<DubboRpcResult> responseFuture =
                dubboNettyClient.sendRequest(dubboRequest, invocation.getReturnType());

        DubboRpcResult rpcResult = responseFuture.get(1000, TimeUnit.MILLISECONDS);
        System.out.println(rpcResult.getValue());
    }

    public static RpcInvocation buildRpcInvocation() {
        Map<String, String> attachments = new HashMap<>();
        attachments.put("test", "group");
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new Object[]{"dubbo"});
        rpcInvocation.setInterfaceName("io.esastack.codec.dubbo.service.DemoService");
        rpcInvocation.setReturnType(String.class);
        rpcInvocation.setAttachments(attachments);
        return rpcInvocation;
    }

}
