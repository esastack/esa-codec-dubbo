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
package io.esastack.codec.serialization.protobuf.utils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReflectUtil {

    private static final ConcurrentMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, Method> methodCache = new ConcurrentHashMap<>();

    public static Class<?> forName(String clzName) {
        try {
            Class<?> clazz = classCache.get(clzName);
            if (clazz == null) {
                clazz = Class.forName(clzName);
                classCache.put(clzName, clazz);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method getMethod(Class<?> clazzType, String methodName, Class... parameterTypes) {
        try {
            String methodKey = methodKey(clazzType.getName(), methodName, parameterTypes);
            Method method = methodCache.get(methodKey);
            if (method == null) {
                method = clazzType.getMethod(methodName, parameterTypes);
                methodCache.put(methodKey, method);
            }
            return method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static String methodKey(String className, String methodName, Class[] parameterTypes) {
        StringBuilder sb = new StringBuilder(className);
        sb.append(".").append(methodName).append("(");
        if (parameterTypes == null) {
            return sb.append(")").toString();
        }

        int len = parameterTypes.length;
        for (int i = 0; i < len; i++) {
            if (i == (len - 1)) {
                sb.append(parameterTypes[i].getName());
            } else {
                sb.append(parameterTypes[i].getName()).append(",");
            }
        }
        return sb.append(")").toString();
    }
}
