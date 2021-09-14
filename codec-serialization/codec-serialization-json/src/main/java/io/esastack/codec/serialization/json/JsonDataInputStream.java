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
package io.esastack.codec.serialization.json;

import com.fasterxml.jackson.core.type.TypeReference;
import io.esastack.codec.serialization.api.DataInputStream;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

public class JsonDataInputStream implements DataInputStream {

    private final BufferedReader reader;

    public JsonDataInputStream(InputStream in) {
        this.reader = new BufferedReader(new InputStreamReader(in));
    }

    @Override
    public int readInt() throws IOException {
        return read(int.class);
    }

    @Override
    public byte readByte() throws IOException {
        return read(byte.class);
    }

    @Override
    public byte[] readBytes() throws IOException {
        return readLine().getBytes();
    }

    @Override
    public String readUTF() throws IOException {
        return read(String.class);
    }

    @Override
    public <T> T readObject(final Class<T> cls) throws IOException, ClassNotFoundException {
        return read(cls);
    }

    @Override
    public <T> T readObject(final Class<T> cls, final Type genericType) throws IOException, ClassNotFoundException {
        if (genericType == null || genericType == cls) {
            return read(cls);
        }
        return read(genericType);
    }

    private <T> T read(Class<T> cls) throws IOException {
        String json = readLine();
        return JacksonUtil.getJsonMapper().readValue(json, cls);
    }

    <T> T read(Type genericType) throws IOException {
        String json = readLine();
        return JacksonUtil.getJsonMapper().readValue(json, new TypeReference<T>() {
            @Override
            public Type getType() {
                return genericType;
            }
        });
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        if (line == null || line.trim().length() == 0) {
            throw new EOFException();
        }
        return line;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
