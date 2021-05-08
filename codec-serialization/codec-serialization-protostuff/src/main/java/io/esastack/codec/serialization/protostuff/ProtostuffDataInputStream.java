/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
package io.esastack.codec.serialization.protostuff;

import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.protostuff.utils.WrapperUtils;
import io.protostuff.GraphIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.io.InputStream;

public class ProtostuffDataInputStream implements DataInputStream {

    private java.io.DataInputStream dis;

    public ProtostuffDataInputStream(InputStream inputStream) {
        dis = new java.io.DataInputStream(inputStream);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readObject(Class<T> clazzType) throws IOException, ClassNotFoundException {
        int classNameLength = dis.readInt();
        int bytesLength = dis.readInt();

        if (classNameLength < 0 || bytesLength < 0) {
            throw new IOException();
        }

        byte[] classNameBytes = new byte[classNameLength];
        dis.readFully(classNameBytes, 0, classNameLength);

        byte[] bytes = new byte[bytesLength];
        dis.readFully(bytes, 0, bytesLength);

        String className = new String(classNameBytes);
        Class clazz = Class.forName(className);

        Object result;
        if (WrapperUtils.needWrapper(clazz)) {
            Schema<Wrapper> schema = RuntimeSchema.getSchema(Wrapper.class);
            Wrapper wrapper = schema.newMessage();
            GraphIOUtil.mergeFrom(bytes, wrapper, schema);
            result = wrapper.getData();
        } else {
            Schema schema = RuntimeSchema.getSchema(clazz);
            result = schema.newMessage();
            GraphIOUtil.mergeFrom(bytes, result, schema);
        }

        return (T) result;
    }

    @Override
    public int readInt() throws IOException {
        return dis.readInt();
    }

    @Override
    public byte readByte() throws IOException {
        return dis.readByte();
    }

    @Override
    public byte[] readBytes() throws IOException {
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.read(bytes, 0, length);
        return bytes;
    }

    @Override
    public String readUTF() throws IOException {
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.read(bytes, 0, length);
        return new String(bytes);
    }

    @Override
    public void close() throws IOException {
        dis.close();
    }
}
