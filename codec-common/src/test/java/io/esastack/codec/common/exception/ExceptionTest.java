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
package io.esastack.codec.common.exception;

import io.esastack.codec.common.constant.Constants;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExceptionTest {

    private static final Exception EXCEPTION = new RuntimeException();

    @Test
    public void testConnectionFailedException() {
        ConnectFailedException exception = new ConnectFailedException("connect failed");
        ConnectFailedException exception1 = new ConnectFailedException(EXCEPTION);
        ConnectFailedException exception2 = new ConnectFailedException("connect failed", EXCEPTION);
        assertEquals("connect failed", exception.getMessage());
        assertEquals("connect failed", exception2.getMessage());
        assertEquals(EXCEPTION, exception1.getCause());
        assertEquals(EXCEPTION, exception2.getCause());
    }

    @Test
    public void testInternalException() {
        InternalException exception = new InternalException("connect failed");
        InternalException exception1 = new InternalException(EXCEPTION);
        InternalException exception2 = new InternalException("connect failed", EXCEPTION);
        assertEquals("connect failed", exception.getMessage());
        assertEquals("connect failed", exception2.getMessage());
        assertEquals(EXCEPTION, exception1.getCause());
        assertEquals(EXCEPTION, exception2.getCause());
    }

    @Test
    public void testRequestTimeoutException() {
        RequestTimeoutException exception = new RequestTimeoutException("connect failed");
        RequestTimeoutException exception1 = new RequestTimeoutException(EXCEPTION);
        RequestTimeoutException exception2 = new RequestTimeoutException("connect failed", EXCEPTION);
        assertEquals("connect failed", exception.getMessage());
        assertEquals("connect failed", exception2.getMessage());
        assertEquals(EXCEPTION, exception1.getCause());
        assertEquals(EXCEPTION, exception2.getCause());
    }

    @Test
    public void testResponseTimeoutException() {
        ResponseTimeoutException exception = new ResponseTimeoutException("connect failed");
        ResponseTimeoutException exception1 = new ResponseTimeoutException(EXCEPTION);
        ResponseTimeoutException exception2 = new ResponseTimeoutException("connect failed", EXCEPTION);
        assertEquals("connect failed", exception.getMessage());
        assertEquals("connect failed", exception2.getMessage());
        assertEquals(EXCEPTION, exception1.getCause());
        assertEquals(EXCEPTION, exception2.getCause());
    }

    @Test
    public void testSerializationException() {
        SerializationException exception = new SerializationException("connect failed");
        SerializationException exception1 = new SerializationException(EXCEPTION);
        SerializationException exception2 = new SerializationException("connect failed", EXCEPTION);
        assertEquals("connect failed", exception.getMessage());
        assertEquals("connect failed", exception2.getMessage());
        assertEquals(EXCEPTION, exception1.getCause());
        assertEquals(EXCEPTION, exception2.getCause());
    }

    @Test
    public void testTslHandshakeFailedException() {
        TslHandshakeFailedException exception = new TslHandshakeFailedException("connect failed");
        TslHandshakeFailedException exception1 = new TslHandshakeFailedException(EXCEPTION);
        TslHandshakeFailedException exception2 = new TslHandshakeFailedException("connect failed", EXCEPTION);
        assertEquals("connect failed", exception.getMessage());
        assertEquals("connect failed", exception2.getMessage());
        assertEquals(EXCEPTION, exception1.getCause());
        assertEquals(EXCEPTION, exception2.getCause());
    }

    @Test
    public void testUnknownProtocolException() {
        UnknownProtocolException exception = new UnknownProtocolException("connect failed");
        UnknownProtocolException exception1 = new UnknownProtocolException(EXCEPTION);
        UnknownProtocolException exception2 = new UnknownProtocolException("connect failed", EXCEPTION);
        assertEquals("connect failed", exception.getMessage());
        assertEquals("connect failed", exception2.getMessage());
        assertEquals(EXCEPTION, exception1.getCause());
        assertEquals(EXCEPTION, exception2.getCause());
    }

    @Test
    public void testUnknownResponseStatusException() {
        UnknownResponseStatusException exception = new UnknownResponseStatusException("connect failed");
        UnknownResponseStatusException exception2 = new UnknownResponseStatusException("connect failed", EXCEPTION);
        assertEquals("connect failed", exception.getMessage());
        assertEquals("connect failed", exception2.getMessage());
        assertEquals(EXCEPTION, exception2.getCause());
        assertEquals("CONNECTION_NAME", Constants.CHANNEL_ATTR_KEY.CONNECTION_NAME.name());
    }
}
