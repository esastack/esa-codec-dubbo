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
package io.esastack.codec.common.connection;

import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.exception.TslHandshakeFailedException;
import io.esastack.codec.common.server.CustomNettyServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.esastack.codec.common.connection.NettyConnectionTest.createServerConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PooledNettyConnectionFactoryTest {

    private static volatile CustomNettyServer server;

    @BeforeClass
    public static void init() {
        server = new CustomNettyServer(createServerConfig());
        server.start();
    }

    @AfterClass
    public static void close() {
        server.shutdown();
    }

    private static NettyConnectionConfig createConnectConfig(int port) {
        NettyConnectionConfig config = new NettyConnectionConfig();
        config.setHost("127.0.0.1");
        config.setPort(port);
        return config;
    }

    @Test
    public void test() throws ExecutionException, InterruptedException {
        PooledNettyConnectionFactory factory = new PooledNettyConnectionFactory(createConnectConfig(20880));
        CompletableFuture<NettyConnection> future = factory.create();
        NettyConnection connection = future.get();
        assertTrue(factory.validate(connection));

        factory.destroy(connection).whenComplete((v, e) -> assertTrue(factory.validate(connection)));

        NettyConnection connection1 = new NettyConnection(new NettyConnectionConfig(), null);
        assertThrows(ConnectFailedException.class,
                () -> factory.connectSync(connection1, new ConnectFailedException("")));
        assertThrows(ConnectFailedException.class, () -> factory.connectSync(connection1, new RuntimeException()));
        NettyConnection connection2 = factory.connectSync(connection1, new TslHandshakeFailedException(""));
        assertTrue(factory.validate(connection2));
        assertEquals(connection1, factory.connectSync(connection1, null));
    }
}
