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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class DubboClientTests {

    private static final org.apache.dubbo.demo.DemoService demoService;

    static {
        ReferenceConfig<org.apache.dubbo.demo.DemoService> reference = new ReferenceConfig<>();
        reference.setApplication(new ApplicationConfig("dubbo-demo-api-consumer"));
        reference.setRegistry(new RegistryConfig("N/A"));
        reference.setInterface(org.apache.dubbo.demo.DemoService.class);
        reference.setUrl("dubbo://127.0.0.1:20880");
        demoService = reference.get();
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
                        String result = demoService.sayHello("hello");
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
                ",qps=" + (requestCount.get() * 1000 / (System.currentTimeMillis() - s)) +
                ",moreThan1000=" + moreThan100Count.get());
    }
}
