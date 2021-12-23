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
package io.esastack.codec.common.utils;

import esa.commons.NetworkUtils;
import esa.commons.StringUtils;
import esa.commons.io.IOUtils;
import io.esastack.codec.common.constant.Constants;
import io.esastack.codec.common.exception.SerializationException;
import io.esastack.codec.common.exception.UnknownProtocolException;
import io.esastack.codec.serialization.api.DataOutputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.esastack.codec.serialization.api.SerializeFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettyUtils {

    private static final ConcurrentHashMap<Byte, ByteBuf> NULL_VALUE_CACHE = new ConcurrentHashMap<>();

    public static ByteBuf nullValue(byte seriType) throws Exception {
        ByteBuf nullValue = NULL_VALUE_CACHE.get(seriType);
        if (nullValue == null) {
            ByteBufOutputStream byteBufOutputStream = null;
            DataOutputStream out = null;
            try {
                Serialization serialization = SerializeFactory.getSerialization(seriType);
                if (serialization == null) {
                    String msg = "Unsupported serialization type, id=" + seriType + ", name=" +
                            SerializeConstants.seriNames.get(seriType) +
                            ", maybe it not included in the classpath, please check your (maven/gradle) dependencies!";
                    throw new SerializationException(msg);
                }
                byteBufOutputStream = new ByteBufOutputStream(Unpooled.buffer());
                out = serialization.serialize(byteBufOutputStream);
                out.writeObject(null);
                out.flush();

                nullValue = byteBufOutputStream.buffer();

                ByteBuf tmpByteBuf = NULL_VALUE_CACHE.putIfAbsent(seriType, nullValue);

                if (tmpByteBuf != null) {
                    nullValue.release();
                    nullValue = tmpByteBuf;
                }
            } finally {
                IOUtils.closeQuietly(byteBufOutputStream);
                IOUtils.closeQuietly(out);
            }
        }

        return nullValue.retainedSlice();
    }

    /**
     * 设置Channel属性
     *
     * @deprecated use {@link #setChannelAttr(Channel, AttributeKey, Object)}
     */
    @Deprecated
    public static void setChannelAttr(Channel channel, String attr, Object value) {
        if (attr == null) {
            return;
        }

        AttributeKey<Object> attributeKey = AttributeKey.valueOf(attr);
        Attribute<Object> attribute = channel.attr(attributeKey);
        if (attribute != null) {
            attribute.set(value);
        }
    }

    public static <T> void setChannelAttr(Channel channel, AttributeKey<T> attr, T value) {
        if (attr == null) {
            return;
        }

        Attribute<T> attribute = channel.attr(attr);
        if (attribute != null) {
            attribute.set(value);
        }
    }

    public static <T> T getChannelAttr(Channel channel, String attr) {
        if (attr == null) {
            return null;
        }

        AttributeKey<T> attributeKey = AttributeKey.valueOf(attr);
        Attribute<T> attribute = channel.attr(attributeKey);
        if (attribute == null) {
            return null;
        }

        return attribute.get();
    }

    public static String parseRemoteAddress(Channel channel) {
        if (channel == null) {
            return StringUtils.empty();
        }
        final SocketAddress remote = channel.remoteAddress();
        return NetworkUtils.parseAddress(remote);
    }

    /**
     * set ttfb key to attachments from channel attr see TTFBLengthFieldBasedFrameDecoder
     *
     * @param attachments attachments
     * @param channel     channel may with ttfb key
     */
    public static void setAttachmentsTtfbKey(Map<String, String> attachments, Channel channel) {
        if (attachments == null || channel == null) {
            return;
        }
        //构建首字节收包事件产生时间,TTFBLengthFieldBasedFrameDecoder 用于调用链上报监控
        //Trace过滤器 ConsumerTraceFilter ProviderTraceFilter
        Attribute<Long> decodeTtfbTime = channel.attr(Constants.DECODE_TTFB_KEY);
        if (decodeTtfbTime.get() != null) {
            attachments.put(Constants.TRACE.TTFB_KEY, String.valueOf(decodeTtfbTime.get()));
        }
        //Dubbo协议收包完成
        Attribute<Long> decodeTtfbCompleteTime = channel.attr(Constants.DECODE_TTFB_COMPLETE_KEY);
        if (decodeTtfbCompleteTime.get() != null) {
            attachments.put(Constants.TRACE.TTFB_COMPLETE_KEY, String.valueOf(decodeTtfbCompleteTime.get()));
        }
    }

    /**
     * extract ttfb key to attachments from channel attr see TTFBLengthFieldBasedFrameDecoder
     */
    public static Map<String, String> extractTtfbKey(final Channel channel) {
        final Map<String, String> attachments = new HashMap<>();
        //构建首字节收包事件产生时间,TTFBLengthFieldBasedFrameDecoder 用于调用链上报监控
        //Trace过滤器 ConsumerTraceFilter ProviderTraceFilter
        final Attribute<Long> decodeTtfbTime = channel.attr(Constants.DECODE_TTFB_KEY);
        if (decodeTtfbTime.get() != null) {
            attachments.put(Constants.TRACE.TTFB_KEY, String.valueOf(decodeTtfbTime.get()));
        }
        //Dubbo协议收包完成
        final Attribute<Long> decodeTtfbCompleteTime = channel.attr(Constants.DECODE_TTFB_COMPLETE_KEY);
        if (decodeTtfbCompleteTime.get() != null) {
            attachments.put(Constants.TRACE.TTFB_COMPLETE_KEY, String.valueOf(decodeTtfbCompleteTime.get()));
        }
        return attachments;
    }

    public static boolean isUnknownProtocolException(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof UnknownProtocolException) {
            return true;
        }
        return t instanceof DecoderException
                && t.getCause() != null
                && t.getCause() instanceof UnknownProtocolException;
    }
}
