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
package io.esastack.codec.dubbo.client.exception;

/**
 * RequestTimeoutException used when sending data timeout in oneway mode
 */
public class RequestTimeoutException extends RuntimeException {
    private static final long serialVersionUID = -7982373252593604818L;

    public RequestTimeoutException(String msg) {
        super(msg);
    }

    public RequestTimeoutException(Throwable t) {
        super(t);
    }

    public RequestTimeoutException(String msg, Throwable t) {
        super(msg, t);
    }
}
