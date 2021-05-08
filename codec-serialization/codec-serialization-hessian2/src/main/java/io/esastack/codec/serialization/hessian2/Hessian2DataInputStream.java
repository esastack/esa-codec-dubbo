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
package io.esastack.codec.serialization.hessian2;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import io.esastack.codec.serialization.api.DataInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Hessian2 object input implementation
 */
public class Hessian2DataInputStream implements DataInputStream {
    private final Hessian2Input mH2i;

    public Hessian2DataInputStream(InputStream is) {
        mH2i = new Hessian2Input(is);
        mH2i.setSerializerFactory(Hessian2SerializerFactory.SERIALIZER_FACTORY);
    }

    @Override
    public String readUTF() throws IOException {
        return mH2i.readString();
    }

    @Override
    public int readInt() throws IOException {
        return mH2i.readInt();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) mH2i.readInt();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return mH2i.readBytes();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        return (T) mH2i.readObject(cls);
    }

    @Override
    public void close() throws IOException {
        mH2i.close();
    }
}
