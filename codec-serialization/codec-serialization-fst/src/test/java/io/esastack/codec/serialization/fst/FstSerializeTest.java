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
package io.esastack.codec.serialization.fst;

import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.SerializeConstants;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class FstSerializeTest {

    private static final Model model = new Model("name");
    private static final Throwable throwable = new RuntimeException("test");
    private static final Map<String, Object> map = new HashMap<String, Object>() {
        private static final long serialVersionUID = -3639299266722489352L;

        {
            put("key", model);
        }
    };

    @Test
    public void test() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FstSerialization serialization = new FstSerialization();
        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);

        Assert.assertEquals(SerializeConstants.FST_SERIALIZATION_ID, serialization.getSeriTypeId());
        Assert.assertEquals("x-application/fst", serialization.getContentType());
        Assert.assertEquals("fst", serialization.getSeriName());

        dataOutputStream.writeByte((byte) 1);
        dataOutputStream.writeBytes(new byte[]{1});
        dataOutputStream.writeBytes(new byte[]{});
        dataOutputStream.writeBytes(null);
        dataOutputStream.writeInt(1);
        dataOutputStream.writeObject(1L);
        dataOutputStream.writeUTF("test");
        dataOutputStream.writeObject(model);
        dataOutputStream.writeEvent(model);
        dataOutputStream.writeThrowable(throwable);
        dataOutputStream.writeObject(map);
        dataOutputStream.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream dataInputStream = serialization.deserialize(byteArrayInputStream);
        byte byte1 = dataInputStream.readByte();
        byte[] bytes1 = dataInputStream.readBytes();
        byte[] bytes2 = dataInputStream.readBytes();
        byte[] bytes3 = dataInputStream.readBytes();
        int int1 = dataInputStream.readInt();
        Long long1 = dataInputStream.readObject(Long.class);
        final String utf8 = dataInputStream.readUTF();
        final Model model1 = dataInputStream.readObject(Model.class);
        final Model model2 = dataInputStream.readObject(Model.class);
        final Throwable t = dataInputStream.readThrowable();
        final HashMap<String, Object> map1 = (HashMap<String, Object>) dataInputStream.readMap();
        assertThrows(IOException.class, () -> dataInputStream.readObject(String.class));
        Assert.assertEquals((byte) 1, byte1);
        Assert.assertEquals((byte) 1, bytes1[0]);
        Assert.assertEquals(0, bytes2.length);
        Assert.assertNull(bytes3);
        Assert.assertEquals(1, int1);
        Assert.assertEquals(1L, long1.longValue());
        Assert.assertEquals("test", utf8);
        Assert.assertEquals(model.name, model1.getName());
        Assert.assertEquals(model.name, model2.getName());
        Assert.assertEquals("test", t.getMessage());
        Assert.assertEquals(model.name, ((Model) map1.get("key")).name);
        dataOutputStream.close();
        dataInputStream.close();
        byteArrayInputStream.close();
    }

    public static class Model implements Serializable {
        private static final long serialVersionUID = 8186201811039134052L;
        private String name;

        public Model() {
        }

        public Model(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
