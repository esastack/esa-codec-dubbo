package io.esastack.codec.serialization.constant;

import java.io.Serializable;
import java.util.Objects;

/**
 * TestConstant
 *
 * @author guconglin
 * @date 2021/9/14 11:29
 */
public class TestConstant {
    public static final byte WRITE_BYTE = 'A';
    public static final byte[] WRITE_BYTES = new byte[]{'A'};
    public static final int WRITE_INT = 1;
    public static final User WRITE_OBJECT = new User("foo", 18);
    public static final String WRITE_UTF = "bar";

    public static class User implements Serializable {
        private static final long serialVersionUID = 2735504896803362160L;
        private String name;
        private int age;

        public User() {
        }

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            User user = (User) o;
            return age == user.age &&
                    name.equals(user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    public static class SubUser extends User {
        private static final long serialVersionUID = -2543880864408490811L;
    }
}
