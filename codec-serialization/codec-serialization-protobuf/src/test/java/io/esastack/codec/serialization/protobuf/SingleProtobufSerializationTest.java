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
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;
import io.esastack.codec.serialization.protobuf.wrapper.TestPB;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import static io.esastack.codec.serialization.api.SerializeConstants.PROTOBUF_SINGLE_SERIALIZATION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SingleProtobufSerializationTest {
    private final Serialization serialization = new SingleProtobufSerialization();

    @Test
    public void testPBObject() throws Exception {
        ProtobufUtil.register(TestPB.PBRequestType.getDefaultInstance());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
        TestPB.PBRequestType request = getPbMessage();

        dataOutputStream.writeObject(request);
        dataOutputStream.flush();
        dataOutputStream.close();

        DataInputStream dataInputStream = getDataInput(byteArrayOutputStream);
        assertEquals(dataInputStream.readObject(TestPB.PBRequestType.class), request);
        dataInputStream.close();
    }

    @Test
    public void test() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SingleProtobufSerialization singleProtobufSerialization = new SingleProtobufSerialization();

        assertEquals(PROTOBUF_SINGLE_SERIALIZATION_ID, singleProtobufSerialization.getSeriTypeId());
        assertEquals("x-application/proto", singleProtobufSerialization.getContentType());
        assertEquals("proto", singleProtobufSerialization.getSeriName());

        SingleProtobufDataOutputStream serialize =
                (SingleProtobufDataOutputStream) singleProtobufSerialization.serialize(byteArrayOutputStream);
        assertThrows(UnsupportedOperationException.class, () -> serialize.writeByte((byte) '1'));
        assertThrows(UnsupportedOperationException.class, () -> serialize.writeBytes(new byte[]{}));
        assertThrows(UnsupportedOperationException.class, () -> serialize.writeInt(1));
        assertThrows(UnsupportedOperationException.class, () -> serialize.writeUTF("foo"));
        assertThrows(UnsupportedOperationException.class, () -> serialize.writeMap(new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> serialize.writeObject("foo"));
        serialize.flush();
        byte[] sourceBytes = byteArrayOutputStream.toByteArray();
        serialize.close();
        SingleProtobufDataInputStream deserialize =
                (SingleProtobufDataInputStream) singleProtobufSerialization.deserialize(
                        new ByteArrayInputStream(sourceBytes));
        assertThrows(UnsupportedOperationException.class, deserialize::readByte);
        assertThrows(UnsupportedOperationException.class, deserialize::readBytes);
        assertThrows(UnsupportedOperationException.class, deserialize::readInt);
        assertThrows(UnsupportedOperationException.class, deserialize::readUTF);
        assertThrows(UnsupportedOperationException.class, deserialize::readMap);
        assertThrows(IllegalArgumentException.class, () -> deserialize.readObject(String.class));
        deserialize.close();
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
