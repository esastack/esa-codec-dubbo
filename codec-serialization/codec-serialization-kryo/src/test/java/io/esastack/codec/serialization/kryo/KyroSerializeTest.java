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
package io.esastack.codec.serialization.kryo;

import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.esastack.codec.serialization.kryo.utils.ReflectionUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class KyroSerializeTest {

    private static final Model model = new Model("name");
    private static final Throwable throwable = new RuntimeException("test");
    private static final Map<String, Object> map = new HashMap<String, Object>() {
        private static final long serialVersionUID = 8814600311412428553L;

        {
            put("key", model);
        }
    };

    @Test
    public void test() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        KryoSerialization serialization = new KryoSerialization();

        assertEquals("x-application/kryo", serialization.getContentType());
        assertEquals(SerializeConstants.KRYO_SERIALIZATION_ID, serialization.getSeriTypeId());
        assertEquals("kryo", serialization.getSeriName());

        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
        dataOutputStream.writeByte((byte) 1);
        dataOutputStream.writeBytes(new byte[]{1});
        dataOutputStream.writeInt(1);
        dataOutputStream.writeObject(1L);
        dataOutputStream.writeUTF("test");
        dataOutputStream.writeObject(model);
        dataOutputStream.writeEvent(model);
        dataOutputStream.writeThrowable(throwable);
        dataOutputStream.writeObject(map);
        dataOutputStream.flush();
        dataOutputStream.close();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        DataInputStream dataInputStream = serialization.deserialize(byteArrayInputStream);
        byte byte1 = dataInputStream.readByte();
        byte[] bytes1 = dataInputStream.readBytes();
        int int1 = dataInputStream.readInt();
        Long long1 = dataInputStream.readObject(Long.class);
        assertEquals((byte) 1, byte1);
        assertEquals((byte) 1, bytes1[0]);
        assertEquals(1, int1);
        assertEquals(1L, long1.longValue());
        String utf8 = dataInputStream.readUTF();
        assertEquals("test", utf8);
        Model model1 = dataInputStream.readObject(Model.class);
        Model model2 = dataInputStream.readObject(Model.class);
        Throwable t = dataInputStream.readThrowable();
        HashMap<String, Object> map1 = (HashMap<String, Object>) dataInputStream.readMap();
        assertEquals(model.name, model1.getName());
        assertEquals(model.name, model2.getName());
        assertEquals("test", t.getMessage());
        assertEquals(model.name, ((Model) map1.get("key")).name);
        dataInputStream.close();
        byteArrayInputStream.close();

        assertFalse(ReflectionUtils.checkZeroArgConstructor(User.class));
    }

    public static class Model implements Serializable {
        private static final long serialVersionUID = -1620673091375325983L;
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

    static class User {
        private String name;
        private int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
