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

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.demo.DemoService;
import org.apache.dubbo.demo.DemoServiceImpl;
import org.junit.Ignore;

import java.util.concurrent.CountDownLatch;

@Ignore
public class DubboServer {
    static final CountDownLatch COUNTDOWN_LATCH = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        ServiceConfig<DemoServiceImpl> service = new ServiceConfig<>();
        service.setApplication(new ApplicationConfig("dubbo-demo-api-provider"));
        service.setRegistry(new RegistryConfig("N/A"));
        service.setInterface(DemoService.class);
        service.setRef(new DemoServiceImpl());
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setPort(20881);
        protocolConfig.setThreads(200);
        service.setProtocol(protocolConfig);
        service.export();
        COUNTDOWN_LATCH.await();
    }
}
