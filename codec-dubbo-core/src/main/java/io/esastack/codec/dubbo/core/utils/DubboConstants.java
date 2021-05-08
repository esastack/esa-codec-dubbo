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

import io.netty.util.AttributeKey;

import java.security.cert.Certificate;

/**
 * SDK常量
 */
public class DubboConstants {

    public static final AttributeKey<Long> DECODE_TTFB_KEY =
            AttributeKey.newInstance("DECODE_TTFB");

    public static final AttributeKey<Long> DECODE_TTFB_COMPLETE_KEY =
            AttributeKey.newInstance("DECODE_TTFB_COMPLETE");

    public static final AttributeKey<Certificate> TLS_PEER_CERTIFICATE_KEY =
            AttributeKey.newInstance("TLS_PEER_CERTIFICATE");

    /**
     * dubbo protocol version
     */
    public static final String DUBBO_VERSION = "2.0.2";
    public static final int SERIALIZATION_MASK = 0x1f;
    /**
     * header length.
     */
    public static final int HEADER_LENGTH = 16;
    /**
     * bytes of magic
     */
    public static final int MAGIC_LENGTH = 2;
    /**
     * magic header.
     */
    public static final short MAGIC = (short) 0xdabb;
    public static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
    public static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];

    public static final class TRACE {

        public static final String TTFB_KEY = "TTFB";

        public static final String TTFB_COMPLETE_KEY = "TTFB_COMPLETE";

        public static final String TIME_OF_REQ_FLUSH_KEY = "TIME_OF_REQ_FLUSH";

        public static final String TIME_OF_RSP_DESERIALIZE_BEGIN_KEY = "TIME_OF_RSP_DESERIALIZE_BEGIN";

        public static final String TIME_OF_RSP_DESERIALIZE_COST_KEY = "TIME_OF_RSP_DESERIALIZE_COST";

    }

    @SuppressWarnings("TypeName")
    public static final class PARAMETER_KEY {
        public static final String PATH_KEY = "path";
        public static final String VERSION_KEY = "version";
        public static final String INTERFACE_KEY = "interface";
        public static final String DUBBO_VERSION_KEY = "dubbo";
        public static final String METHODS_KEY = "methods";
        public static final String METHOD_KEY = "method";
        public static final String METHOD_PARAMETER_DESC_KEY = "DESC_OF_PARAMETER_OF_METHOD";
        public static final String PID_KEY = "pid";
        public static final String TIMESTAMP_KEY = "timestamp";
        public static final String GROUP_KEY = "group";
    }

    @SuppressWarnings("TypeName")
    public static final class GENERIC_INVOKE {

        public static final String PARAM_TYPES_DESE = "Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;";

        public static final String $INVOKE = "$invoke";

        public static final String $INVOKE_ASYNC = "$invokeAsync";

        public static final Class<?> RETURN_TYPE = Object.class;

        public static final Class<?>[] PARAM_TYPES = new Class<?>[]{String.class, String[].class, Object[].class};

    }

    @SuppressWarnings("TypeName")
    public static final class HEADER_FLAG {
        /**
         * request flag
         */
        public static final byte FLAG_REQUEST = (byte) 0x80;

        /**
         * two way flag
         */
        public static final byte FLAG_TWOWAY = (byte) 0x40;

        /**
         * heartbeat flag
         */
        public static final byte FLAG_HEARTBEAT = (byte) 0x20;
    }

    @SuppressWarnings("TypeName")
    public static final class RESPONSE_STATUS {
        /**
         * ok.
         */
        public static final byte OK = 20;

        /**
         * client side timeout.
         */
        public static final byte CLIENT_TIMEOUT = 30;

        /**
         * server side timeout.
         */
        public static final byte SERVER_TIMEOUT = 31;

        /**
         * channel inactive, directly return the unfinished requests.
         */
        public static final byte CHANNEL_INACTIVE = 35;

        /**
         * request format error.
         */
        public static final byte BAD_REQUEST = 40;

        /**
         * response format error.
         */
        public static final byte BAD_RESPONSE = 50;

        /**
         * service not found.
         */
        public static final byte SERVICE_NOT_FOUND = 60;

        /**
         * service error.
         */
        public static final byte SERVICE_ERROR = 70;

        /**
         * internal server error.
         */
        public static final byte SERVER_ERROR = 80;

        /**
         * internal server error.
         */
        public static final byte CLIENT_ERROR = 90;

        /**
         * server side threadpool exhausted and quick return.
         */
        public static final byte SERVER_THREADPOOL_EXHAUSTED_ERROR = 100;
    }
}
