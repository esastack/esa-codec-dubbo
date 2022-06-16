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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReflectUtils {

    /**
     * void(V).
     */
    public static final char JVM_VOID = 'V';

    /**
     * boolean(Z).
     */
    public static final char JVM_BOOLEAN = 'Z';

    /**
     * byte(B).
     */
    public static final char JVM_BYTE = 'B';

    /**
     * char(C).
     */
    public static final char JVM_CHAR = 'C';

    /**
     * double(D).
     */
    public static final char JVM_DOUBLE = 'D';

    /**
     * float(F).
     */
    public static final char JVM_FLOAT = 'F';

    /**
     * int(I).
     */
    public static final char JVM_INT = 'I';

    /**
     * long(J).
     */
    public static final char JVM_LONG = 'J';

    /**
     * short(S).
     */
    public static final char JVM_SHORT = 'S';

    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    public static final String JAVA_IDENT_REGEX = "(?:[_$a-zA-Z][_$a-zA-Z0-9]*)";

    public static final String CLASS_DESC = "(?:L" + JAVA_IDENT_REGEX + "(?:\\/" + JAVA_IDENT_REGEX + ")*;)";

    public static final String ARRAY_DESC = "(?:\\[+(?:(?:[VZBCDFIJS])|" + CLASS_DESC + "))";

    public static final String DESC_REGEX = "(?:(?:[VZBCDFIJS])|" + CLASS_DESC + "|" + ARRAY_DESC + ")";

    public static final Pattern DESC_PATTERN = Pattern.compile(DESC_REGEX);

    private static final ConcurrentMap<Class<?>, String> CLASS_DESC_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Method, String> PARAMETER_DESC_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentMap<ClassLoader, ConcurrentMap<String, Class<?>>> DESC_CLASS_CACHE =
            new ConcurrentHashMap<>();

    private static final ConcurrentMap<ClassLoader, ConcurrentMap<String, Class<?>[]>> PARAMETER_TYPE_CACHE =
            new ConcurrentHashMap<>();

    private ReflectUtils() {
    }

    /**
     * <p>get class desc.</p>
     * <p>boolean[].class:"[Z"</p>
     * <p>Object.class:"Ljava/lang/Object;"</p>
     *
     * @param c class.
     * @return desc.
     */
    public static String getDesc(Class<?> c) {

        Class<?> originClass = c;

        if (CLASS_DESC_CACHE.containsKey(c)) {
            return CLASS_DESC_CACHE.get(c);
        }

        StringBuilder ret = new StringBuilder();

        while (c.isArray()) {
            ret.append('[');
            c = c.getComponentType();
        }

        if (c.isPrimitive()) {
            String t = c.getName();
            switch (t) {
                case "void":
                    ret.append(JVM_VOID);
                    break;
                case "boolean":
                    ret.append(JVM_BOOLEAN);
                    break;
                case "byte":
                    ret.append(JVM_BYTE);
                    break;
                case "char":
                    ret.append(JVM_CHAR);
                    break;
                case "double":
                    ret.append(JVM_DOUBLE);
                    break;
                case "float":
                    ret.append(JVM_FLOAT);
                    break;
                case "int":
                    ret.append(JVM_INT);
                    break;
                case "long":
                    ret.append(JVM_LONG);
                    break;
                case "short":
                    ret.append(JVM_SHORT);
                    break;
                default:
                    throw new RuntimeException("Invalid primitive type: " + t);
            }
        } else {
            ret.append('L');
            ret.append(c.getName().replace('.', '/'));
            ret.append(';');
        }
        String desc = ret.toString();
        CLASS_DESC_CACHE.putIfAbsent(originClass, desc);
        return desc;
    }

    /**
     * <p>get class array desc.</p>
     * <p>[int.class, boolean[].class, Object.class]:"I[ZLjava/lang/Object;"</p>
     *
     * @param cs class array.
     * @return desc.
     */
    public static String getDesc(final Class<?>[] cs) {
        if (cs.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(64);
        for (Class<?> c : cs) {
            sb.append(getDesc(c));
        }
        return sb.toString();
    }

    /**
     * name to desc, if the name is already a desc, just return it.
     * "boolean" : "Z"
     * "java.lang.String" : "Ljava/lang/String;"
     * "Ljava.lang.String" : "Ljava/lang/String;"
     * "Ljava/lang/String" : "Ljava/lang/String;"
     * "java.util.Map[][]" : "[[Ljava/util/Map;"
     * "[[Ljava.util.Map;" : "[[Ljava/util/Map;"
     * "[[Ljava/util/Map;" : "[[Ljava/util/Map;"
     *
     * @param name name.
     * @return Class desc.
     */
    public static String name2Desc(String name) {
        switch (name.charAt(0)) {
            case JVM_VOID:
            case JVM_BOOLEAN:
            case JVM_BYTE:
            case JVM_CHAR:
            case JVM_DOUBLE:
            case JVM_FLOAT:
            case JVM_INT:
            case JVM_LONG:
            case JVM_SHORT:
                return name;
            case 'L':
                //"Ljava.lang.String" : "Ljava/lang/String;"
                //"Ljava/lang/String" : "Ljava/lang/String;"
            case '[':
                //"[[Ljava.util.Map;" : "[[Ljava/util/Map;"
                //"[[Ljava/util/Map;" : "[[Ljava/util/Map;"
                return name.replace(".", "/");
            default:
                break;
        }
        int c = 0, index = name.indexOf('[');
        if (index > 0) {
            c = (name.length() - index) / 2;
            name = name.substring(0, index);
        }
        StringBuilder sb = new StringBuilder();
        while (c-- > 0) {
            sb.append("[");
        }
        switch (name) {
            case "void":
                sb.append(JVM_VOID);
                break;
            case "boolean":
                sb.append(JVM_BOOLEAN);
                break;
            case "byte":
                sb.append(JVM_BYTE);
                break;
            case "char":
                sb.append(JVM_CHAR);
                break;
            case "double":
                sb.append(JVM_DOUBLE);
                break;
            case "float":
                sb.append(JVM_FLOAT);
                break;
            case "int":
                sb.append(JVM_INT);
                break;
            case "long":
                sb.append(JVM_LONG);
                break;
            case "short":
                sb.append(JVM_SHORT);
                break;
            default:
                // "java.lang.Object" =: "Ljava.lang.Object;"
                sb.append('L').append(name.replace(".", "/")).append(';');
                break;
        }
        return sb.toString();
    }

    /**
     * desc to name.
     * "I" =: "int"
     * "[[I" : "int[][]"
     * "int[][]" : "int[][]"
     * "int" : "int"
     * "java.lang.String" : "java.lang.String"
     *
     * @param desc desc.
     * @return name.
     */
    public static String desc2name(String desc) {
        if (desc.endsWith("[]")) {
            return desc;
        }
        StringBuilder sb = new StringBuilder();
        int c = desc.lastIndexOf('[') + 1;
        if (desc.length() == c + 1) {
            switch (desc.charAt(c)) {
                case JVM_VOID: {
                    sb.append("void");
                    break;
                }
                case JVM_BOOLEAN: {
                    sb.append("boolean");
                    break;
                }
                case JVM_BYTE: {
                    sb.append("byte");
                    break;
                }
                case JVM_CHAR: {
                    sb.append("char");
                    break;
                }
                case JVM_DOUBLE: {
                    sb.append("double");
                    break;
                }
                case JVM_FLOAT: {
                    sb.append("float");
                    break;
                }
                case JVM_INT: {
                    sb.append("int");
                    break;
                }
                case JVM_LONG: {
                    sb.append("long");
                    break;
                }
                case JVM_SHORT: {
                    sb.append("short");
                    break;
                }
                default:
                    throw new RuntimeException();
            }
        } else if (desc.startsWith("L") || desc.startsWith("[")) {
            sb.append(desc.substring(c + 1, desc.length() - 1).replace('/', '.'));
        } else {
            //like java.lang.String
            sb.append(desc);
        }
        while (c-- > 0) {
            sb.append("[]");
        }
        return sb.toString();
    }

    public static Class<?>[] name2Class(String[] names) throws ClassNotFoundException {
        if (names == null || names.length == 0) {
            return new Class<?>[0];
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        Class<?>[] parameterTypes = new Class<?>[names.length];
        for (int i = 0; i < names.length; i++) {
            parameterTypes[i] = name2Class(names[i], classLoader);
        }
        return parameterTypes;
    }

    /**
     * name to class.
     * "boolean" : boolean.class
     * "java.util.Map[][]" : java.util.Map[][].class
     *
     * @param name name.
     * @return Class instance.
     */
    public static Class<?> name2Class(String name) throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return name2Class(name, classLoader);
    }

    private static Class<?> name2Class(String name, ClassLoader classLoader) throws ClassNotFoundException {
        name = desc2name(name);
        int c = 0, index = name.indexOf('[');
        if (index > 0) {
            c = (name.length() - index) / 2;
            name = name.substring(0, index);
        }
        if (c > 0) {
            StringBuilder sb = new StringBuilder();
            while (c-- > 0) {
                sb.append("[");
            }

            switch (name) {
                case "void":
                    sb.append(JVM_VOID);
                    break;
                case "boolean":
                    sb.append(JVM_BOOLEAN);
                    break;
                case "byte":
                    sb.append(JVM_BYTE);
                    break;
                case "char":
                    sb.append(JVM_CHAR);
                    break;
                case "double":
                    sb.append(JVM_DOUBLE);
                    break;
                case "float":
                    sb.append(JVM_FLOAT);
                    break;
                case "int":
                    sb.append(JVM_INT);
                    break;
                case "long":
                    sb.append(JVM_LONG);
                    break;
                case "short":
                    sb.append(JVM_SHORT);
                    break;
                default:
                    // "java.lang.Object" : "Ljava.lang.Object;"
                    sb.append('L').append(name).append(';');
                    break;
            }
            name = sb.toString();
        } else {
            if ("void".equals(name)) {
                return void.class;
            }
            if ("boolean".equals(name)) {
                return boolean.class;
            }
            if ("byte".equals(name)) {
                return byte.class;
            }
            if ("char".equals(name)) {
                return char.class;
            }
            if ("double".equals(name)) {
                return double.class;
            }
            if ("float".equals(name)) {
                return float.class;
            }
            if ("int".equals(name)) {
                return int.class;
            }
            if ("long".equals(name)) {
                return long.class;
            }
            if ("short".equals(name)) {
                return short.class;
            }
        }

        if (!DESC_CLASS_CACHE.containsKey(classLoader)) {
            DESC_CLASS_CACHE.computeIfAbsent(classLoader, loader -> new ConcurrentHashMap<>());
        }
        ConcurrentMap<String, Class<?>> cacheMap = DESC_CLASS_CACHE.get(classLoader);
        Class<?> clazz = cacheMap.get(name);
        if (clazz == null) {
            clazz = Class.forName(name, true, classLoader);
            cacheMap.put(name, clazz);
        }
        return clazz;
    }

    /**
     * desc to class.
     * "[Z" : boolean[].class
     * "[[Ljava/util/Map;" : java.util.Map[][].class
     *
     * @param desc desc.
     * @return Class instance.
     */
    private static Class<?> desc2class(String desc, ClassLoader classLoader) throws ClassNotFoundException {
        switch (desc.charAt(0)) {
            case JVM_VOID:
                return void.class;
            case JVM_BOOLEAN:
                return boolean.class;
            case JVM_BYTE:
                return byte.class;
            case JVM_CHAR:
                return char.class;
            case JVM_DOUBLE:
                return double.class;
            case JVM_FLOAT:
                return float.class;
            case JVM_INT:
                return int.class;
            case JVM_LONG:
                return long.class;
            case JVM_SHORT:
                return short.class;
            case 'L':
                // "Ljava/lang/Object;" : "java.lang.Object"
                desc = desc.substring(1, desc.length() - 1).replace('/', '.');
                break;
            case '[':
                // "[[Ljava/lang/Object;" : "[[Ljava.lang.Object;"
                desc = desc.replace('/', '.');
                break;
            default:
                throw new ClassNotFoundException("Class not found: " + desc);
        }

        if (!DESC_CLASS_CACHE.containsKey(classLoader)) {
            DESC_CLASS_CACHE.computeIfAbsent(classLoader, loader -> new ConcurrentHashMap<>());
        }
        ConcurrentMap<String, Class<?>> cacheMap = DESC_CLASS_CACHE.get(classLoader);
        Class<?> clazz = cacheMap.get(desc);
        if (clazz == null) {
            clazz = Class.forName(desc, true, classLoader);
            cacheMap.put(desc, clazz);
        }
        return clazz;
    }

    public static Class<?>[] desc2classArray(String desc) throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return desc2classArray(desc, classLoader);
    }

    /**
     * get class array instance.
     *
     * @param desc desc.
     * @return Class[] class array.
     */
    public static Class<?>[] desc2classArray(String desc, ClassLoader classLoader) throws ClassNotFoundException {
        if (desc.length() == 0) {
            return EMPTY_CLASS_ARRAY;
        }

        if (!PARAMETER_TYPE_CACHE.containsKey(classLoader)) {
            PARAMETER_TYPE_CACHE.computeIfAbsent(classLoader, loader -> new ConcurrentHashMap<>());
        }
        ConcurrentMap<String, Class<?>[]> cacheMap = PARAMETER_TYPE_CACHE.get(classLoader);
        Class<?>[] parameterTypes = cacheMap.get(desc);
        if (parameterTypes == null) {
            List<Class<?>> cs = new ArrayList<>();
            Matcher m = DESC_PATTERN.matcher(desc);
            while (m.find()) {
                cs.add(desc2class(m.group(), classLoader));
            }
            parameterTypes = cs.toArray(EMPTY_CLASS_ARRAY);
            cacheMap.put(desc, parameterTypes);
        }

        return parameterTypes;
    }
}
