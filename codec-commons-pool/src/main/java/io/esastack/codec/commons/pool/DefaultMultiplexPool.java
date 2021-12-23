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

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.commons.pool.exception.AcquireFailedException;

import java.util.Map;
import java.util.concurrent.*;

public class DefaultMultiplexPool<T> implements MultiplexPool<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMultiplexPool.class);

    private final Builder<T> builder;

    private final ConcurrentMap<Integer, AcquireTask> pool;

    private DefaultMultiplexPool(final Builder<T> builder) {
        this.builder = builder;
        this.pool = new ConcurrentHashMap<>(builder.maxPoolSize);
        initPool();
    }

    private void initPool() {
        if (builder.maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize of the pool must bigger than 0");
        }

        if (builder.maxRetryTimes <= 0) {
            throw new IllegalArgumentException("maxRetryTimes must bigger than 0");
        }

        if (builder.factory == null) {
            throw new IllegalArgumentException("Pool element factory cannot be null");
        }

        if (builder.init) {
            try {
                for (int i = 0; i < builder.maxPoolSize; i++) {
                    acquireFromPool(i, builder.blockCreateWhenInit);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to init connection pool", e);
            }
        }
    }

    @Override
    public CompletableFuture<T> acquire() {
        final AcquireTask acquireTask = doAcquire0();
        final CompletableFuture<T> future = new CompletableFuture<>();
        if (acquireTask.isAcquired()) {
            future.complete(acquireTask.getResult());
        } else {
            lastTryAcquire(acquireTask, future);
        }
        return future;
    }

    /**
     * time cost is same as acquire();
     * if continual 'maxRetryTimes' elements are invalid, assume it cannot acquire now;
     *
     * @return
     */
    @Override
    public boolean canAcquire() {
        final AcquireTask acquireTask = doAcquire0();
        if (acquireTask.isAcquired()) {
            return true;
        }
        if (builder.waitCreateWhenLastTryAcquire) {
            acquireTask.waitForCreated();
            return acquireTask.isAcquired();
        }
        return false;
    }

    private AcquireTask doAcquire0() {
        AcquireTask acquireTask = null;
        int index = ThreadLocalRandom.current().nextInt(builder.maxPoolSize);
        for (int i = 1; i <= builder.maxRetryTimes; i++) {
            final int idxCopy = index;
            acquireTask = acquireFromPool(index);
            index = decrementIdx(index);
            //resource is not ready
            if (!acquireTask.isCompleted()) {
                continue;
            }
            //resource is valid
            if (acquireTask.isAcquired()) {
                break;
            } else {
                //resource is invalid
                destroy(acquireTask.getResult());
            }
            //Atomic update
            acquireTask = updateByIndex(idxCopy, acquireTask);
        }

        return acquireTask;
    }

    @Override
    public void closeAll() {
        for (AcquireTask task : pool.values()) {
            if (task != null && task.isCompleted() && task.getResult() != null) {
                builder.factory.destroy(task.getResult());
            }
        }
    }

    @Override
    public void close(final T element) {
        if (element == null) {
            return;
        }

        int index = -1;
        for (Map.Entry<Integer, AcquireTask> entry : pool.entrySet()) {
            AcquireTask task = entry.getValue();
            if (task != null && task.isCompleted() && element.equals(task.getResult())) {
                index = entry.getKey();
                break;
            }
        }

        if (index >= 0) {
            pool.remove(index);
        }
        builder.factory.destroy(element);
    }

    private void destroy(final T element) {
        if (element != null) {
            builder.factory.destroy(element);
        }
    }

    /**
     * last try acquire, wait create complete or throw exception by the {@link Builder#waitCreateWhenLastTryAcquire}.
     */
    private void lastTryAcquire(final AcquireTask acquireTask, final CompletableFuture<T> future) {
        if (acquireTask == null) {
            future.completeExceptionally(new AcquireFailedException("maxRetryTimes must be set bigger than 0"));
            return;
        }

        if (this.builder.waitCreateWhenLastTryAcquire) {
            acquireTask.waitForCreated();
            if (acquireTask.isCompleted()) {
                if (acquireTask.isAcquired()) {
                    future.complete(acquireTask.getResult());
                } else if (acquireTask.getException() != null) {
                    future.completeExceptionally(acquireTask.getException());
                } else {
                    future.completeExceptionally(new AcquireFailedException("Acquire failed after try times: " +
                            this.builder.maxRetryTimes));
                }
            } else {
                //AcquireTask is in the pool, no need to release
                future.completeExceptionally(
                        new AcquireFailedException("Acquire timeout [" + builder.getMaxWaitCreateTime() + "]ms"));
            }
        } else {
            acquireTask.createFuture.whenComplete((result, ex) -> {
                if (ex != null) {
                    if (ex instanceof CompletionException) {
                        ex = ex.getCause();
                    }
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Acquire failed after try times: " + this.builder.maxRetryTimes, ex);
                    }
                    future.completeExceptionally(ex);
                } else if (result != null && this.builder.factory.validate(result)) {
                    future.complete(result);
                } else {
                    future.completeExceptionally(new AcquireFailedException("Acquire failed after try times: " +
                            this.builder.maxRetryTimes));
                }
            });
        }
    }

    private int decrementIdx(final int index) {
        if (index == 0) {
            return this.builder.maxPoolSize - 1;
        }
        return index - 1;
    }

    private AcquireTask acquireFromPool(final int index) {
        return acquireFromPool(index, false);
    }

    /**
     * acquire from pool, only one can get acquire task to create.
     */
    private AcquireTask acquireFromPool(final int index, final boolean blocked) {
        final AcquireTask acquireTask = pool.get(index);
        if (acquireTask != null) {
            return acquireTask;
        }
        return pool.computeIfAbsent(index, integer -> new AcquireTask(blocked));
    }

    /**
     * Concurrent updating controlling, avoiding of resource leak
     */
    private AcquireTask updateByIndex(final int index, final AcquireTask oldTask) {
        final AcquireTask newTask = new AcquireTask(false);
        if (!pool.replace(index, oldTask, newTask)) {
            newTask.createFuture.whenComplete((result, throwable) -> {
                if (result != null) {
                    destroy(result);
                }
            });
        }
        return pool.get(index);
    }

    public static final class Builder<T> {

        private int maxPoolSize;
        private int maxRetryTimes = 3;
        private int maxWaitCreateTime = 3000;
        private boolean init = false;
        private boolean blockCreateWhenInit = true;
        private boolean waitCreateWhenLastTryAcquire = true;
        private PooledObjectFactory<T> factory;

        public DefaultMultiplexPool<T> build() {
            return new DefaultMultiplexPool<>(this);
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public Builder<T> maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public boolean isInit() {
            return init;
        }

        public Builder<T> init(Boolean init) {
            this.init = init;
            return this;
        }

        public Builder<T> blockCreateWhenInit(Boolean blockCreateWhenInit) {
            this.blockCreateWhenInit = blockCreateWhenInit;
            return this;
        }

        public boolean isBlockCreateWhenInit() {
            return this.blockCreateWhenInit;
        }

        public Builder<T> maxRetryTimes(int maxRetryTimes) {
            this.maxRetryTimes = maxRetryTimes;
            return this;
        }

        public int getMaxRetryTimes() {
            return maxRetryTimes;
        }

        public PooledObjectFactory<T> getFactory() {
            return factory;
        }

        public Builder<T> factory(PooledObjectFactory<T> factory) {
            this.factory = factory;
            return this;
        }

        public Builder<T> waitCreateWhenLastTryAcquire(boolean waitCreateWhenLastTryAcquire) {
            this.waitCreateWhenLastTryAcquire = waitCreateWhenLastTryAcquire;
            return this;
        }

        public boolean waitCreateWhenLastTryAcquire() {
            return this.waitCreateWhenLastTryAcquire;
        }

        public Builder<T> maxWaitCreateTime(int maxWaitCreateTime) {
            this.maxWaitCreateTime = maxWaitCreateTime;
            return this;
        }

        public int getMaxWaitCreateTime() {
            return this.maxWaitCreateTime;
        }
    }

    private class AcquireTask {

        private final CompletableFuture<T> createFuture;
        private volatile boolean completed;
        private volatile AcquireResult acquireResult;

        private AcquireTask(boolean block) {
            CompletableFuture<T> tmp;
            try {
                final CompletableFuture<T> future = builder.factory.create();
                if (block) {
                    try {
                        future.get();
                    } catch (Throwable e) {
                        //
                    }
                }
                tmp = future.whenComplete(this::handleWhenCreateCompleted);
            } catch (Throwable e) {
                tmp = new CompletableFuture<>();
                tmp.completeExceptionally(e);
            }
            this.createFuture = tmp;
        }

        private void handleWhenCreateCompleted(T result, Throwable throwable) {
            acquireResult = new AcquireResult(result, throwable);
            completed = true;
            synchronized (this) {
                notifyAll();
            }
        }

        public void waitForCreated() {
            final int timeout = builder.getMaxWaitCreateTime();
            synchronized (this) {
                final long start = System.currentTimeMillis();
                while (!completed && System.currentTimeMillis() - start < timeout) {
                    try {
                        wait(start + timeout - System.currentTimeMillis());
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        private boolean isCompleted() {
            return completed;
        }

        private boolean isAcquired() {
            return completed && acquireResult.result != null && builder.factory.validate(acquireResult.result);
        }

        private T getResult() {
            if (acquireResult.hasException()) {
                return null;
            } else {
                return acquireResult.getResult();
            }
        }

        private Throwable getException() {
            return acquireResult.getThrowable();
        }
    }

    private class AcquireResult {

        private final T result;

        private final Throwable throwable;

        public AcquireResult(T result, Throwable throwable) {
            this.result = result;
            this.throwable = throwable;
        }

        public boolean hasException() {
            return throwable != null;
        }

        public T getResult() {
            return result;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
