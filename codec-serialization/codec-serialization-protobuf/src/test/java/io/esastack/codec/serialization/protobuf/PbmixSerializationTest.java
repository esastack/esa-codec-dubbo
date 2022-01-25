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

import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.esastack.codec.serialization.protobuf.entity.User;
import io.esastack.codec.serialization.protobuf.entity.UserProto2Entity;
import io.esastack.codec.serialization.protobuf.entity.UserProto3Entity;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PbmixSerializationTest {

    private final Serialization serialization = new PbmixSerialization();

    @Test
    public void test() {
        assertEquals(SerializeConstants.PBMIX_SERIALIZATION_ID, serialization.getSeriTypeId());
        assertEquals("x-application/pb", serialization.getContentType());
        assertEquals("pbmix", serialization.getSeriName());
    }

    @Test
    public void test_proto2_messageLite() {
        UserProto2Entity.User user = UserProto2Entity.User
                .newBuilder().setId(1).setName("Stephen").setEmail("xxx.@oppo.com").build();
        byte[] userBytes = serialize(user);
        Object object = deserialize(userBytes, UserProto2Entity.User.class);

        assertNotNull(object);
        assertEquals(user, object);
    }

    @Test
    public void test_proto2_messageLite_builder() {
        UserProto2Entity.User.Builder builder =
                UserProto2Entity.User.newBuilder().setId(1).setName("Stephen").setEmail("xxx.@oppo.com");
        byte[] serialize = serialize(builder);
        Object object = deserialize(serialize, UserProto2Entity.User.Builder.class);

        assertTrue(object instanceof UserProto2Entity.User.Builder);
        UserProto2Entity.User deserializeUser = ((UserProto2Entity.User.Builder) object).build();
        assertEquals(deserializeUser.getId(), builder.build().getId());
    }

    @Test
    public void test_proto3_messageLite() {
        UserProto3Entity.User user =
                UserProto3Entity.User.newBuilder().setId(1).setName("Stephen").setEmail("xxx.@oppo.com").build();
        byte[] userBytes = serialize(user);
        Object object = deserialize(userBytes, UserProto3Entity.User.class);
        assertEquals(user, object);
    }

    @Test
    public void test_proto3_messageLite_builder() {
        UserProto3Entity.User.Builder builder =
                UserProto3Entity.User.newBuilder().setId(1).setName("Stephen").setEmail("xxx.@oppo.com");
        byte[] serialize = serialize(builder);
        Object object = deserialize(serialize, UserProto3Entity.User.Builder.class);

        assertTrue(object instanceof UserProto3Entity.User.Builder);
        UserProto3Entity.User deserializeUser = ((UserProto3Entity.User.Builder) object).build();
        assertEquals(deserializeUser.getId(), builder.build().getId());
    }

    @Test
    public void test_map_type() {
        Map<String, String> map = new HashMap<>();
        map.put("id", "1");
        map.put("name", "Stephen");
        map.put("email", "xxx@oppo.com");

        byte[] serialize = serialize(map);
        Object object = deserialize(serialize, Map.class);
        Map readMap = (Map) object;
        assertEquals(map.get("id"), readMap.get("id"));
    }

    @Test
    public void test_not_protobuf_entity() {
        User user = new User();
        user.setAge(18);
        user.setName("Stephen");
        byte[] serialize = serialize(user);
        Object deUser = deserialize(serialize, User.class);
        assertTrue(deUser instanceof User);
        assertEquals(((User) deUser).getName(), "Stephen");
    }

    @Test
    public void testMultiTypes() throws Exception {
        User user = new User();
        user.setAge(18);
        user.setName("Stephen");

        UserProto2Entity.User user2 = UserProto2Entity.User.newBuilder()
                .setId(1).setName("Stephen").setEmail("xxx.@oppo.com").build();
        UserProto3Entity.User user3 = UserProto3Entity.User.newBuilder()
                .setId(1).setName("Stephen").setEmail("xxx.@oppo.com").build();

        byte[] serialize = serialize(user, user2, user3);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(serialize);
        DataInputStream deserialize = serialization.deserialize(inputStream);
        User readUser = deserialize.readObject(User.class);
        UserProto2Entity.User readUser2 = deserialize.readObject(UserProto2Entity.User.class);
        UserProto3Entity.User readUser3 = deserialize.readObject(UserProto3Entity.User.class);

        assertEquals(user, readUser);
        assertEquals(user2, readUser2);
        assertEquals(user3, readUser3);
    }

    private byte[] serialize(Object... objects) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            DataOutputStream dataOutputStream = serialization.serialize(outputStream);
            for (Object object : objects) {
                dataOutputStream.writeObject(object);
            }
            dataOutputStream.flush();
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                //NOP
            }
        }

        return outputStream.toByteArray();
    }

    private Object deserialize(byte[] bytes, Class<?> returnType) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        Object object = null;
        try {
            DataInputStream deserialize = serialization.deserialize(inputStream);
            object = deserialize.readObject(returnType);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                //NOP
            }
        }

        return object;
    }
}

