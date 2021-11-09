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
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.netty.channel.ChannelOption;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class DubboSDKClientTests {
    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    static NettyDubboClient dubboNettyClient;

    static {
        Map<ChannelOption, Object> channelOptions = new HashMap<>();
        channelOptions.put(ChannelOption.SO_KEEPALIVE, true);
        channelOptions.put(ChannelOption.TCP_NODELAY, true);
        channelOptions.put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        channelOptions.put(ChannelOption.SO_RCVBUF, 1024);
        channelOptions.put(ChannelOption.SO_SNDBUF, 1024);
        NettyConnectionConfig.MultiplexPoolBuilder multiplexPoolBuilder =
                NettyConnectionConfig.MultiplexPoolBuilder.newBuilder()
                        .setInit(true)
                        .setMaxRetryTimes(3)
                        .setBlockCreateWhenInit(true)
                        .setWaitCreateWhenLastTryAcquire(true)
                        .setMaxPoolSize(10);
        NettyConnectionConfig connectionConfig = new NettyConnectionConfig()
                .setHost("localhost")
                .setPort(20880)
                .setMultiplexPoolBuilder(multiplexPoolBuilder)
                .setChannelOptions(channelOptions)
                .setConnectTimeout(3000);
        DubboClientBuilder builder = new DubboClientBuilder()
                .setConnectionConfig(connectionConfig)
                .setReadTimeout(4000)
                .setWriteTimeout(3000);

        dubboNettyClient = new NettyDubboClient(builder);
    }

    public static void main(String[] args) throws Exception {

        final AtomicLong requestCount = new AtomicLong();

        final AtomicLong errCount = new AtomicLong();

        final int totalRequest = 20 * 5000;

        final CountDownLatch countDownLatch = new CountDownLatch(totalRequest);

        final AtomicLong moreThan100Count = new AtomicLong();

        long s = System.currentTimeMillis();

        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                while (requestCount.incrementAndGet() <= totalRequest) {
                    long startTime = System.currentTimeMillis();

                    try {
                        RpcInvocation invocation = buildRpcInvocation();
                        DubboMessage dubboRequest = ClientCodecHelper.toDubboMessage(invocation);

                        CompletableFuture<DubboRpcResult> responseFuture = dubboNettyClient.sendRequest(dubboRequest,
                                invocation.getReturnType());

                        DubboRpcResult rpcResult = responseFuture.get();
                        if (rpcResult.getStatus() != 20) {
                            errCount.incrementAndGet();
                        }
                        long cost = System.currentTimeMillis() - startTime;

                        if (cost >= 1000) {
                            moreThan100Count.incrementAndGet();
                        }
                        countDownLatch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("requestCount=" + requestCount.get() + ",errCount=" + errCount.get() + ",qps=" +
                (requestCount.get() * 1000 / (System.currentTimeMillis() - s)) +
                ",moreThan1000=" + moreThan100Count.get());

    }

    public static RpcInvocation buildRpcInvocation() {
        Map<String, String> attachments = new HashMap<>();
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new String[]{"dubbo"});
        rpcInvocation.setInterfaceName("io.esastack.codec.dubbo.service.DemoService");
        rpcInvocation.setReturnType(String.class);
        rpcInvocation.setAttachments(attachments);
        return rpcInvocation;
    }
}
