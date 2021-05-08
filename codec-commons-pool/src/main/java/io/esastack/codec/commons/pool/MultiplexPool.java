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

import java.util.concurrent.CompletableFuture;

/**
 * 多路复用pool
 */
public interface MultiplexPool<T> {

    /**
     * acquire element from the pool.
     * Try acquire until successful within the given try times {@link DefaultMultiplexPool.Builder#getMaxRetryTimes()}.
     * In the last try times, this method will wait for the element to complete the creating process
     * until {@link DefaultMultiplexPool.Builder#getMaxWaitCreateTime()}.
     * So the {@link PooledObjectFactory} create method cannot take too long time.
     * It's better to have a {@code maxCreateTime}.
     *
     */
    CompletableFuture<T> acquire();

    /**
     * estimate whether there is element ready for acquiring at current time
     * the result is just a estimation, will soon be expired
     *
     */
    boolean canAcquire();

    /**
     * close all elements in this pool.
     */
    void closeAll();

    /**
     * close the given {@code element}, even if the {@code element} doesn't belong to the pool.
     *
     */
    void close(T element);
}
