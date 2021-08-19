/*
 * Copyright 1999-2011 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
package io.esastack.codec.dubbo.server.handler.telnet;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TelnetDecodeHandler extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(TelnetDecodeHandler.class);

    /**
     * Command history
     */
    private static final AttributeKey<LinkedList<String>> HISTORY_LIST_KEY =
            AttributeKey.newInstance("telnet.history.list");

    private static final AttributeKey<Integer> HISTORY_INDEX_KEY = AttributeKey.newInstance("telnet.history.index");

    /**
     * Up and down(Not currently supported)
     */
    private static final byte[] UP = new byte[]{27, 91, 65};

    private static final byte[] DOWN = new byte[]{27, 91, 66};

    /**
     * Backspace command
     */
    private static final byte BACKSPACE_BYTE = '\b';
    private static final byte[] BACKSPACE = new byte[]{32, 8};
    private static final byte[] BACKSPACE_DOUBLE = new byte[]{32, 32, 8, 8};

    /**
     * Enter commands for different systems
     */
    private static final List<?> ENTER = Arrays.asList(
            /* Windows Enter */
            new byte[]{'\r', '\n'},
            /* Linux Enter */
            new byte[]{'\n'});

    /**
     * exit commands for different systems
     */
    private static final List<?> EXIT = Arrays.asList(
            /* Windows Ctrl+C */
            new byte[]{3},
            /* Linux Ctrl+C */
            new byte[]{-1, -12, -1, -3, 6},
            /* Linux Pause */
            new byte[]{-1, -19, -1, -3, 6});

    private static boolean isEquals(byte[] message, byte[] command) {
        return message.length == command.length && endsWith(message, command);
    }

    private static boolean endsWith(byte[] message, byte[] command) {
        if (message.length < command.length) {
            return false;
        }
        int offset = message.length - command.length;
        for (int i = command.length - 1; i >= 0; i--) {
            if (message[offset + i] != command[i]) {
                return false;
            }
        }
        return true;
    }

    private static void processUpAddDown(ChannelHandlerContext ctx, boolean up) {
        LinkedList<String> history = ctx.channel().attr(HISTORY_LIST_KEY).get();
        if (history == null || history.size() == 0) {
            return;
        }
        Integer index = ctx.channel().attr(HISTORY_INDEX_KEY).get();
        Integer old = index;
        if (index == null) {
            index = history.size() - 1;
        } else {
            if (up) {
                index = index - 1;
                if (index < 0) {
                    index = history.size() - 1;
                }
            } else {
                index = index + 1;
                if (index > history.size() - 1) {
                    index = 0;
                }
            }
        }
        if (old == null || !old.equals(index)) {
            ctx.channel().attr(HISTORY_INDEX_KEY).set(index);
            String value = history.get(index);
            if (old != null && old >= 0 && old < history.size()) {
                String ov = history.get(old);
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < ov.length(); i++) {
                    buf.append("\b");
                }
                for (int i = 0; i < ov.length(); i++) {
                    buf.append(" ");
                }
                for (int i = 0; i < ov.length(); i++) {
                    buf.append("\b");
                }
                value = buf.toString() + value;
            }
            ctx.writeAndFlush(Unpooled.wrappedBuffer(value.getBytes()));
        }
    }

    private static String toString(byte[] message) throws UnsupportedEncodingException {
        byte[] copy = new byte[message.length];
        int index = 0;
        for (int i = 0; i < message.length; i++) {
            byte b = message[i];
            // backspace
            if (b == '\b') {
                if (index > 0) {
                    index--;
                }
                // double byte char
                if (i > 2 && message[i - 2] < 0) {
                    if (index > 0) {
                        index--;
                    }
                }
            } else if (b == 27) {
                // escape
                if (i < message.length - 4 && message[i + 4] == 126) {
                    i = i + 4;
                } else if (i < message.length - 3 && message[i + 3] == 126) {
                    i = i + 3;
                } else if (i < message.length - 2) {
                    i = i + 2;
                }
            } else if (b == -1 && i < message.length - 2
                    && (message[i + 1] == -3 || message[i + 1] == -5)) {
                // handshake
                i = i + 2;
            } else {
                copy[index++] = message[i];
            }
        }
        if (index == 0) {
            return "";
        }
        return new String(copy, 0, index, StandardCharsets.UTF_8.name()).trim();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readerIndex = in.readerIndex();
        String msg = decode(ctx, in);
        if (msg != null) {
            out.add(msg);
        } else {
            if (ctx.channel().isActive()) {
                in.readerIndex(readerIndex);
            }
        }
    }

    private String decode(ChannelHandlerContext ctx, ByteBuf in) {
        int readableBytes = in.readableBytes();
        if (readableBytes == 0) {
            return null;
        }
        byte[] message = new byte[readableBytes];
        in.readBytes(message);
        if (message[readableBytes - 1] == BACKSPACE_BYTE) {
            boolean doubleChar = message.length >= 3 && message[message.length - 3] < 0;
            ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(
                    (new String(doubleChar ? BACKSPACE_DOUBLE : BACKSPACE, StandardCharsets.UTF_8)).getBytes()));
            return null;
        }
        for (Object command : EXIT) {
            if (isEquals(message, (byte[]) command)) {
                if (ctx.channel().isActive()) {
                    logger.info("Close channel on exit command: " + Arrays.toString((byte[]) command));
                    ctx.channel().close();
                }
                return null;
            }
        }
        boolean up = endsWith(message, UP);
        boolean down = endsWith(message, DOWN);
        if (up || down) {
            processUpAddDown(ctx, up);
            return null;
        }
        byte[] enter = null;
        for (Object command : ENTER) {
            if (endsWith(message, (byte[]) command)) {
                enter = (byte[]) command;
                break;
            }
        }
        if (enter == null) {
            return null;
        }
        try {
            LinkedList<String> history = ctx.channel().attr(HISTORY_LIST_KEY).get();
            Integer index = ctx.channel().attr(HISTORY_INDEX_KEY).get();
            ctx.channel().attr(HISTORY_INDEX_KEY).set(null);
            if (history != null && history.size() > 0 && index != null && index >= 0 && index < history.size()) {
                String value = history.get(index);
                if (value != null) {
                    byte[] b1 = value.getBytes();
                    byte[] b2 = new byte[b1.length + message.length];
                    System.arraycopy(b1, 0, b2, 0, b1.length);
                    System.arraycopy(message, 0, b2, b1.length, message.length);
                    message = b2;
                }
            }
            String result = toString(message);
            if (result.trim().length() > 0) {
                if (history == null) {
                    history = new LinkedList<>();
                    ctx.channel().attr(HISTORY_LIST_KEY).set(history);
                }
                if (history.isEmpty()) {
                    history.addLast(result);
                } else if (!result.equals(history.getLast())) {
                    history.remove(result);
                    history.addLast(result);
                    if (history.size() > 10) {
                        history.removeFirst();
                    }
                }
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("UnsupportedEncodingException" + e);
            }
            return null;
        }
    }
}
