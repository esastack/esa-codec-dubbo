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
package io.esastack.codec.common;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseCallbackTest {
    @Test
    public void testResponseCallback() {
        ResponseCallback responseCallback = new ResponseCallback() {
            @Override
            public void onResponse(Object result) {
            }

            @Override
            public void onError(Throwable e) {
            }
        };
        responseCallback.onGotConnection(true, "test");
        responseCallback.onWriteToNetwork(true, null);
        assertFalse(responseCallback.deserialized());
        assertNull(responseCallback.getReturnType());
        assertNull(responseCallback.getGenericReturnType());
    }
}
