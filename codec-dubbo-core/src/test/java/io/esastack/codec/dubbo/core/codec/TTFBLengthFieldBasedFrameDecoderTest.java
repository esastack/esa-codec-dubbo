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
package io.esastack.codec.dubbo.core.codec;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TTFBLengthFieldBasedFrameDecoderTest {

    @Test
    public void testDecode() {
        TTFBLengthFieldBasedFrameDecoder ttfbLengthFieldBasedFrameDecoder =
                new TTFBLengthFieldBasedFrameDecoder(8 * 1024 * 1024, 12, 4, 0, 0);
        EmbeddedChannel channel = new EmbeddedChannel(ttfbLengthFieldBasedFrameDecoder);

        try {
            channel.writeInbound(Unpooled.wrappedBuffer("1".getBytes()));
        } catch (Exception e) {
            assertEquals(DecoderException.class, e.getClass());
        }
    }

}
