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

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RpcResultTest {

    @Test
    public void testRpcResult() {

        RpcResult rpcResult = new RpcResult();
        assertNull(rpcResult.getValue());
        assertNull(rpcResult.getException());
        assertEquals(0, rpcResult.getAttachments().size());

        Exception exception = new RuntimeException();
        rpcResult = new RpcResult("res", exception);
        assertEquals(rpcResult.getException(), exception);
        assertEquals(rpcResult.getValue(), "res");
        assertEquals(0, rpcResult.getAttachments().size());

        HashMap<String, String> map = new HashMap<>();
        rpcResult.setAttachments(map);
        assertEquals(map, rpcResult.getAttachments());
        rpcResult.setAttachment("key", null);
        rpcResult.setAttachment("key", "value");
        assertEquals("value", rpcResult.getAttachments().get("key"));

        Exception exception1 = new RuntimeException();
        rpcResult.setValue("new");
        rpcResult.setException(exception1);

        assertEquals("new", rpcResult.getValue());
        assertEquals(exception1, rpcResult.getException());
    }

}
