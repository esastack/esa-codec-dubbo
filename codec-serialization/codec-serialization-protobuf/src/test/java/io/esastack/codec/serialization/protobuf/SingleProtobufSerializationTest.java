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
import com.google.protobuf.InvalidProtocolBufferException;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;
import io.esastack.codec.serialization.protobuf.wrapper.TestPB;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import static io.esastack.codec.serialization.api.SerializeConstants.PROTOBUF_SINGLE_SERIALIZATION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SingleProtobufSerializationTest {
    private final Serialization serialization = new SingleProtobufSerialization();

    @Test
    public void testPBObject() throws Exception {
        assertEquals(PROTOBUF_SINGLE_SERIALIZATION_ID, serialization.getSeriTypeId());
        assertEquals("x-application/proto", serialization.getContentType());
        assertEquals("proto", serialization.getSeriName());

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
    public void testString() throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream serialize = serialization.serialize(byteArrayOutputStream);
        serialize.writeUTF("single");
        assertThrows(IllegalArgumentException.class, () -> serialize.writeUTF(null));
        assertThrows(IllegalArgumentException.class, () -> serialize.writeBytes(null));
        assertThrows(IllegalArgumentException.class, () -> serialize.writeMap(null));
        serialize.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        DataInputStream deserialize = serialization.deserialize(new ByteArrayInputStream(bytes));
        assertEquals("single", deserialize.readUTF());
    }

    @Test
    public void testInt() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream serialize = serialization.serialize(byteArrayOutputStream);
        serialize.writeInt(1);
        serialize.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        DataInputStream deserialize = serialization.deserialize(new ByteArrayInputStream(bytes));
        assertEquals(1, deserialize.readInt());
    }

    @Test
    public void testByte() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream serialize = serialization.serialize(byteArrayOutputStream);
        serialize.writeByte((byte) 97);
        serialize.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        DataInputStream deserialize = serialization.deserialize(new ByteArrayInputStream(bytes));
        assertEquals('a', deserialize.readByte());
    }

    @Test
    public void testBytes() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream serialize = serialization.serialize(byteArrayOutputStream);
        serialize.writeBytes(new byte[]{(byte) 97, (byte) 98});
        serialize.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        DataInputStream deserialize = serialization.deserialize(new ByteArrayInputStream(bytes));
        assertEquals(2, deserialize.readBytes().length);
    }

    @Test
    public void testMap() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream serialize = serialization.serialize(byteArrayOutputStream);
        serialize.writeMap(Collections.singletonMap("k", "v"));
        serialize.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        DataInputStream deserialize = serialization.deserialize(new ByteArrayInputStream(bytes));
        assertThrows(InvalidProtocolBufferException.class, deserialize::readMap);
    }

    @Test
    public void testThrowable() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream serialize = serialization.serialize(byteArrayOutputStream);
        serialize.writeThrowable(new RuntimeException());
        serialize.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        DataInputStream deserialize = serialization.deserialize(new ByteArrayInputStream(bytes));
        assertThrows(InvalidProtocolBufferException.class, deserialize::readThrowable);
        //assertNotNull(deserialize.readThrowable());
    }

    @Test
    public void testDefaultInst() {
        assertThrows(RuntimeException.class, () -> SingleProtobufDataInputStream.defaultInst(String.class));
    }

    @Ignore
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
