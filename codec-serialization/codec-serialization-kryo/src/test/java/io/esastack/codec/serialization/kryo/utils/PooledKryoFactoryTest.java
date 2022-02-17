/*
 * Copyright 2022 OPPO ESA Stack Project
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
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.Test;

import javax.net.ssl.SSLEngine;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PooledKryoFactoryTest {

    @Test
    public void test() {
        try {
            KryoUtils.register(String.class);
        } catch (Exception ignore) {
        }
        PooledKryoFactory pooledKryoFactory = new PooledKryoFactory();
        pooledKryoFactory.registerClass(String.class);
        Kryo kryo = pooledKryoFactory.getKryo();
        pooledKryoFactory.returnKryo(kryo);
        pooledKryoFactory.setRegistrationRequired(false);
        assertThrows(IllegalStateException.class, () -> pooledKryoFactory.registerClass(String.class));
        pooledKryoFactory.create();

        PrototypeKryoFactory prototypeKryoFactory = new PrototypeKryoFactory();
        Kryo kryo1 = prototypeKryoFactory.getKryo();
        prototypeKryoFactory.returnKryo(kryo1);
        ThreadLocalKryoFactory threadLocalKryoFactory = new ThreadLocalKryoFactory();
        threadLocalKryoFactory.returnKryo(kryo1);

        KryoUtils.setRegistrationRequired(false);
        Kryo kryo2 = KryoUtils.get();
        KryoUtils.release(kryo2);
        Thread.currentThread().setContextClassLoader(null);
        assertThrows(IllegalArgumentException.class, () -> KryoUtils.setDefaultSerializer("none"));
        assertThrows(IllegalArgumentException.class, () -> KryoUtils.setDefaultSerializer("java.lang.String"));
        KryoUtils.setDefaultSerializer(DemoSerializer.class.getName());

        assertTrue(ReflectionUtils.isJdk(SSLEngine.class));
    }

    static class DemoSerializer extends Serializer {

        @Override
        public void write(Kryo kryo, Output output, Object object) {

        }

        @Override
        public Object read(Kryo kryo, Input input, Class type) {
            return null;
        }
    }
}
