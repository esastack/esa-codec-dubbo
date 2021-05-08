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
package io.esastack.codec.serialization.api;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import esa.commons.spi.SpiLoader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SerializeFactory {

    private static final Logger logger = LoggerFactory.getLogger(SerializeFactory.class);

    private static final Map<Byte, Serialization> ID_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, Serialization> NAME_CACHE = new ConcurrentHashMap<>();

    static {
        try {
            List<Serialization> serializations = SpiLoader.cached(Serialization.class).getAll();
            for (Serialization serialization : serializations) {
                ID_CACHE.put(serialization.getSeriTypeId(), serialization);
                NAME_CACHE.put(serialization.getSeriName(), serialization);
                logger.info(String.format("Loaded serialization: type = %d, name = %s from SPI",
                        serialization.getSeriTypeId(), serialization.getSeriName()));
            }
        } catch (Throwable ex) {
            logger.error("Failed to load serializations from SPI", ex);
        }
    }

    public static Serialization getSerialization(byte seriType) {
        return ID_CACHE.get(seriType);
    }

    public static Serialization getSerialization(String extensionName) {
        return NAME_CACHE.get(extensionName);
    }
}
