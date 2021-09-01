package io.esastack.codec.dubbo.client.serialize;

import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ServerCodecHelper;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class SerializeHandlerTest {

    @Test
    public void test() throws InterruptedException {
        System.setProperty("dubbo.lite.enable.serialize.pool", "true");
        SerializeHandler serializeHandler = SerializeHandler.get();
        DubboMessage response = ServerCodecHelper.toDubboMessage(DubboRpcResult.success(0, (byte) 2, "ok"));
        final AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        serializeHandler.deserialize(response, new ResponseCallback() {

            @Override
            public boolean deserialized() {
                return true;
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
            public void onResponse(Object rpcResult) {
                result.set((String) ((DubboRpcResult) rpcResult).getValue());
                countDownLatch.countDown();
            }

            @Override
            public Type getGenericReturnType() {
                return null;
            }
        }, new HashMap<>());
        countDownLatch.await();
        assertEquals("ok", result.get());
    }

}
