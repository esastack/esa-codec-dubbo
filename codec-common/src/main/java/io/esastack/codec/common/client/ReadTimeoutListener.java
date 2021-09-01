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
package io.esastack.codec.common.client;

import io.esastack.codec.common.ResponseCallback;
import io.esastack.codec.common.exception.ConnectFailedException;
import io.esastack.codec.common.exception.RequestTimeoutException;
import io.esastack.codec.common.exception.ResponseTimeoutException;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.Map;

/**
 * 针对连接超时的请求进行检测
 */
public class ReadTimeoutListener implements TimerTask {
    private final long requestId;
    private final long requestTimeout;
    private final Map<Long, ResponseCallback> callbackMap;
    private final ChannelFuture channelFuture;

    public ReadTimeoutListener(long requestTimeout, long requestId,
                               Map<Long, ResponseCallback> callbackMap, ChannelFuture channelFuture) {
        this.requestTimeout = requestTimeout;
        this.requestId = requestId;
        this.callbackMap = callbackMap;
        this.channelFuture = channelFuture;
    }

    @Override
    public void run(Timeout timeout) {
        final ResponseCallback callback = callbackMap.remove(requestId);
        if (callback != null) {
            if (channelFuture.isDone()) {
                if (channelFuture.isSuccess()) {
                    callback.onError(new ResponseTimeoutException("Response timeout: " + requestTimeout + " ms."));
                } else if (channelFuture.cause() != null) {
                    callback.onError(new ConnectFailedException("Failed to send data cause "
                            + channelFuture.cause().getMessage() + " and the exception is " + channelFuture.cause()));
                } else {
                    callback.onError(
                            new ConnectFailedException("Failed to send data because the sending was cancelled."));
                }
            } else {
                // Client sends data timeout
                callback.onError(new RequestTimeoutException("Client sends data timeout: " + requestTimeout + " ms."));
            }
        }
    }
}
