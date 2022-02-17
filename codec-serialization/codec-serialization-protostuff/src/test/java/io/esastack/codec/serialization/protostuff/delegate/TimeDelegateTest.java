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
package io.esastack.codec.serialization.protostuff.delegate;

import io.protostuff.WireFormat;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeDelegateTest {

    @Test
    public void testTimeDelegate() {
        TimeDelegate delegate = new TimeDelegate();
        assertEquals(WireFormat.FieldType.FIXED64, delegate.getFieldType());
    }

    @Test
    public void testTimestampDelegate() {
        TimestampDelegate delegate = new TimestampDelegate();
        assertEquals(WireFormat.FieldType.FIXED64, delegate.getFieldType());
    }
}
