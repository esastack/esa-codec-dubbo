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
package io.esastack.codec.serialization.json;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;

public class JsonTest {

    public static void main(String[] args) throws Exception {
        final List<ModelOne<ModelTwo>> list = new ArrayList<>();
        ModelOne<ModelTwo> modelOne = new ModelOne<>();

        ModelTwo modelTwo = new ModelTwo();
        modelTwo.setName("name");

        ModelTwo[] modelTwoArray = new ModelTwo[1];
        modelTwoArray[0] = modelTwo;

        Map<String, ModelTwo> modelTwoMap = new HashMap<>();
        modelTwoMap.put(modelTwo.getName(), modelTwo);

        modelOne.setModelTwo(modelTwo);
        modelOne.setModelArray(modelTwoArray);
        modelOne.setModelMap(modelTwoMap);

        list.add(modelOne);

        JsonMapper mapper = new JsonMapper();
        //mapper.activateDefaultTypingAsProperty(null, NON_FINAL, null);
        String json = mapper.writeValueAsString(list);
        System.out.println(json);

        JsonMapper mapper1 = new JsonMapper();
        mapper1.activateDefaultTypingAsProperty(null, NON_FINAL, null);
        Object obj = mapper1.readValue(json, ArrayList.class);
        System.out.println(obj);
    }

    @Test
    public void readObjectTest() throws Exception{

        Model model = new Model();
        model.setName("wangwei");
        Object result = deserializeObj(model, Object.class);
        Assert.assertTrue(result instanceof Map);
        result = deserializeObj(model, Model.class);
        Assert.assertTrue(result instanceof Model);


        SubModel subModel = new SubModel();
        subModel.setName("wangwei");
        subModel.setAge(10);
        Object subResult = deserializeObj(subModel, Object.class);
        Assert.assertTrue(subResult instanceof Map);
        subResult = deserializeObj(subModel, Model.class);
        Assert.assertTrue(subResult instanceof Model);
        subResult = deserializeObj(subModel, SubModel.class);
        Assert.assertTrue(subResult instanceof SubModel);

    }

    private <T> T deserializeObj(final Object obj, final Class clazz) throws Exception {
        JsonSerialization serialization = new JsonSerialization();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = serialization.serialize(byteArrayOutputStream);
        dataOutputStream.writeObject(obj);
        dataOutputStream.flush();
        byte[] content = byteArrayOutputStream.toByteArray();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
        DataInputStream dataInputStream = serialization.deserialize(byteArrayInputStream);
        return (T) dataInputStream.readObject(clazz);
    }

    public static class Model {
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

    public static class SubModel extends Model {
        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }
    }
}

class ModelOne<T> {
    private T modelTwo;

    private T[] modelArray;

    private Map<String, T> modelMap;

    public T getModelTwo() {
        return modelTwo;
    }

    public void setModelTwo(final T modelTwo) {
        this.modelTwo = modelTwo;
    }

    public T[] getModelArray() {
        return modelArray;
    }

    public void setModelArray(final T[] modelArray) {
        this.modelArray = modelArray;
    }

    public Map<String, T> getModelMap() {
        return modelMap;
    }

    public void setModelMap(final Map<String, T> modelMap) {
        this.modelMap = modelMap;
    }
}

class ModelTwo {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}

