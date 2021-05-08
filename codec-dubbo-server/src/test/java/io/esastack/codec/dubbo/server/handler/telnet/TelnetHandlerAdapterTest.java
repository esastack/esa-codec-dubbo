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
package io.esastack.codec.dubbo.server.handler.telnet;

import io.esastack.codec.dubbo.server.handler.telnet.handler.ExceptionHandler;
import io.esastack.codec.dubbo.server.handler.telnet.handler.IdleEventHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.string.StringDecoder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TelnetHandlerAdapterTest {

    @Test
    public void channelRead() {
        final TelnetHandlerAdapter adapter = new TelnetHandlerAdapter();
        StringDecoder stringDecoder = new StringDecoder();
        EmbeddedChannel channel = new EmbeddedChannel(stringDecoder, adapter);
        assertFalse(channel.writeInbound(Unpooled.EMPTY_BUFFER));
        assertFalse(channel.writeInbound(Unpooled.wrappedBuffer("echo test".getBytes())));
        //TODO
        //assertEquals("test\r\ndubbo>", ((ByteBuf) channel.readOutbound()).toString(StandardCharsets.UTF_8));
        assertFalse(channel.writeInbound(Unpooled.wrappedBuffer("test test".getBytes())));

        //TODO
        //assertEquals("Unsupported command: test\r\ndubbo>",
        //        ((ByteBuf) channel.readOutbound()).toString(StandardCharsets.UTF_8));
    }

    @Test
    public void testUserEventTriggered() {
        final TelnetHandlerAdapter adapter = new TelnetHandlerAdapter();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new StringDecoder(), new IdleEventHandler(), adapter);
        assertFalse(embeddedChannel.writeInbound(Unpooled.wrappedBuffer("test".getBytes())));
    }

    @Test
    public void testExceptionCaught() {
        final TelnetHandlerAdapter adapter = new TelnetHandlerAdapter();
        final ExceptionHandler exceptionHandler = new ExceptionHandler();
        final EmbeddedChannel embeddedChannel = new EmbeddedChannel(exceptionHandler, adapter);
        try {
            embeddedChannel.writeInbound(Unpooled.EMPTY_BUFFER);
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }
    }
}
