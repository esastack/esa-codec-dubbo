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

import io.esastack.codec.dubbo.core.RpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端构建
 */
public interface DubboClient {

    CompletableFuture<RpcResult> sendRequest(final DubboMessage request,
                                             final Class<?> returnType);

    CompletableFuture<RpcResult> sendRequest(final DubboMessage request,
                                             final Class<?> returnType,
                                             final Type genericReturnType);

    CompletableFuture<RpcResult> sendRequest(final DubboMessage request,
                                             final Class<?> returnType,
                                             final long timeout);

    CompletableFuture<RpcResult> sendRequest(final DubboMessage request,
                                             final Class<?> returnType,
                                             final Type genericReturnType,
                                             final long timeout);

    boolean isActive();

    void close();
}