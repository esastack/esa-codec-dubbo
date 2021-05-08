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

import io.esastack.codec.dubbo.core.exception.SerializationException;
import io.esastack.codec.dubbo.core.exception.UnknownProtocolException;
import io.esastack.codec.serialization.api.DataInputStream;
import io.esastack.codec.serialization.api.Serialization;
import io.esastack.codec.serialization.api.SerializeFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNull;

public class NettyUtilsTest {

    @Test
    public void testNullValue() throws Exception {
        ByteBuf byteBuf = NettyUtils.nullValue((byte) 2);

        Serialization serialization = SerializeFactory.getSerialization((byte) 2);
        ByteBufInputStream inputStream = new ByteBufInputStream(byteBuf);
        DataInputStream dataInputStream = serialization.deserialize(inputStream);
        Object object = dataInputStream.readObject(null);

        assertNull(object);
    }

    @Test
    public void testChannelAttr() {
        String key = "key";
        String value = "value";

        EmbeddedChannel channel = new EmbeddedChannel();
        NettyUtils.setChannelAttr(channel, key, value);
        assertNull(NettyUtils.getChannelAttr(channel, null));
        Assert.assertEquals(value, NettyUtils.getChannelAttr(channel, key));

        EmbeddedChannel channel2 = new EmbeddedChannel();
        NettyUtils.setChannelAttr(channel2, AttributeKey.valueOf(key), value);
        Assert.assertEquals(value, NettyUtils.getChannelAttr(channel2, key));
    }

    @Test
    public void testChannelAddr() {
        EmbeddedChannel channel = new EmbeddedChannel();
        Assert.assertEquals("embedded", NettyUtils.parseRemoteAddress(channel));
    }

    @Test
    public void testTtfb() {
        EmbeddedChannel channel = new EmbeddedChannel();

        Attribute<Long> ttfbAttr = channel.attr(DubboConstants.DECODE_TTFB_KEY);
        ttfbAttr.set(System.currentTimeMillis());

        Attribute<Long> ttfbCompleteAttr = channel.attr(DubboConstants.DECODE_TTFB_COMPLETE_KEY);
        ttfbCompleteAttr.set(System.currentTimeMillis() + 10);

        Map<String, String> attachments = new HashMap<>();
        NettyUtils.setAttachmentsTtfbKey(attachments, channel);

        final Map<String, String> ttfbAttachments = NettyUtils.extractTtfbKey(channel);

        Assert.assertEquals(String.valueOf(ttfbAttr.get()), ttfbAttachments.get(DubboConstants.TRACE.TTFB_KEY));
        Assert.assertEquals(String.valueOf(ttfbCompleteAttr.get()),
                ttfbAttachments.get(DubboConstants.TRACE.TTFB_COMPLETE_KEY));

    }

    @Test
    public void testException() {
        Assert.assertFalse(NettyUtils.isUnknownProtocolException(null));
        Assert.assertTrue(NettyUtils.isUnknownProtocolException(new UnknownProtocolException("test")));
        Assert.assertTrue(NettyUtils.isUnknownProtocolException(new DecoderException(
                new UnknownProtocolException(new SerializationException("test"))
        )));
        Assert.assertTrue(NettyUtils.isUnknownProtocolException(new DecoderException(
                new UnknownProtocolException("test", new NullPointerException())
        )));
    }

}
