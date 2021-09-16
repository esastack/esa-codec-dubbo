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

import com.google.protobuf.*;
import esa.commons.serialize.protobuf.wrapper.MapValue;
import esa.commons.serialize.protobuf.wrapper.ThrowableValue;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.protobuf.utils.ProtobufUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SingleProtobufDataInputStream implements DataInputStream {
    private static final ConcurrentHashMap<Class<?>, Message> instCache = new ConcurrentHashMap<>();
    private static final ExtensionRegistryLite globalRegistry =
            ExtensionRegistryLite.getEmptyRegistry();
    private final ConcurrentMap<Class<?>, SingleMessageMarshaller<?>> marshallers = new ConcurrentHashMap<>();
    private final InputStream is;

    public static Message defaultInst(Class<?> clz) {
        Message defaultInst = instCache.get(clz);
        if (defaultInst != null) {
            return defaultInst;
        }
        try {
            defaultInst = (Message) clz.getMethod("getDefaultInstance").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Create default protobuf instance failed ", e);
        }
        instCache.put(clz, defaultInst);
        return defaultInst;
    }

    public SingleProtobufDataInputStream(InputStream is) {
        this.is = is;
    }

    @Override
    public int readInt() throws IOException {
        return read(Int32Value.class).getValue();
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) read(Int32Value.class).getValue();
    }

    @Override
    public byte[] readBytes() throws IOException {
        return read(BytesValue.class).getValue().toByteArray();
    }

    @Override
    public String readUTF() throws IOException {
        return read(StringValue.class).getValue();
    }

    @Override
    public <T> T readObject(final Class<T> cls) throws IOException, ClassNotFoundException {
        return read(cls);
    }

    @Override
    public Throwable readThrowable() throws IOException, ClassNotFoundException {
        final ThrowableValue.Throwable throwableValue = ProtobufUtil.parseFrom(ThrowableValue.Throwable.class, is);
        return new ProtobufWrappedException(throwableValue);
    }

    @Override
    public Map readMap() throws IOException, ClassNotFoundException {
        return ProtobufUtil.parseFrom(MapValue.Map.class, is).getAttachmentsMap();
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    private <T> T read(Class<T> cls) throws IOException {
        if (ProtobufUtil.isNotSupport(cls)) {
            throw new IllegalArgumentException("This serialization only support google protobuf messages, " +
                    "but the actual input type is :" + cls.getName());
        }
        return (T) getMarshaller(cls).parse(is);
    }

    private SingleMessageMarshaller<?> getMarshaller(Class<?> clz) {
        return marshallers.computeIfAbsent(clz, k -> new SingleMessageMarshaller(k));
    }

    public static final class SingleMessageMarshaller<T extends MessageLite> {
        private final Parser<T> parser;

        SingleMessageMarshaller(Class<T> clz) {
            final T inst = (T) defaultInst(clz);
            this.parser = (Parser<T>) inst.getParserForType();
        }

        public T parse(InputStream stream) throws InvalidProtocolBufferException {
            return parser.parseFrom(stream, globalRegistry);
        }
    }
}
