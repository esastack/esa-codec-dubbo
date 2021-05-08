/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
package io.esastack.codec.serialization.kryo.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * The kryo utils used by dubbo
 *
 * @since 2.6.0
 */
public class KryoUtils {
    private static AbstractKryoFactory kryoFactory = new ThreadLocalKryoFactory();

    public static void setDefaultSerializer(final String serializerClazz) {
        final Class<?> serializer;
        try {
            serializer = getClassLoader().loadClass(serializerClazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not load class " + serializerClazz, e);
        }
        if (Serializer.class.isAssignableFrom(serializer)) {
            AbstractKryoFactory.setDefaultSerializer((Class<? extends Serializer>) serializer);
        } else {
            throw new IllegalArgumentException(serializerClazz + " does not implement " + Serializer.class.getName());
        }
    }

    public static Kryo get() {
        return kryoFactory.getKryo();
    }

    public static void release(Kryo kryo) {
        kryoFactory.returnKryo(kryo);
    }

    public static void register(Class<?> clazz) {
        kryoFactory.registerClass(clazz);
    }

    public static void setRegistrationRequired(boolean registrationRequired) {
        kryoFactory.setRegistrationRequired(registrationRequired);
    }

    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return classLoader;
    }
}
