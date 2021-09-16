package io.esastack.codec.serialization.kryo.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PooledKryoFactoryTest {
    @Test
    public void test() {
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
        assertThrows(IllegalArgumentException.class, () -> KryoUtils.setDefaultSerializer("none"));
        assertThrows(IllegalArgumentException.class, () -> KryoUtils.setDefaultSerializer("java.lang.String"));
        KryoUtils.setDefaultSerializer(DemoSerializer.class.getName());
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
