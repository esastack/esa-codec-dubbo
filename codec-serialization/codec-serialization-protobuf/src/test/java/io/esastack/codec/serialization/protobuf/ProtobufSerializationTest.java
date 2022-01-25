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
import io.esastack.codec.serialization.api.SerializeConstants;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;
import io.esastack.codec.serialization.protobuf.wrapper.TestPB;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProtobufSerializationTest {

    private final Serialization serialization = new ProtobufSerialization();

    @Test
    public void test() {
        Assert.assertEquals(SerializeConstants.PROTOBUF_SERIALIZATION_ID, serialization.getSeriTypeId());
        Assert.assertEquals("x-application/protobuf", serialization.getContentType());
        Assert.assertEquals("protobuf", serialization.getSeriName());
    }

    @Test
    public void testNull() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
        dataOutputStream.writeObject(null);
        dataOutputStream.flush();

        DataInputStream dataInputStream = getDataInput(byteArrayOutputStream);
        assertEquals(dataInputStream.readObject(TestPB.PBRequestType.class),
                TestPB.PBRequestType.newBuilder().build());
    }

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

    @Test
    public void testMap() throws Exception {
        Map<String, String> attachment = new HashMap<>();
        attachment.put("k1", "v1");
        attachment.put("k2", "v2");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
        dataOutputStream.writeMap(attachment);
        dataOutputStream.writeMap(null);
        dataOutputStream.flush();

        DataInputStream dataInputStream = getDataInput(byteArrayOutputStream);
        assertEquals(dataInputStream.readMap(), attachment);
        assertEquals(dataInputStream.readMap().size(), 0);
    }

    @Test
    public void testThrowable() throws Exception {
        RuntimeException exception = new RuntimeException("test error");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
        dataOutputStream.writeThrowable(exception);
        dataOutputStream.flush();

        DataInputStream dataInputStream = getDataInput(byteArrayOutputStream);
        Throwable throwable = dataInputStream.readThrowable();
        assertTrue(throwable instanceof ProtobufWrappedException);
    }

    @Test
    public void testMultiTypes() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
        dataOutputStream.writeByte((byte) 1);

        byte[] bytes = new byte[10];
        new Random().nextBytes(bytes);
        dataOutputStream.writeBytes(bytes);

        String msg = "test message!";
        dataOutputStream.writeUTF(msg);
        dataOutputStream.writeUTF(null);

        ProtobufUtil.register(TestPB.PBRequestType.getDefaultInstance());
        TestPB.PBRequestType request = getPbMessage();
        dataOutputStream.writeObject(request);

        ProtobufUtil.register(TestPB.PhoneNumber.getDefaultInstance());
        TestPB.PhoneNumber phoneNumber = TestPB.PhoneNumber.newBuilder()
                .setNumber("1234567890").setTypeValue(2).build();
        dataOutputStream.writeObject(phoneNumber);

        Map<String, String> attachment = new HashMap<>();
        attachment.put("k1", "v1");
        attachment.put("k2", "v2");
        dataOutputStream.writeMap(attachment);

        dataOutputStream.flush();

        DataInputStream dataInputStream = getDataInput(byteArrayOutputStream);
        assertEquals(dataInputStream.readByte(), (byte) 1);
        assertArrayEquals(dataInputStream.readBytes(), bytes);
        assertEquals(dataInputStream.readUTF(), msg);
        assertEquals(dataInputStream.readUTF(), "");
        assertEquals(dataInputStream.readObject(TestPB.PBRequestType.class), request);
        assertEquals(dataInputStream.readObject(TestPB.PhoneNumber.class), phoneNumber);
        assertEquals(dataInputStream.readMap(), attachment);
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
