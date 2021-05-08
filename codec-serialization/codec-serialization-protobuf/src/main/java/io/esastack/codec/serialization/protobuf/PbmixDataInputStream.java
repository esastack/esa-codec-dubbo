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

import io.esastack.codec.serialization.hessian2.Hessian2DataInputStream;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;

import java.io.IOException;
import java.io.InputStream;

public class PbmixDataInputStream extends Hessian2DataInputStream {

    public PbmixDataInputStream(InputStream is) {
        super(is);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(Class<T> cls) throws IOException, ClassNotFoundException {
        int type = super.readInt();
        if (type == ProtobufUtil.Type.TYPE_NORMAL) {
            return super.readObject(cls);
        }
        String className = readUTF();
        byte[] data = readBytes();
        return (T) ProtobufUtil.parseFrom(className, data);
    }
}
