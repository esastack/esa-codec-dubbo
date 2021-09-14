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
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JsonTest {

    @Test
    public void test() throws Exception {
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
        ArrayList list1 = mapper1.readValue(json, ArrayList.class);
        assertEquals(1, list1.size());
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

