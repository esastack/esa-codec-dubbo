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

import java.util.Collection;
import java.util.Map;

/**
 * ATTENTION: if A Class is registered to the KRYO, then it's transported with a ID insteadof its name, making the
 * packet smaller; One esa.commons.serialize.api.SerializationOptimizer must be used by both consumer and provider
 * at the same time or both not, or else deserialization exception would be thrown, because the ID of a class
 * could not be found in the other side.
 * <p>
 * Interface defining serialization optimizer, there are nothing implementations for now.
 */
public interface SerializationOptimizer {

    default Collection<Class<?>> getSerializableClasses() {
        return null;
    }

    default Map<Class<?>, Object> getSerializers() {
        return null;
    }
}
