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

import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class DubboSDKDomainClientTests {
    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    static NettyDubboClient dubboNettyClient;

    static {
        DubboClientBuilder.MultiplexPoolBuilder multiplexPoolBuilder =
                DubboClientBuilder.MultiplexPoolBuilder.newBuilder().setMaxPoolSize(10);
        DubboClientBuilder clientConfig = new DubboClientBuilder()
                .setHost("localhost")
                .setPort(20880)
                .setMultiplexPoolBuilder(multiplexPoolBuilder)
                .setConnectTimeout(3000)
                .setWriteTimeout(3000)
                .setUnixDomainSocketFile("\0/data/uds/auds.sock");
        dubboNettyClient = new NettyDubboClient(clientConfig);
    }

    public static void main(String[] args) throws Exception {

        final AtomicLong requestCount = new AtomicLong();

        final AtomicLong errCount = new AtomicLong();

        final int totalRequest = 20 * 50000;

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

        System.out.println("requestCount=" + requestCount.get() + ",errCount=" + errCount.get() +
                ",qps=" + (requestCount.get() * 1000 / (System.currentTimeMillis() - s)) + ",moreThan1000=" +
                moreThan100Count.get());

    }

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
}
