/*
 * Copyright 2022 OPPO ESA Stack Project
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
package io.esastack.codec.common.server;

import io.netty.channel.ChannelOption;
import org.junit.Test;

import java.util.Collections;

public class NettyServerTest {
    @Test
    public void testNettyServer() {
        NettyServerConfig config = new NettyServerConfig();
        config.setUnixDomainSocketFile("test");
        //assertThrows(RuntimeException.class, () -> new CustomNettyServer(config).start());

        config.setUnixDomainSocketFile(null);
        config.setChannelOptions(Collections.singletonMap(ChannelOption.SO_BACKLOG, 128));
        config.setChildChannelOptions(Collections.singletonMap(ChannelOption.SO_BACKLOG, 128));
        CustomNettyServer server = new CustomNettyServer(config);
        server.start();
        server.shutdown();
    }
}
