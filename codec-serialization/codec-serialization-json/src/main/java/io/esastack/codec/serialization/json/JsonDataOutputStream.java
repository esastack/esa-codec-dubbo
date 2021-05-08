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

import io.esastack.codec.serialization.api.DataOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class JsonDataOutputStream implements DataOutputStream {

    private final PrintWriter writer;

    public JsonDataOutputStream(OutputStream out) {
        this.writer = new PrintWriter(out);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeByte(final byte v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeBytes(final byte[] b) throws IOException {
        writer.println(new String(b));
    }

    @Override
    public void writeUTF(final String v) throws IOException {
        writeObject(v);
    }

    @Override
    public void writeObject(final Object obj) throws IOException {
        final String content = JacksonUtil.getJsonMapper().writeValueAsString(obj);
        writer.println(content);
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }
}
