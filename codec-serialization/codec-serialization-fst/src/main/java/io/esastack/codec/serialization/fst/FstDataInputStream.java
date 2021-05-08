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

import io.esastack.codec.serialization.api.DataInputStream;
import org.nustaq.serialization.FSTObjectInput;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fst object input implementation
 */
public class FstDataInputStream implements DataInputStream {

    //this is reusable, should not close it
    private final FSTObjectInput input;

    private final InputStream inputStream;

    public FstDataInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.input = FstFactory.getDefaultFactory().getObjectInput(inputStream);
    }

    @Override
    public String readUTF() throws IOException {
        return input.readUTF();
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt();
    }

    @Override
    public byte readByte() throws IOException {
        return input.readByte();
    }

    @Override
    public byte[] readBytes() throws IOException {
        int len = input.readInt();
        if (len < 0) {
            return null;
        } else if (len == 0) {
            return new byte[]{};
        } else {
            byte[] b = new byte[len];
            input.readFully(b);
            return b;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> clazz) throws IOException, ClassNotFoundException {
        try {
            return (T) input.readObject(clazz);
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof ClassNotFoundException) {
                throw (ClassNotFoundException) e;
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
