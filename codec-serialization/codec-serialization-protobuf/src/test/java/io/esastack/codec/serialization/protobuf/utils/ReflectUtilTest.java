package io.esastack.codec.serialization.protobuf.utils;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReflectUtilTest {
    @Test
    public void test() {
        assertThrows(RuntimeException.class, () -> ReflectUtil.forName("none"));
        assertThrows(RuntimeException.class, () -> ReflectUtil.getMethod(Inner.class, "none"));
        assertNotNull(ReflectUtil.getMethod(Inner.class, "echo"));
        assertNotNull(ReflectUtil.getMethod(Inner.class, "echoString", String.class, int.class));
    }

    static class Inner {
        public void echo() {
        }

        public String echoString(String name, int age) {
            return name + age;
        }
    }
}
