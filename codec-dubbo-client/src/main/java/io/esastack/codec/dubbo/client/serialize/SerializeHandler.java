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
package io.esastack.codec.dubbo.client.serialize;

import esa.commons.concurrent.ThreadFactories;
import io.esastack.codec.dubbo.client.ResponseCallbackWithDeserialization;
import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.esastack.codec.dubbo.core.utils.DubboConstants;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * If the serialize thread pool queue is full, do the serialization in IO threads;
 * The default pool queue size is POOL_SIZE, avoiding too much task queued and delayed.
 */
public class SerializeHandler {

    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2 + 1;

    private static final int MAX_QUEUE_SIZE = Integer.getInteger("dubbo.lite.serialize.queue.size", POOL_SIZE);

    private static final boolean ENABLE_SERIALIZE_POOL = Boolean.getBoolean("dubbo.lite.enable.serialize.pool");

    private static volatile SerializeHandler instance;

    private final ThreadPoolExecutor executor;

    public static SerializeHandler get() {
        if (instance == null) {
            synchronized (SerializeHandler.class) {
                if (instance == null) {
                    instance = new SerializeHandler();
                }
            }
        }
        return instance;
    }

    private SerializeHandler() {
        this.executor = new ThreadPoolExecutor(POOL_SIZE,
                POOL_SIZE,
                Integer.MAX_VALUE,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                ThreadFactories.namedThreadFactory("dubboLiteClient-serialize"));
    }

    /**
     * if the thread queue is full, do the deserialization in IO THREAD
     */
    public void deserialize(final DubboMessage response,
                            final ResponseCallbackWithDeserialization callback,
                            final Map<String, String> ttfbAttachments) {
        response.retain();
        if (ENABLE_SERIALIZE_POOL) {
            try {
                executor.submit(() -> doDeserialize(response, callback, ttfbAttachments));
            } catch (Throwable e) {
                doDeserialize(response, callback, ttfbAttachments);
            }
        } else {
            doDeserialize(response, callback, ttfbAttachments);
        }
    }

    private void doDeserialize(final DubboMessage response,
                               final ResponseCallbackWithDeserialization callback,
                               final Map<String, String> ttfbAttachments) {
        try {
            final long startAt = System.currentTimeMillis();
            ttfbAttachments.put(DubboConstants.TRACE.TIME_OF_RSP_DESERIALIZE_BEGIN_KEY, startAt + "");
            final RpcResult rpcResult = ClientCodecHelper.toRpcResult(
                    response, callback.getReturnType(), callback.getGenericReturnType(), ttfbAttachments);
            rpcResult.setAttachment(DubboConstants.TRACE.TIME_OF_RSP_DESERIALIZE_COST_KEY,
                    String.valueOf(System.currentTimeMillis() - startAt));
            callback.onResponse(rpcResult);
        } catch (Throwable t) {
            callback.onError(t);
        } finally {
            response.release();
        }
    }

}
