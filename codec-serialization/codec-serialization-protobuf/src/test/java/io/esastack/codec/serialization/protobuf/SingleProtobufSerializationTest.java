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

import com.google.protobuf.ByteString;
import esa.commons.serialize.protobuf.wrapper.TestPB;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class SingleProtobufSerializationTest {

    private Serialization serialization = new SingleProtobufSerialization();

    @Test
    public void testPBObject() throws Exception {
        ProtobufUtil.register(TestPB.PBRequestType.getDefaultInstance());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
        TestPB.PBRequestType request = getPbMessage();

        dataOutputStream.writeObject(request);
        dataOutputStream.flush();

        DataInputStream dataInputStream = getDataInput(byteArrayOutputStream);
        assertEquals(dataInputStream.readObject(TestPB.PBRequestType.class), request);
    }

    private DataInputStream getDataInput(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        ByteArrayInputStream byteArrayInputStream =
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return serialization.deserialize(byteArrayInputStream);
    }

    private TestPB.PBRequestType getPbMessage() {
        return TestPB.PBRequestType
                .newBuilder()
                .setName("Stephen")
                .setAge(new Random().nextInt(20))
                .setCash(new Random().nextInt(1000000000))
                .setMsg(ByteString.copyFromUtf8("Oh, I'm rich!"))
                .setSex(true)
                .build();
    }
}
