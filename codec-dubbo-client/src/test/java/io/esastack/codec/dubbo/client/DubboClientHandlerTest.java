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

import io.esastack.codec.dubbo.client.handler.DubboClientHandler;
import io.esastack.codec.dubbo.client.handler.ExceptionHandler;
import io.esastack.codec.dubbo.client.handler.IdleEventHandler;
import io.esastack.codec.dubbo.core.RpcResult;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertFalse;

public class DubboClientHandlerTest {
    private final Map<Long, ResponseCallback> callbackMap = new ConcurrentHashMap<>();
    private final ResponseCallback dubboResponseCallback = new ResponseCallbackWithDeserialization() {
        @Override
        public void onResponse(RpcResult rpcResult) {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onGotConnection(boolean b, String errMsg) {

        }

        @Override
        public void onWriteToNetwork(boolean isSuccess, String errMsg) {

        }

        @Override
        public Class<?> getReturnType() {
            return null;
        }

        @Override
        public Type getGenericReturnType() {
            return null;
        }
    };

    @Test
    public void exceptionCaught() {
        callbackMap.put(1L, dubboResponseCallback);
        DubboClientHandler clientHandler = new DubboClientHandler("test", callbackMap);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new ExceptionHandler(), clientHandler);
        assertFalse(embeddedChannel.writeInbound(Unpooled.EMPTY_BUFFER));
    }

    @Test
    public void userEventTriggered() {
        callbackMap.put(1L, dubboResponseCallback);
        DubboClientHandler clientHandler = new DubboClientHandler("test", callbackMap);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new IdleEventHandler(), clientHandler);
        for (int i = 0; i < 3; i++) {
            try {
                embeddedChannel.writeInbound(Unpooled.EMPTY_BUFFER);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
