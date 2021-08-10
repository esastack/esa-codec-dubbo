package io.esastack.codec.dubbo.client;

import io.esastack.codec.dubbo.client.exception.ConnectFailedException;
import io.esastack.codec.dubbo.client.exception.RequestTimeoutException;
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
    public void run() {
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
