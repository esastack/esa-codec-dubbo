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

import java.util.HashMap;
import java.util.Map;

public interface SerializeConstants {
    byte HESSIAN2_SERIALIZATION_ID = 2;
    byte JSON_SERIALIZATION_ID = 5;
    byte FASTJSON_SERIALIZATION_ID = 6;
    byte KRYO_SERIALIZATION_ID = 8;
    byte FST_SERIALIZATION_ID = 9;
    byte PROTOSTUFF_SERIALIZATION_ID = 12;
    byte AVRO_SERIALIZATION_ID = 11;
    byte GSON_SERIALIZATION_ID = 16;
    byte PBMIX_SERIALIZATION_ID = 17;

    byte PROTOBUF_SERIALIZATION_ID = 22;
    byte PROTOBUF_SINGLE_SERIALIZATION_ID = 44;

    Map<Byte, String> seriNames = new HashMap<Byte, String>() {
        {
            put(HESSIAN2_SERIALIZATION_ID, "hessian2");
            put(JSON_SERIALIZATION_ID, "json");
            put(FASTJSON_SERIALIZATION_ID, "fastjson");
            put(KRYO_SERIALIZATION_ID, "kryo");
            put(FST_SERIALIZATION_ID, "fst");
            put(PROTOSTUFF_SERIALIZATION_ID, "protostuff");
            put(AVRO_SERIALIZATION_ID, "avro");
            put(GSON_SERIALIZATION_ID, "gson");
            put(PBMIX_SERIALIZATION_ID, "pbmix");
            put(PROTOBUF_SERIALIZATION_ID, "protobuf");
            put(PROTOBUF_SINGLE_SERIALIZATION_ID, "proto");
        }
    };

    Map<Byte, String> seriTypes = new HashMap<Byte, String>() {
        {
            put(HESSIAN2_SERIALIZATION_ID, "x-application/hessian2");
            put(JSON_SERIALIZATION_ID, "x-application/json");
            put(FASTJSON_SERIALIZATION_ID, "x-application/fastjson");
            put(KRYO_SERIALIZATION_ID, "x-application/kryo");
            put(FST_SERIALIZATION_ID, "x-application/fst");
            put(PROTOSTUFF_SERIALIZATION_ID, "x-application/protostuff");
            put(AVRO_SERIALIZATION_ID, "x-application/avro");
            put(GSON_SERIALIZATION_ID, "x-application/gson");
            put(PBMIX_SERIALIZATION_ID, "x-application/pbmix");
            put(PROTOBUF_SERIALIZATION_ID, "x-application/protobuf");
            put(PROTOBUF_SINGLE_SERIALIZATION_ID, "x-application/proto");
        }
    };
}
