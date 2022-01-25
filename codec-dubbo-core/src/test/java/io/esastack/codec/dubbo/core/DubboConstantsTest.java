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
package io.esastack.codec.dubbo.core;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DubboConstantsTest {
    @Test
    public void test() {
        assertEquals("path", DubboConstants.PARAMETER_KEY.PATH_KEY);
        assertEquals("dubbo", DubboConstants.PARAMETER_KEY.DUBBO_VERSION_KEY);
        assertEquals("group", DubboConstants.PARAMETER_KEY.GROUP_KEY);
        assertEquals("interface", DubboConstants.PARAMETER_KEY.INTERFACE_KEY);
        assertEquals("method", DubboConstants.PARAMETER_KEY.METHOD_KEY);
        assertEquals("methods", DubboConstants.PARAMETER_KEY.METHODS_KEY);
        assertEquals("pid", DubboConstants.PARAMETER_KEY.PID_KEY);
        assertEquals("timestamp", DubboConstants.PARAMETER_KEY.TIMESTAMP_KEY);
        assertEquals("version", DubboConstants.PARAMETER_KEY.VERSION_KEY);
        assertEquals("DESC_OF_PARAMETER_OF_METHOD", DubboConstants.PARAMETER_KEY.METHOD_PARAMETER_DESC_KEY);


        assertEquals("$invoke", DubboConstants.GENERIC_INVOKE.$INVOKE);
        assertEquals("Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;",
                DubboConstants.GENERIC_INVOKE.PARAM_TYPES_DESE);
        assertEquals("$invokeAsync", DubboConstants.GENERIC_INVOKE.$INVOKE_ASYNC);
        assertEquals(3, DubboConstants.GENERIC_INVOKE.PARAM_TYPES.length);
        assertEquals(Object.class, DubboConstants.GENERIC_INVOKE.RETURN_TYPE);

        assertEquals((byte) 0x20, DubboConstants.HEADER_FLAG.FLAG_HEARTBEAT);
        assertEquals((byte) 0x80, DubboConstants.HEADER_FLAG.FLAG_REQUEST);
        assertEquals((byte) 0x40, DubboConstants.HEADER_FLAG.FLAG_TWOWAY);

        assertEquals((byte) 20, DubboConstants.RESPONSE_STATUS.OK);
        assertEquals((byte) 30, DubboConstants.RESPONSE_STATUS.CLIENT_TIMEOUT);
        assertEquals((byte) 40, DubboConstants.RESPONSE_STATUS.BAD_REQUEST);
        assertEquals((byte) 50, DubboConstants.RESPONSE_STATUS.BAD_RESPONSE);
        assertEquals((byte) 31, DubboConstants.RESPONSE_STATUS.SERVER_TIMEOUT);
        assertEquals((byte) 35, DubboConstants.RESPONSE_STATUS.CHANNEL_INACTIVE);
        assertEquals((byte) 60, DubboConstants.RESPONSE_STATUS.SERVICE_NOT_FOUND);
        assertEquals((byte) 70, DubboConstants.RESPONSE_STATUS.SERVICE_ERROR);
        assertEquals((byte) 80, DubboConstants.RESPONSE_STATUS.SERVER_ERROR);
        assertEquals((byte) 90, DubboConstants.RESPONSE_STATUS.CLIENT_ERROR);
        assertEquals((byte) 100, DubboConstants.RESPONSE_STATUS.SERVER_THREADPOOL_EXHAUSTED_ERROR);
    }
}
