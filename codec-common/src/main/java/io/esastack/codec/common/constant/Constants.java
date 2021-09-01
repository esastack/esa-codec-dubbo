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
package io.esastack.codec.common.constant;

import io.netty.util.AttributeKey;

public class Constants {

    public static final AttributeKey<Long> DECODE_TTFB_KEY =
            AttributeKey.newInstance("DECODE_TTFB");

    public static final AttributeKey<Long> DECODE_TTFB_COMPLETE_KEY =
            AttributeKey.newInstance("DECODE_TTFB_COMPLETE");

    @SuppressWarnings("TypeName")
    public static class CHANNEL_ATTR_KEY {
        public static final AttributeKey<String> CONNECTION_NAME = AttributeKey.valueOf("CONNECTION_NAME");
    }

    public static final class TRACE {

        public static final String TTFB_KEY = "TTFB";

        public static final String TTFB_COMPLETE_KEY = "TTFB_COMPLETE";

        public static final String TIME_OF_REQ_FLUSH_KEY = "TIME_OF_REQ_FLUSH";

        public static final String TIME_OF_RSP_DESERIALIZE_BEGIN_KEY = "TIME_OF_RSP_DESERIALIZE_BEGIN";

        public static final String TIME_OF_RSP_DESERIALIZE_COST_KEY = "TIME_OF_RSP_DESERIALIZE_COST";

    }
}
