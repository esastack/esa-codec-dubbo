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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class TelnetTelnetDetectHandlerTest {

    private static final byte[] UP = new byte[]{27, 91, 65};

    private static final byte[] DOWN = new byte[]{27, 91, 66};

    private static final byte[] BACKSPACE = new byte[]{32, 8};
    private static final byte[] BACKSPACE_DOUBLE = new byte[]{32, 32, 8, 8};

    @Test
    public void decode() {
        TelnetDecodeHandler telnetDecodeHandler = new TelnetDecodeHandler();
        EmbeddedChannel embeddedChannelRead = new EmbeddedChannel(telnetDecodeHandler);
        // 写入上下
        assertFalse(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(UP)));
        assertFalse(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(DOWN)));
        // 写入null
        assertFalse(embeddedChannelRead.writeInbound(Unpooled.EMPTY_BUFFER));
        // 写入删除键
        assertFalse(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("lsl\b".getBytes())));
        Object outboundMessage = embeddedChannelRead.readOutbound();
        assertNotNull(outboundMessage);
        byte[] msg = new byte[2];
        assertEquals(2, ((ByteBuf) outboundMessage).readableBytes());
        ((ByteBuf) outboundMessage).readBytes(msg);
        assertArrayEquals(new byte[] {32, 8}, msg);
        // 写入正常指令
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(" -l\r\n".getBytes())));
        assertEquals("ls -l", embeddedChannelRead.readInbound());
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("help\r\n".getBytes())));
        assertEquals("help", embeddedChannelRead.readInbound());
        // 写入上和下
        assertFalse(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(UP)));
        assertEquals("help", ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertFalse(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(UP)));
        assertEquals("\b\b\b\b    \b\b\b\bls -l",
                ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertFalse(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(DOWN)));
        assertEquals("\b\b\b\b\b     \b\b\b\b\bhelp",
                ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(new byte[]{'\r', '\n'})));
        //写入退出
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(new byte[]{3})));
        assertFalse(embeddedChannelRead.isActive());
    }

    @Test
    public void testUpAndDown() {
        TelnetDecodeHandler telnetDecodeHandler = new TelnetDecodeHandler();
        EmbeddedChannel embeddedChannelRead = new EmbeddedChannel(telnetDecodeHandler);
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("ls\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("help\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(UP)));
        assertEquals("help", ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(UP)));
        assertEquals("\b\b\b\b    \b\b\b\bls",
                ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(UP)));
        assertEquals("\b\b  \b\bhelp", ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(DOWN)));
        assertEquals("\b\b\b\b    \b\b\b\bls",
                ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(DOWN)));
        assertEquals("\b\b  \b\bhelp",
                ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(DOWN)));
        assertEquals("\b\b\b\b    \b\b\b\bls",
                ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("1\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("2\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("3\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("4\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("5\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("6\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("7\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("8\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer("9\r\n".getBytes())));
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(UP)));
        embeddedChannelRead.readOutbound();
        assertTrue(embeddedChannelRead.writeInbound(Unpooled.wrappedBuffer(DOWN)));
        assertEquals("\b \bls", ((ByteBuf) embeddedChannelRead.readOutbound()).toString(StandardCharsets.UTF_8));
    }
}
