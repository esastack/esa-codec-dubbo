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

/**
 * 工具类 用于处理Byte字节
 */
public class Bytes {

    /**
     * to byte array.
     *
     * @param v value.
     * @return byte[].
     */
    public static byte[] short2bytes(short v) {
        byte[] ret = {0, 0};
        short2bytes(v, ret);
        return ret;
    }

    /**
     * to byte array.
     *
     * @param v value.
     * @param b byte array.
     */
    public static void short2bytes(short v, byte[] b) {
        short2bytes(v, b, 0);
    }

    /**
     * to byte array.
     *
     * @param v value.
     * @param b byte array.
     */
    public static void short2bytes(short v, byte[] b, int off) {
        b[off + 1] = (byte) v;
        b[off + 0] = (byte) (v >>> 8);
    }
}
