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
package io.esastack.codec.serialization.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public interface DataOutputStream extends Closeable {

    void writeInt(int v) throws IOException;

    void writeByte(byte v) throws IOException;

    void writeBytes(byte[] b) throws IOException;

    void writeUTF(String v) throws IOException;

    void writeObject(Object obj) throws IOException;

    default void writeMap(Map<String, String> map) throws IOException {
        writeObject(map);
    }

    default void writeThrowable(Object obj) throws IOException {
        writeObject(obj);
    }

    default void writeEvent(Object data) throws IOException {
        writeObject(data);
    }

    void flush() throws IOException;
}
