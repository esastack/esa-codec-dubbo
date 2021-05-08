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

public class DubboClientTest {

    private static final org.apache.dubbo.demo.DemoService demoService;

    static {
        ReferenceConfig<org.apache.dubbo.demo.DemoService> reference = new ReferenceConfig<>();
        reference.setApplication(new ApplicationConfig("dubbo-demo-api-consumer"));
        RegistryConfig registryConfig = new RegistryConfig("N/A");
        reference.setRegistry(registryConfig);
        reference.setInterface(org.apache.dubbo.demo.DemoService.class);
        reference.setUrl("dubbo://172.17.161.145:20880");
        reference.setTimeout(10000);
        demoService = reference.get();
    }

    public static void main(String[] args) throws Exception {
        demoService.sayHello("hello");
    }
}
