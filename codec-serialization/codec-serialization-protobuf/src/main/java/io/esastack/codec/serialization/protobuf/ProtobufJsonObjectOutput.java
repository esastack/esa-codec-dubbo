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

import com.google.protobuf.*;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;
import io.esastack.codec.serialization.protobuf.wrapper.MapValue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ProtobufJsonObjectOutput implements DataOutputStream {

    private final PrintWriter writer;

    public ProtobufJsonObjectOutput(OutputStream out) {
        this.writer = new PrintWriter(new OutputStreamWriter(out));
    }
    
    @Override
    public void writeByte(byte v) throws IOException {
        writeObject(Int32Value.newBuilder().setValue((v)).build());
    }

    @Override
    public void writeInt(int v) throws IOException {
        writeObject(Int32Value.newBuilder().setValue(v).build());
    }

    @Override
    public void writeUTF(String v) throws IOException {
        writeObject(StringValue.newBuilder().setValue(v).build());
    }

    @Override
    public void writeBytes(byte[] b) throws IOException {
        writeObject(BytesValue.newBuilder().setValue(ByteString.copyFrom(b)).build());
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        if (obj == null) {
            throw new IllegalArgumentException(
                    "This serialization only support google protobuf object, the object is : null");
        }
        if (ProtobufUtil.isNotSupport(obj.getClass())) {
            throw new IllegalArgumentException(
                    "This serialization only support google protobuf object, the object class is: " +
                            obj.getClass().getName());
        }

        writer.write(ProtobufUtil.serializeJson(obj));
        writer.println();
        writer.flush();
    }

    @Override
    public void writeThrowable(Object th) throws IOException {
        if (th instanceof Throwable && ProtobufUtil.isNotSupport(th.getClass())) {
            th = ProtobufUtil.convertToThrowableValue((Throwable) th);
        }
        writer.write(ProtobufUtil.serializeJson(th));
        writer.println();
        writer.flush();
    }

    @Override
    public void writeMap(Map<String, String> attachments) throws IOException {
        if (attachments == null) {
            return;
        }

        Map<String, String> stringAttachments = new HashMap<>();
        attachments.forEach((k, v) -> stringAttachments.put(k, v));
        MapValue.Map proto = MapValue.Map.newBuilder().putAllAttachments(stringAttachments).build();
        writer.write(ProtobufUtil.serializeJson(proto));
        writer.println();
        writer.flush();
    }

    @Override
    public void flush() {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
