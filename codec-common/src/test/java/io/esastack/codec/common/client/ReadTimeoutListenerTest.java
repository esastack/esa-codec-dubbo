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
package io.esastack.codec.common.client;

import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.exception.RequestTimeoutException;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.local.LocalChannel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

public class ReadTimeoutListenerTest {
    private final AtomicReference<Throwable> throwable = new AtomicReference<>();
    private final ResponseCallback callback = new ResponseCallback() {

        @Override
        public boolean deserialized() {
            return false;
        }

        @Override
        public void onResponse(final Object result) {

        }

        @Override
        public void onError(Throwable e) {
            throwable.set(e);
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
    };

    private final Timeout timeout = new Timeout() {
        @Override
        public Timer timer() {
            return null;
        }

        @Override
        public TimerTask task() {
            return null;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean cancel() {
            return false;
        }
    };

    @Test
    public void testTimeout() {
        DefaultChannelPromise promise = new DefaultChannelPromise(new LocalChannel());
        HashMap<Long, ResponseCallback> map = new HashMap<>();
        map.put(1L, callback);
        ReadTimeoutListener readTimeoutListener =
                new ReadTimeoutListener(100L, 1L, map, promise);
        readTimeoutListener.run(timeout);
        assertTrue(throwable.get() instanceof RequestTimeoutException);
        map.put(1L, callback);
        promise.setFailure(new IllegalArgumentException());
        readTimeoutListener.run(timeout);
        assertTrue(throwable.get() instanceof ConnectFailedException);
    }
}
