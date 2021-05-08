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
import io.esastack.codec.serialization.hessian2.Hessian2DataOutputStream;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;

import java.io.IOException;
import java.io.OutputStream;

public class PbmixDataOutputStream extends Hessian2DataOutputStream {

    public PbmixDataOutputStream(OutputStream os) {
        super(os);
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        int type = (obj instanceof MessageLite) ? ProtobufUtil.Type.TYPE_PROTOBUF :
                (obj instanceof MessageLite.Builder ? ProtobufUtil.Type.TYPE_PROTOBUF_BUILDER :
                        ProtobufUtil.Type.TYPE_NORMAL);
        super.writeInt(type);

        byte[] data;
        switch (type) {
            case ProtobufUtil.Type.TYPE_PROTOBUF:
                writeUTF(obj.getClass().getName());
                data = ((MessageLite) obj).toByteArray();
                writeBytes(data);
                break;
            case ProtobufUtil.Type.TYPE_PROTOBUF_BUILDER:
                writeUTF(obj.getClass().getName());
                data = ((MessageLite.Builder) obj).build().toByteArray();
                writeBytes(data);
                break;
            default:
                super.writeObject(obj);
        }
    }
}
