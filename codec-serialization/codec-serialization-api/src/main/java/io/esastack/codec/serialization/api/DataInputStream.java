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
import java.lang.reflect.Type;
import java.util.Map;

public interface DataInputStream extends Closeable {

    int readInt() throws IOException;

    byte readByte() throws IOException;

    byte[] readBytes() throws IOException;

    String readUTF() throws IOException;

    <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException;

    default <T> T readObject(Class<T> cls, Type genericType) throws IOException, ClassNotFoundException {
        return readObject(cls);
    }

    default Throwable readThrowable() throws IOException, ClassNotFoundException {
        return readObject(Throwable.class);
    }

    default Map readMap() throws IOException, ClassNotFoundException {
        return readObject(Map.class);
    }
}
