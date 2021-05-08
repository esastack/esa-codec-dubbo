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
package io.esastack.codec.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.kryo.utils.KryoUtils;

import java.io.IOException;
import java.io.OutputStream;

public class KryoDataOutputStream implements DataOutputStream {

    private Output output;
    private Kryo kryo;

    public KryoDataOutputStream(OutputStream outputStream) {
        output = new Output(outputStream);
        this.kryo = KryoUtils.get();
    }

    @Override
    public void writeUTF(String v) throws IOException {
        output.writeString(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        output.writeByte(v);
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        if (b == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(b.length);
            output.write(b, 0, b.length);
        }
    }

    @Override
    public void writeObject(Object v) throws IOException {
        kryo.writeClassAndObject(output, v);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
