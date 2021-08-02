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

import io.esastack.codec.dubbo.client.exception.ConnectFailedException;
import io.esastack.codec.dubbo.client.exception.ResponseTimeoutException;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.esastack.codec.dubbo.core.ssl.DubboSslContextBuilder;
import io.esastack.codec.dubbo.server.NettyDubboServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NettyDubboClientTest {

    private static volatile NettyDubboClient client;

    private static volatile NettyDubboServer server;

    @BeforeClass
    public static void startServer() {
        try {
            server = DubboSDKServer.start(new String[0]);
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }

        client = createClient(20880);
    }

    @AfterClass
    public static void stopServer() {
        //server.shutdown();
    }

    @Test
    public void requestSuccess() {
        final DubboMessage request = createDubboMessage(String.class, false);
        final CompletableFuture<RpcResult> future = client.sendRequestAsync(request, String.class, 1000);
        try {
            RpcResult rpcResult = future.get();
            Assert.assertEquals(rpcResult.getValue(), "test");
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail();
        }

        //final DubboMessage timeoutRequest = createDubboMessage(String.class, false);
        //final CompletableFuture<RpcResult> timeoutFuture = client.sendRequestAsync(timeoutRequest, String.class, 5);
        //try {
        //    timeoutFuture.get();
        //} catch (Throwable ex) {
        //    ex.printStackTrace();
        //    assertEquals(ResponseTimeoutException.class, ex.getCause().getClass());
        //}
        //final DubboMessage onewayRequest = createDubboMessage(String.class, true);
        //CompletableFuture<RpcResult> onewayFuture = client.sendRequestAsync(onewayRequest, String.class, 1000);
        //try {
        //    RpcResult rpcResult = onewayFuture.get();
        //    assertNull(rpcResult.getValue());
        //} catch (Exception e) {
        //    e.printStackTrace();
        //    fail();
        //}
        //assert onewayRequest != null;
        //onewayRequest.getHeader().setOnewayWaited(true);
        //CompletableFuture<RpcResult> onewayWaitedRequest = client.sendRequestAsync(onewayRequest, String.class, 1000);
        //try {
        //    RpcResult rpcResult = onewayWaitedRequest.get();
        //    assertNull(rpcResult.getValue());
        //} catch (Exception ignore) {
        //}
    }

    @Test
    public void connectFailed() throws InterruptedException {
        Thread.sleep(1000);
        final DubboMessage request = createDubboMessage(String.class, false);
        final NettyDubboClient nettyDubboClient = createClient(20000);
        final CompletableFuture<RpcResult> future = nettyDubboClient.sendRequestAsync(request, String.class, 1000);
        try {
            future.get();
            fail();
        } catch (Throwable ex) {
            assertTrue(ex.getCause() instanceof ConnectFailedException);
        }
        final CompletableFuture<RpcResult> requestFuture = nettyDubboClient.sendRequestAsync(request, String.class);
        try {
            requestFuture.get();
            fail();
        } catch (Throwable ex) {
            assertTrue(ex.getCause() instanceof ConnectFailedException);
        }
        nettyDubboClient.close();
        assertFalse(nettyDubboClient.isActive());
    }

    @Test
    public void testDefaultValue() throws InterruptedException {
        Thread.sleep(1000);
        final DubboMessage byteRequest = createDubboMessage(byte.class, true);
        final DubboMessage shortRequest = createDubboMessage(short.class, true);
        final DubboMessage intRequest = createDubboMessage(int.class, true);
        final DubboMessage longRequest = createDubboMessage(long.class, true);
        final DubboMessage booleanRequest = createDubboMessage(boolean.class, true);
        final DubboMessage charRequest = createDubboMessage(char.class, true);
        final DubboMessage doubleRequest = createDubboMessage(double.class, true);
        final DubboMessage floatRequest = createDubboMessage(float.class, true);
        final DubboMessage timeoutRequest = createDubboMessage(int.class, true);
        CompletableFuture<RpcResult> byteResult = client.sendRequestAsync(byteRequest, byte.class, 1000);
        CompletableFuture<RpcResult> shortResult = client.sendRequestAsync(shortRequest, short.class, 1000);
        CompletableFuture<RpcResult> intResult = client.sendRequestAsync(intRequest, int.class, 1000);
        CompletableFuture<RpcResult> longResult = client.sendRequestAsync(longRequest, long.class, 1000);
        CompletableFuture<RpcResult> booleanResult = client.sendRequestAsync(booleanRequest, boolean.class, 1000);
        CompletableFuture<RpcResult> charResult = client.sendRequestAsync(charRequest, char.class, 1000);
        CompletableFuture<RpcResult> doubleResult = client.sendRequestAsync(doubleRequest, double.class, 1000);
        CompletableFuture<RpcResult> floatResult = client.sendRequestAsync(floatRequest, float.class, 1000);
        CompletableFuture<RpcResult> timeoutResult = client.sendRequestAsync(timeoutRequest, int.class, 1);
        try {
            assertEquals((byte) 0, byteResult.get().getValue());
            assertEquals((short) 0, shortResult.get().getValue());
            assertEquals(0, intResult.get().getValue());
            assertEquals(0L, longResult.get().getValue());
            assertEquals(false, booleanResult.get().getValue());
            assertEquals(Character.MIN_VALUE, charResult.get().getValue());
            assertEquals(0.0f, floatResult.get().getValue());
            assertEquals(0.0d, doubleResult.get().getValue());
            assertEquals(0, timeoutResult.get().getValue());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void requestTimeout() throws InterruptedException {
        Thread.sleep(1000);
        final DubboMessage request = createDubboMessage(String.class, false);
        final CompletableFuture<RpcResult> future = client.sendRequestAsync(request, String.class, 100);
        try {
            future.get();
            fail();
        } catch (Throwable ex) {
            assertTrue(ex.getCause() instanceof ResponseTimeoutException);
        }
    }

    //@Test
    //public void tlsFallbackToNormal() {
    //    final DubboMessage request = createDubboMessage();
    //    final CompletableFuture<RpcResult> future = tlsClient.sendRequestAsync(request, String.class, 1000);
    //    try {
    //        RpcResult rpcResult = future.get();
    //        Assert.assertEquals(rpcResult.getValue(), "test");
    //    } catch (Throwable ex) {
    //        ex.printStackTrace();
    //        Assert.fail();
    //    }
    //}

    private static NettyDubboClient createClient(int port) {
        final DubboClientBuilder.MultiplexPoolBuilder poolBuilder = DubboClientBuilder
                .MultiplexPoolBuilder
                .newBuilder()
                .setInit(false)
                .setBlockCreateWhenInit(false)
                .setWaitCreateWhenLastTryAcquire(false)
                .setMaxPoolSize(1);
        final DubboClientBuilder builder = new DubboClientBuilder()
                .setMultiplexPoolBuilder(poolBuilder)
                .setHost("127.0.0.1")
                .setPort(port);
        return new NettyDubboClient(builder);
    }

    private static NettyDubboClient createTlsClient(int port) {
        final DubboClientBuilder.MultiplexPoolBuilder poolBuilder = DubboClientBuilder
                .MultiplexPoolBuilder
                .newBuilder()
                .setInit(false)
                .setBlockCreateWhenInit(false)
                .setWaitCreateWhenLastTryAcquire(false)
                .setMaxPoolSize(1);
        final DubboClientBuilder builder = new DubboClientBuilder()
                .setMultiplexPoolBuilder(poolBuilder)
                .setTlsFallback2Normal(true)
                .setDubboSslContextBuilder(new DubboSslContextBuilder())
                .setHost("127.0.0.1")
                .setPort(port);
        return new NettyDubboClient(builder);
    }

    private static DubboMessage createDubboMessage(Class<?> returnType, boolean oneway) {
        final RpcInvocation invocation = new RpcInvocation();
        invocation.setInterfaceName("com.oppo.test.EchoService");
        invocation.setMethodName("echo");
        invocation.setReturnType(returnType);
        invocation.setSeriType((byte) 2);
        invocation.setParameterTypes(new Class[]{String.class});
        invocation.setArguments(new Object[]{"test"});
        invocation.setOneWay(oneway);
        try {
            return ClientCodecHelper.toDubboMessage(invocation);
        } catch (Exception e) {
            fail();
            return null;
        }
    }
}
