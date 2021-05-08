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
package io.esastack.codec.dubbo.core;

import io.esastack.codec.dubbo.core.exception.SerializationException;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExceptionTest {
    @Test
    public void testSerializationException() {
        Assert.assertEquals("Test", new SerializationException("Test", new Throwable()).getMessage());
        assertEquals("java.lang.Throwable: Test",
                new SerializationException(new Throwable("Test")).getMessage());
    }

    @Test
    public void testRpcResultError() {
        RpcResult error = RpcResult.error(1L, (byte) 2, new Throwable());
        assertNull(error.getValue());
    }
}
