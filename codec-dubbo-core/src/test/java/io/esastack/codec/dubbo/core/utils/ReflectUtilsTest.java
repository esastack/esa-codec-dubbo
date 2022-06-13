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
package io.esastack.codec.dubbo.core.utils;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReflectUtilsTest {

    @Test
    public void test() throws ClassNotFoundException {
        Assert.assertEquals("[Ljava/lang/String;", ReflectUtils.getDesc(String[].class));
        Assert.assertEquals("Ljava/lang/Void;", ReflectUtils.getDesc(Void.class));
        Assert.assertEquals("Z", ReflectUtils.getDesc(boolean.class));
        Assert.assertEquals("B", ReflectUtils.getDesc(byte.class));
        Assert.assertEquals("C", ReflectUtils.getDesc(char.class));
        Assert.assertEquals("D", ReflectUtils.getDesc(double.class));
        Assert.assertEquals("I", ReflectUtils.getDesc(int.class));
        Assert.assertEquals("F", ReflectUtils.getDesc(float.class));
        Assert.assertEquals("J", ReflectUtils.getDesc(long.class));
        Assert.assertEquals("S", ReflectUtils.getDesc(short.class));

        Assert.assertEquals(void.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(void.class))[0]);
        Assert.assertEquals(boolean.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(boolean.class))[0]);
        Assert.assertEquals(byte.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(byte.class))[0]);
        Assert.assertEquals(char.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(char.class))[0]);
        Assert.assertEquals(double.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(double.class))[0]);
        Assert.assertEquals(int.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(int.class))[0]);
        Assert.assertEquals(float.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(float.class))[0]);
        Assert.assertEquals(long.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(long.class))[0]);
        Assert.assertEquals(short.class, ReflectUtils.desc2classArray(ReflectUtils.getDesc(short.class))[0]);

        Assert.assertEquals("I", ReflectUtils.name2Desc("int"));
        Assert.assertEquals("I", ReflectUtils.name2Desc("I"));
        Assert.assertEquals("[I", ReflectUtils.name2Desc("int[]"));
        Assert.assertEquals("[I", ReflectUtils.name2Desc("[I"));
        Assert.assertEquals("[[I", ReflectUtils.name2Desc("int[][]"));
        Assert.assertEquals("[[I", ReflectUtils.name2Desc("[[I"));
        Assert.assertEquals("Ljava/lang/String;", ReflectUtils.name2Desc("java.lang.String"));
        Assert.assertEquals("Ljava/lang/String;", ReflectUtils.name2Desc("Ljava.lang.String;"));
        Assert.assertEquals("Ljava/lang/String;", ReflectUtils.name2Desc("Ljava/lang/String;"));
        Assert.assertEquals("[Ljava/lang/String;", ReflectUtils.name2Desc("java.lang.String[]"));
        Assert.assertEquals("[Ljava/lang/String;", ReflectUtils.name2Desc("[Ljava.lang.String;"));
        Assert.assertEquals("[Ljava/lang/String;", ReflectUtils.name2Desc("[Ljava/lang/String;"));
        Assert.assertEquals("[[Ljava/lang/String;", ReflectUtils.name2Desc("java.lang.String[][]"));
        Assert.assertEquals("[[Ljava/lang/String;", ReflectUtils.name2Desc("[[Ljava.lang.String;"));
        Assert.assertEquals("[[Ljava/lang/String;", ReflectUtils.name2Desc("[[Ljava/lang/String;"));

        Assert.assertEquals("int", ReflectUtils.desc2name("I"));
        Assert.assertEquals("int", ReflectUtils.desc2name("int"));
        Assert.assertEquals("int[]", ReflectUtils.desc2name("[I"));
        Assert.assertEquals("int[]", ReflectUtils.desc2name("int[]"));
        Assert.assertEquals("int[][]", ReflectUtils.desc2name("[[I"));
        Assert.assertEquals("int[][]", ReflectUtils.desc2name("int[][]"));
        Assert.assertEquals("java.lang.String", ReflectUtils.desc2name("Ljava/lang/String;"));
        Assert.assertEquals("java.lang.String", ReflectUtils.desc2name("Ljava.lang.String;"));
        Assert.assertEquals("java.lang.String", ReflectUtils.desc2name("java.lang.String"));
        Assert.assertEquals("java.lang.String[]", ReflectUtils.desc2name("[Ljava/lang/String;"));
        Assert.assertEquals("java.lang.String[]", ReflectUtils.desc2name("[Ljava.lang.String;"));
        Assert.assertEquals("java.lang.String[]", ReflectUtils.desc2name("java.lang.String[]"));
        Assert.assertEquals("java.lang.String[][]", ReflectUtils.desc2name("[[Ljava/lang/String;"));
        Assert.assertEquals("java.lang.String[][]", ReflectUtils.desc2name("[[Ljava.lang.String;"));
        Assert.assertEquals("java.lang.String[][]", ReflectUtils.desc2name("java.lang.String[][]"));

        Assert.assertEquals(int.class, ReflectUtils.name2Class("int"));
        Assert.assertEquals(int.class, ReflectUtils.name2Class("I"));
        Assert.assertEquals(int[].class, ReflectUtils.name2Class("int[]"));
        Assert.assertEquals(int[].class, ReflectUtils.name2Class("[I"));
        Assert.assertEquals(int[][].class, ReflectUtils.name2Class("int[][]"));
        Assert.assertEquals(int[][].class, ReflectUtils.name2Class("[[I"));
        Assert.assertEquals(String.class, ReflectUtils.name2Class("java.lang.String"));
        Assert.assertEquals(String.class, ReflectUtils.name2Class("Ljava.lang.String;"));
        Assert.assertEquals(String.class, ReflectUtils.name2Class("Ljava/lang/String;"));
        Assert.assertEquals(String[].class, ReflectUtils.name2Class("java.lang.String[]"));
        Assert.assertEquals(String[].class, ReflectUtils.name2Class("[Ljava.lang.String;"));
        Assert.assertEquals(String[].class, ReflectUtils.name2Class("[Ljava/lang/String;"));
        Assert.assertEquals(String[][].class, ReflectUtils.name2Class("java.lang.String[][]"));
        Assert.assertEquals(String[][].class, ReflectUtils.name2Class("[[Ljava.lang.String;"));
        Assert.assertEquals(String[][].class, ReflectUtils.name2Class("[[Ljava/lang/String;"));

        Thread.currentThread().setContextClassLoader(null);
        Assert.assertEquals(0, ReflectUtils.desc2classArray("").length);
        Assert.assertEquals("", ReflectUtils.getDesc(new Class[0]));
        assertEquals(0, ReflectUtils.desc2classArray("[Ljava.lang.Object;").length);
    }
}
