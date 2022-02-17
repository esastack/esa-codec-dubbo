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

import io.esastack.codec.serialization.api.SerializeConstants;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DubboRpcResultTest {
    @Test
    public void testConstructor() {
        DubboRpcResult result = DubboRpcResult.success(1L, SerializeConstants.HESSIAN2_SERIALIZATION_ID, "result");
        assertNull(result.getErrorMessage());
        assertEquals(DubboRpcResult.RESPONSE_FLAG.RESPONSE_VALUE, (byte) 1);

        DubboRpcResult result1 = new DubboRpcResult();
        result1.setErrorMessage("error");
        assertNotNull(result1.getException());
    }
}
