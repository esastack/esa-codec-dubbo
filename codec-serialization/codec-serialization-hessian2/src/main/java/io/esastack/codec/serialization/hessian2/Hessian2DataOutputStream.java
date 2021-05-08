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

import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import io.esastack.codec.serialization.api.DataOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class Hessian2DataOutputStream implements DataOutputStream {
    private final Hessian2Output mH2o;

    public Hessian2DataOutputStream(OutputStream os) {
        mH2o = new Hessian2Output(os);
        mH2o.setSerializerFactory(Hessian2SerializerFactory.SERIALIZER_FACTORY);
    }

    @Override
    public void writeUTF(String v) throws IOException {
        mH2o.writeString(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        mH2o.writeInt(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        mH2o.writeInt(v);
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        mH2o.writeBytes(b);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        mH2o.writeObject(obj);
    }

    @Override
    public void flush() throws IOException {
        mH2o.flushBuffer();
    }

    @Override
    public void close() throws IOException {
        mH2o.close();
    }
}
