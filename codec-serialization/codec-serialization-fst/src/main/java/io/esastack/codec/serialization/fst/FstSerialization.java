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
package io.esastack.codec.serialization.fst;

import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static io.esastack.codec.serialization.api.SerializeConstants.FST_SERIALIZATION_ID;

public class FstSerialization implements Serialization {

    @Override
    public byte getSeriTypeId() {
        return FST_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "x-application/fst";
    }

    @Override
    public String getSeriName() {
        return "fst";
    }

    @Override
    public DataOutputStream serialize(OutputStream out) throws IOException {
        return new FstDataOutputStream(out);
    }

    @Override
    public DataInputStream deserialize(InputStream is) throws IOException {
        return new FstDataInputStream(is);
    }
}
