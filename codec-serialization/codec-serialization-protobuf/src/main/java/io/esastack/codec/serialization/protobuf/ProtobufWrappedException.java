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
package io.esastack.codec.serialization.protobuf;

import com.google.common.base.Strings;
import esa.commons.serialize.protobuf.wrapper.ThrowableValue;

/**
 * For protobuf, all server side exceptions should be wrapped using this specific one.
 */
public class ProtobufWrappedException extends RuntimeException {

    private static final long serialVersionUID = -1792808536714102039L;

    private String originalClassName;
    private String originalMessage;

    public ProtobufWrappedException(ThrowableValue.Throwable throwableValue) {
        super(throwableValue.getOriginalClassName() + ": " + throwableValue.getOriginalMessage());

        originalClassName = throwableValue.getOriginalClassName();
        originalMessage = throwableValue.getOriginalMessage();

        if (throwableValue.getStackTraceCount() > 0) {
            setStackTrace(throwableValue.getStackTraceList().stream()
                    .map(ProtobufWrappedException::toStackTraceElement)
                    .toArray(StackTraceElement[]::new));
        }

        if (throwableValue.hasCause()) {
            initCause(new ProtobufWrappedException(throwableValue.getCause()));
        }
    }

    private static StackTraceElement toStackTraceElement(ThrowableValue.StackTraceElement proto) {
        return new StackTraceElement(
                proto.getClassName(),
                proto.getMethodName(),
                Strings.emptyToNull(proto.getFileName()),
                proto.getLineNumber());
    }

    public String getOriginalClassName() {
        return originalClassName;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

}

