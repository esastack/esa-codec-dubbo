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

import io.esastack.codec.common.exception.*;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AllExceptionTest {
    @Test
    public void testException() {
        Assert.assertEquals("java.lang.Throwable: ConnectFailed",
                new ConnectFailedException(new Throwable("ConnectFailed")).getMessage());
        Assert.assertEquals("RequestTimeout", new RequestTimeoutException("RequestTimeout").getMessage());
        assertEquals("RequestTimeout",
                new RequestTimeoutException("RequestTimeout", new Throwable()).getMessage());
        assertEquals("java.lang.Throwable: RequestTimeout",
                new RequestTimeoutException(new Throwable("RequestTimeout")).getMessage());
        Assert.assertEquals("ResponseTimeout",
                new ResponseTimeoutException("ResponseTimeout", new Throwable()).getMessage());
        assertEquals("java.lang.Throwable: ResponseTimeout",
                new ResponseTimeoutException(new Throwable("ResponseTimeout")).getMessage());
        Assert.assertEquals("TslHandshakeFailed", new TslHandshakeFailedException("TslHandshakeFailed").getMessage());
        assertEquals("TslHandshakeFailed",
                new TslHandshakeFailedException("TslHandshakeFailed", new Throwable()).getMessage());
        assertEquals("java.lang.Throwable: TslHandshakeFailed",
                new TslHandshakeFailedException(new Throwable("TslHandshakeFailed")).getMessage());
        Assert.assertEquals("UnknownResponseStatus",
                new UnknownResponseStatusException("UnknownResponseStatus").getMessage());
        assertEquals("UnknownResponseStatus",
                new UnknownResponseStatusException("UnknownResponseStatus", new Throwable()).getMessage());
    }
}
