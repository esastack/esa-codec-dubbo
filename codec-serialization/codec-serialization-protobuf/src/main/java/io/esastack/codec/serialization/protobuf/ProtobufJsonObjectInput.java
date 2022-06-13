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

import com.google.protobuf.BytesValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;
import io.esastack.codec.serialization.protobuf.wrapper.MapValue;
import io.esastack.codec.serialization.protobuf.wrapper.ThrowableValue;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class ProtobufJsonObjectInput implements DataInputStream {

    private final BufferedReader reader;

    public ProtobufJsonObjectInput(InputStream in) {
        this.reader = new BufferedReader(new InputStreamReader(in));
    }

    @Override
    public int readInt() throws IOException {
        return read(Int32Value.class).getValue();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) read(Int32Value.class).getValue();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return read(BytesValue.class).getValue().toByteArray();
    }

    @Override
    public String readUTF() throws IOException {
        return read(StringValue.class).getValue();
    }

    @Override
    public <T> T readObject(final Class<T> cls) throws IOException, ClassNotFoundException {
        return read(cls);
    }

    @Override
    public Throwable readThrowable() throws IOException {
        String json = readLine();
        ThrowableValue.Throwable throwableProto = ProtobufUtil.deserializeJson(json, ThrowableValue.Throwable.class);
        return new ProtobufWrappedException(throwableProto);
    }

    @Override
    public Map readMap() throws IOException, ClassNotFoundException {
        String json = readLine();
        Map<String, String> attachments = ProtobufUtil.deserializeJson(json, MapValue.Map.class).getAttachmentsMap();
        Map<String, Object> genericAttachments = new HashMap<>();
        attachments.forEach((k, v) -> {
            genericAttachments.put(k, v);
        });
        return genericAttachments;
    }


    @Override
    public void close() throws IOException {
        reader.close();
    }

    private <T> T read(Class<T> cls) throws IOException {
        if (ProtobufUtil.isNotSupport(cls)) {
            throw new IllegalArgumentException(
                    "This serialization only support google protobuf entity, the class is :" + cls.getName());
        }

        String json = readLine();
        return ProtobufUtil.deserializeJson(json, cls);
    }

    private String readLine() throws IOException {
        String line = reader.readLine();
        if (line == null || line.trim().length() == 0) {
            throw new EOFException();
        }
        return line;
    }
}
