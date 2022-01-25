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
package io.esastack.codec.commons.pool;

import io.esastack.codec.commons.pool.exception.AcquireFailedException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MultiplexPoolTests {

    @Test
    public void testCanAcquire() throws Exception {
        MultiplexPool<String> pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(1)
                .factory(new MockPooledObjectFactory())
                .build();

        Assert.assertTrue(pool.canAcquire());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAcquire() throws Exception {
        MockPooledObjectFactory factory = new MockPooledObjectFactory();
        DefaultMultiplexPool.Builder builder = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(1)
                .maxRetryTimes(1)
                .maxWaitCreateTime(1000)
                .blockCreateWhenInit(false)
                .init(false)
                .waitCreateWhenLastTryAcquire(false)
                .factory(factory);
        MultiplexPool<String> pool = builder.build();

        CountDownLatch latch = new CountDownLatch(2);
        Worker worker1 = new Worker(latch, pool);
        Worker worker2 = new Worker(latch, pool);
        Thread t1 = new Thread(worker1);
        t1.setName("t1");
        t1.start();

        new Thread(worker2).start();

        latch.await();

        Assert.assertFalse(builder.isInit());
        Assert.assertFalse(builder.isBlockCreateWhenInit());
        Assert.assertFalse(builder.waitCreateWhenLastTryAcquire());
        Assert.assertEquals(builder.getMaxWaitCreateTime(), 1000);
        Assert.assertEquals(builder.getMaxRetryTimes(), 1);
        Assert.assertEquals(builder.getMaxPoolSize(), 1);
        Assert.assertEquals(builder.getFactory(), factory);

        Assert.assertEquals(worker1.getResult(), worker2.getResult());
        pool.close(worker1.getResult());
        pool.closeAll();
    }

    @Test
    public void testNonBlockInit_asyncGet() throws Exception {
        final MultiplexPool<String> pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(10)
                .init(true)
                .blockCreateWhenInit(false)
                .factory(new MockPooledObjectFactory())
                .waitCreateWhenLastTryAcquire(false)
                .build();
        final CountDownLatch latch = new CountDownLatch(10);
        final AtomicBoolean failed = new AtomicBoolean(false);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                final CompletableFuture<String> future = pool.acquire();
                try {
                    future.get();
                } catch (Throwable throwable) {
                    failed.set(true);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        Assert.assertFalse(failed.get());
    }

    @Test
    public void testAcuqireAsync() throws Exception {
        MultiplexPool<String> pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(1)
                .init(false)
                .factory(new MockPooledObjectFactory())
                .waitCreateWhenLastTryAcquire(false)
                .build();
        CompletableFuture<String> future = pool.acquire();
        AtomicReference<String> result = new AtomicReference<>();
        future.whenComplete((rt, ex) -> {
            if (rt != null) {
                result.set(rt);
            } else {
                ex.printStackTrace();
            }
        });
        future.get();
        Assert.assertEquals(result.get().substring(0, 6), "create");

        pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(1)
                .init(false)
                .factory(new MockPooledObjectFactory())
                .waitCreateWhenLastTryAcquire(true)
                .maxWaitCreateTime(500)
                .build();
        future = pool.acquire();
        AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
        future.whenComplete((rt, ex) -> {
            if (ex != null) {
                throwableAtomicReference.set(ex);
            }
        });
        try {
            future.get();
        } catch (Throwable ignored) {
        }
        Assert.assertEquals(throwableAtomicReference.get().getClass(), AcquireFailedException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAcquire_validateFalse() throws Exception {
        MultiplexPool<String> pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(1)
                .init(true)
                .blockCreateWhenInit(true)
                .factory(new ValidateFalsePoolFactory())
                .build();

        CountDownLatch latch = new CountDownLatch(2);
        Worker worker1 = new Worker(latch, pool);
        Worker worker2 = new Worker(latch, pool);

        new Thread(worker1).start();
        new Thread(worker2).start();
        latch.await();
        assertNull(worker1.getResult());
        assertNull(worker2.getResult());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAcquire_initPool() throws Exception {
        MultiplexPool<String> pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(2)
                .factory(new MockPooledObjectFactory())
                .init(true)
                .blockCreateWhenInit(true)
                .build();

        CountDownLatch latch = new CountDownLatch(2);
        Worker worker1 = new Worker(latch, pool);
        Worker worker2 = new Worker(latch, pool);

        new Thread(worker1).start();
        new Thread(worker2).start();
        latch.await();
        Assert.assertNotNull(worker1.getResult());
        Assert.assertNotNull(worker2.getResult());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAcquire_createExceptionally() {
        MultiplexPool<String> pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(2)
                .factory(new PooledObjectFactory() {

                    @Override
                    public CompletableFuture create() {
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                //NOP
                            }
                            throw new IllegalStateException("create error");
                        });
                    }

                    @Override
                    public CompletableFuture<Void> destroy(Object object) {
                        return null;
                    }

                    @Override
                    public Boolean validate(Object object) {
                        return false;
                    }
                })
                .build();

        CompletableFuture<String> future = pool.acquire();
        try {
            future.get();
        } catch (Throwable throwable) {
            assertTrue(throwable.getCause() instanceof IllegalStateException);
            assertEquals("create error", throwable.getCause().getMessage());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_waitCreateWhenLastTryAcquire_false() {
        DefaultMultiplexPool<String> pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(2)
                .waitCreateWhenLastTryAcquire(false)
                .factory(new ValidateFalsePoolFactory())
                .build();

        CompletableFuture<String> acquire = pool.acquire();
        try {
            acquire.get();
            Assert.fail();
        } catch (Throwable throwable) {
            assertTrue(throwable.getCause() instanceof AcquireFailedException);
            assertTrue(throwable.getCause().getMessage().startsWith("Acquire failed after try times:"));
        }
    }

    @Test
    public void test_maxWaitCreateTime() {
        DefaultMultiplexPool<String> pool = new DefaultMultiplexPool.Builder<String>()
                .maxPoolSize(1)
                .factory(new MockPooledObjectFactory(2000))
                .blockCreateWhenInit(true)
                .init(false)
                .maxWaitCreateTime(1000)
                .build();

        CompletableFuture<String> acquire = pool.acquire();
        try {
            acquire.get();
        } catch (Throwable throwable) {
            assertTrue(throwable.getCause() instanceof AcquireFailedException);
            assertTrue(throwable.getCause().getMessage().startsWith("Acquire timeout"));
        }
    }

    @Test
    public void testAcquireFailedException() {
        RuntimeException exception = new RuntimeException();
        AcquireFailedException exception1 = new AcquireFailedException("test");
        AcquireFailedException exception2 = new AcquireFailedException("test", exception);
        assertEquals("test", exception1.getMessage());
        assertEquals("test", exception2.getMessage());
        assertEquals(exception, exception2.getCause());
    }

    @Test
    public void testInit() {
        DefaultMultiplexPool.Builder<String> builder = new DefaultMultiplexPool.Builder<>();
        builder.maxPoolSize(-1);
        assertThrows(IllegalArgumentException.class, builder::build);

        builder.maxPoolSize(5);
        builder.maxRetryTimes(-1);
        assertThrows(IllegalArgumentException.class, builder::build);

        builder.maxRetryTimes(2);
        builder.factory(null);
        assertThrows(IllegalArgumentException.class, builder::build);

        builder.factory(new ValidateFalsePoolFactory());
        builder.init(true);
        builder.build();
    }

    private static class Worker implements Runnable {

        private CountDownLatch latch;

        private MultiplexPool<String> pool;

        private String result;

        public Worker(CountDownLatch latch, MultiplexPool<String> pool) {
            this.latch = latch;
            this.pool = pool;
        }

        public String getResult() {
            return result;
        }

        @Override
        public void run() {
            try {
                result = pool.acquire().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            latch.countDown();
        }
    }

    private static class ValidateFalsePoolFactory implements PooledObjectFactory<String> {

        private final AtomicInteger index = new AtomicInteger();

        @Override
        public CompletableFuture<String> create() {
            return CompletableFuture.completedFuture("create-" + index.getAndIncrement());
        }

        @Override
        public CompletableFuture<Void> destroy(String object) {
            return null;
        }

        @Override
        public Boolean validate(String object) {
            return false;
        }
    }
}
