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
package io.esastack.codec.serialization.protobuf.utils;

import com.google.common.base.Strings;
import com.google.protobuf.*;
import esa.commons.serialize.protobuf.wrapper.MapValue;
import esa.commons.serialize.protobuf.wrapper.ThrowableValue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProtobufUtil {

    private static final String BUILDER_CLASS_SUFFIX = "$Builder";

    private static volatile ExtensionRegistryLite globalRegistry = ExtensionRegistryLite.getEmptyRegistry();

    private static ConcurrentMap<Class<? extends MessageLite>, MessageBuilder> cache = new ConcurrentHashMap<>();

    public static void init() {
        register(Empty.getDefaultInstance());
        register(BoolValue.getDefaultInstance());
        register(Int32Value.getDefaultInstance());
        register(Int64Value.getDefaultInstance());
        register(FloatValue.getDefaultInstance());
        register(DoubleValue.getDefaultInstance());
        register(BytesValue.getDefaultInstance());
        register(Duration.getDefaultInstance());
        register(Timestamp.getDefaultInstance());
        register(StringValue.getDefaultInstance());
        register(ListValue.getDefaultInstance());
        register(ThrowableValue.Throwable.getDefaultInstance());
        register(MapValue.Map.getDefaultInstance());
    }

    public static <T extends MessageLite> void register(T defaultInstance) {
        cache.put(defaultInstance.getClass(), new MessageBuilder<>(defaultInstance));
    }

    public static boolean isNotSupport(Class<?> clazz) {
        return clazz == null || !MessageLite.class.isAssignableFrom(clazz);
    }

    public static Object parseFrom(final String className, byte[] data) throws IOException {
        String copyClass = className;
        boolean isBuilderClass = copyClass.endsWith(BUILDER_CLASS_SUFFIX);
        if (isBuilderClass) {
            copyClass = substringBuilder(className);
        }

        Class<?> clazz = ReflectUtil.forName(copyClass);
        MessageBuilder messageBuilder = getMessageBuilder(clazz);
        MessageLite.Builder builder = messageBuilder.parseFrom(CodedInputStream.newInstance(data));
        return isBuilderClass ? builder : builder.buildPartial();
    }

    @SuppressWarnings("unchecked")
    public static <T> T parseFrom(Class<T> clazz, InputStream is) throws IOException {
        MessageBuilder messageBuilder = getMessageBuilder(clazz);
        return (T) messageBuilder.parseFrom(is);
    }

    @SuppressWarnings("unchecked")
    private static <T> MessageBuilder getMessageBuilder(Class<T> clazz) {
        Class<? extends MessageLite> messageLiteClazz = (Class<? extends MessageLite>) clazz;
        MessageBuilder messageBuilder = cache.get(messageLiteClazz);
        if (messageBuilder == null) {
            // create if no messageBuilder, but it's better to register in advance.
            messageBuilder = createMessageParser(messageLiteClazz);
            cache.put(messageLiteClazz, messageBuilder);
        }
        return messageBuilder;
    }

    @SuppressWarnings("unchecked")
    private static <T extends MessageLite> MessageBuilder createMessageParser(Class<T> clazz) {
        try {
            Method method = ReflectUtil.getMethod(clazz, "getDefaultInstance");
            T defaultInstance = (T) method.invoke(null, null);
            return new MessageBuilder(defaultInstance);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static String substringBuilder(String className) {
        return className.substring(0, className.length() - BUILDER_CLASS_SUFFIX.length());
    }

    public static ThrowableValue.Throwable convertToThrowableValue(Throwable throwable) {
        final ThrowableValue.Throwable.Builder builder = ThrowableValue.Throwable.newBuilder();
        builder.setOriginalClassName(throwable.getClass().getCanonicalName());
        builder.setOriginalMessage(Strings.nullToEmpty(throwable.getMessage()));

        for (StackTraceElement e : throwable.getStackTrace()) {
            builder.addStackTrace(toStackTraceElement(e));
        }

        if (throwable.getCause() != null) {
            builder.setCause(convertToThrowableValue(throwable.getCause()));
        }
        return builder.build();
    }

    private static ThrowableValue.StackTraceElement toStackTraceElement(StackTraceElement element) {
        final ThrowableValue.StackTraceElement.Builder builder =
                ThrowableValue.StackTraceElement.newBuilder()
                        .setClassName(element.getClassName())
                        .setMethodName(element.getMethodName())
                        .setLineNumber(element.getLineNumber());
        if (element.getFileName() != null) {
            builder.setFileName(element.getFileName());
        }
        return builder.build();
    }

    private static final class MessageBuilder<T extends MessageLite> {

        private final MessageLite.Builder builder;

        MessageBuilder(T defaultInstance) {
            this.builder = defaultInstance.newBuilderForType();
        }

        MessageBuilder(MessageLite.Builder builder) {
            this.builder = builder;
        }

        private Object parseFrom(InputStream inputStream) throws IOException {
            MessageLite.Builder cloneBuilder = cloneBuilder();
            cloneBuilder.mergeDelimitedFrom(inputStream, globalRegistry);
            return cloneBuilder.buildPartial();
        }

        private MessageLite.Builder parseFrom(CodedInputStream inputStream) throws IOException {
            MessageLite.Builder cloneBuilder = cloneBuilder();
            cloneBuilder.mergeFrom(inputStream);
            return cloneBuilder;
        }

        private MessageLite.Builder cloneBuilder() {
            return this.builder.getDefaultInstanceForType().newBuilderForType();
        }
    }

    public static class Type {
        public static final int TYPE_NORMAL = 0;
        public static final int TYPE_PROTOBUF = 1;
        public static final int TYPE_PROTOBUF_BUILDER = 2;
    }
}
