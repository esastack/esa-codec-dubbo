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
package io.esastack.codec.serialization.serialize;

import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.api.SerializeFactory;
import io.esastack.codec.serialization.hessian2.Hessian2Serialization;
import io.esastack.codec.serialization.kryo.KryoSerialization;
import io.esastack.codec.serialization.protostuff.ProtostuffSerialization;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"LineLength", "TypeName", "MemberName"})
public class CompareTest {

    @BeforeClass
    public static void initTest() {
        SerializeFactory.getSerialization("hessian2");
    }

    @Test
    public void compareLong() throws Exception {
        serializeInt(new Hessian2Serialization());
        serializeInt(new ProtostuffSerialization());
        serializeInt(new KryoSerialization());

        serializeUTF8(new Hessian2Serialization());
        serializeUTF8(new ProtostuffSerialization());
        serializeUTF8(new KryoSerialization());

        serializeMap(new Hessian2Serialization());
        serializeMap(new ProtostuffSerialization());
        serializeMap(new KryoSerialization());

        serializeObj(new Hessian2Serialization());
        serializeObj(new ProtostuffSerialization());
        serializeObj(new KryoSerialization());
    }

    private int serializeInt(Serialization serialization) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
            dataOutputStream.writeInt(100);
            dataOutputStream.flush();
            System.out.println(serialization.getSeriName() + " serializeInt: " + byteArrayOutputStream.size());
            return byteArrayOutputStream.size();
        }
    }

    private int serializeUTF8(Serialization serialization) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
            dataOutputStream.writeUTF("12345678901234567890");
            dataOutputStream.flush();
            System.out.println(serialization.getSeriName() + " serializeUTF8: " + byteArrayOutputStream.size());
            return byteArrayOutputStream.size();
        }
    }

    private int serializeMap(Serialization serialization) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
            Map<String, String> map = new HashMap<>();
            map.put("1234567890", "1234567890");
            map.put("1234567891", "1234567890");
            map.put("1234567892", "1234567890");
            dataOutputStream.writeMap(map);
            dataOutputStream.flush();
            System.out.println(serialization.getSeriName() + " serializeMap: " + byteArrayOutputStream.size());
            return byteArrayOutputStream.size();
        }
    }

    private int serializeObj(Serialization serialization) throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            final DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
            final Model_ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd model1 = new Model_ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd();
            final Model_ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd model2 = new Model_ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd();
            model1.b = true;
            model1.c = (byte) 1;
            model1.i = 1000;
            model1.l = 10000;
            model1.s = 100;
            model1.utf = "1234567890";
            model1.utf11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111 = "12345678900987654321";
            model2.b = true;
            model2.c = (byte) 1;
            model2.i = 1000;
            model2.l = 10000;
            model2.s = 100;
            model2.utf = "1234567890";
            model2.utf11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111 = "12345678900987654321";
            model2.model_ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd = model1;
            dataOutputStream.writeObject(model2);
            dataOutputStream.flush();
            System.out.println(serialization.getSeriName() + " serializeObj: " + byteArrayOutputStream.size());
            return byteArrayOutputStream.size();
        }
    }

    private static class Model_ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd {
        private byte c;
        private short s;
        private int i;
        private long l;
        private boolean b;
        private String utf;
        private String utf11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111;
        private Model_ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd model_ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd;
    }
}
