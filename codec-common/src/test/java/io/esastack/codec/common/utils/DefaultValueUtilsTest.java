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
package io.esastack.codec.common.utils;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultValueUtilsTest {
    @Test
    public void testDefaultValueUtils() {
        assertNull(DefaultValueUtils.getUnboxedPrimitiveDefaultValue(String.class));
        assertEquals((byte) 0, DefaultValueUtils.getUnboxedPrimitiveDefaultValue(byte.class));
        assertEquals((short) 0, DefaultValueUtils.getUnboxedPrimitiveDefaultValue(short.class));
        assertEquals(Character.MIN_VALUE, DefaultValueUtils.getUnboxedPrimitiveDefaultValue(char.class));
        assertEquals(0, DefaultValueUtils.getUnboxedPrimitiveDefaultValue(int.class));
        assertEquals(0L, DefaultValueUtils.getUnboxedPrimitiveDefaultValue(long.class));
        assertEquals(0.0d, DefaultValueUtils.getUnboxedPrimitiveDefaultValue(double.class));
        assertEquals(0.0f, DefaultValueUtils.getUnboxedPrimitiveDefaultValue(float.class));
        assertFalse((Boolean) DefaultValueUtils.getUnboxedPrimitiveDefaultValue(boolean.class));
    }
}
