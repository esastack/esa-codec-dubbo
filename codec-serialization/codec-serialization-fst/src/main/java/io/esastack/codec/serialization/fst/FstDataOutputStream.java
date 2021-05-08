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
package io.esastack.codec.serialization.fst;

import io.esastack.codec.serialization.api.DataOutputStream;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;
import java.io.OutputStream;

public class FstDataOutputStream implements DataOutputStream {

    private FSTObjectOutput output;

    public FstDataOutputStream(OutputStream outputStream) {
        output = FstFactory.getDefaultFactory().getObjectOutput(outputStream);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        output.writeUTF(v);
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
    public void writeBytes(byte[] v) throws IOException {
        if (v == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(v.length);
            output.write(v, 0, v.length);
        }
    }

    @Override
    public void writeObject(Object v) throws IOException {
        output.writeObject(v);
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
