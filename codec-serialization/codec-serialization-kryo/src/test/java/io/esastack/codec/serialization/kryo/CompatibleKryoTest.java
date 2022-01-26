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
package io.esastack.codec.serialization.kryo;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CompatibleKryoTest {
    @Test
    public void test() {
        CompatibleKryo compatibleKryo = new CompatibleKryo();
        assertThrows(IllegalArgumentException.class, () -> compatibleKryo.getDefaultSerializer(null));
        assertNotNull(compatibleKryo.getDefaultSerializer(Inner.class));
        assertNotNull(compatibleKryo.getDefaultSerializer(String.class));
    }

    static class Inner {
        public Inner(String name) {
        }
    }
}
