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
package io.esastack.codec.serialization.protobuf;

import com.google.protobuf.MessageLite;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class SingleProtobufDataOutputStream implements DataOutputStream {

    private final OutputStream os;

    public SingleProtobufDataOutputStream(OutputStream os) {
        this.os = os;
    }

    @Override
    public void writeUTF(String v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeInt(int v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeByte(byte v) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        if (obj == null || ProtobufUtil.isNotSupport(obj.getClass())) {
            throw new IllegalArgumentException("This serialization only supports google protobuf objects, " +
                    "current object class is: " + obj.getClass().getName());
        }

        ((MessageLite) obj).writeTo(os);
        os.flush();
    }

    @Override
    public void writeMap(Map<String, String> map) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeThrowable(Object obj) throws IOException {
        if (obj instanceof Throwable && !(obj instanceof MessageLite)) {
            obj = ProtobufUtil.convertToThrowableValue((Throwable) obj);
        }
        writeObject(obj);
    }

    @Override
    public void flush() throws IOException {
        os.flush();
    }

    @Override
    public void close() throws IOException {
        os.close();
    }
}
