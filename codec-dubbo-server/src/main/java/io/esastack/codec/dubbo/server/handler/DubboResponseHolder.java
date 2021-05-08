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
package io.esastack.codec.dubbo.server.handler;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 因为请求是异步
 * 将ChannelHandlerContext进行传递
 */
public class DubboResponseHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboResponseHolder.class);

    private static final AttributeKey<String> REMOTE_ADDRESS_ATTR = AttributeKey.newInstance("REMOTE_ADDRESS_ATTR");

    private static final AttributeKey<String> LOCAL_ADDRESS_ATTR = AttributeKey.newInstance("LOCAL_ADDRESS_ATTR");

    private final ChannelHandlerContext ctx;

    private final SocketAddress remoteAddress;

    private final SocketAddress localAddress;

    private final String remoteAddressString;

    private final String localAddressString;

    public DubboResponseHolder(ChannelHandlerContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("ChannelHandlerContext cannot be null");
        }
        this.ctx = ctx;
        this.remoteAddress = ctx.channel().remoteAddress();
        this.localAddress = ctx.channel().localAddress();
        this.remoteAddressString = getAddressString(ctx, REMOTE_ADDRESS_ATTR, remoteAddress);
        this.localAddressString = getAddressString(ctx, LOCAL_ADDRESS_ATTR, localAddress);
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return ctx;
    }

    public SocketAddress getRemoteSocketAddress() {
        return remoteAddress;
    }

    public SocketAddress getLocalSocketAddress() {
        return localAddress;
    }

    public String getRemoteAddressString() {
        return remoteAddressString;
    }

    public String getLocalAddressString() {
        return localAddressString;
    }

    public ChannelFuture end(DubboMessage response) {
        ChannelFuture future = ctx.writeAndFlush(response);
        if (future.isDone()) {
            notifyWrite(future);
        } else {
            future.addListener((GenericFutureListener<ChannelFuture>) this::notifyWrite);
        }
        return future;
    }

    private String getAddressString(final ChannelHandlerContext ctx,
                                    final AttributeKey<String> attributeKey,
                                    final SocketAddress address) {
        String addr = ctx.channel().attr(attributeKey).get();
        if (addr == null) {
            if (address == null) {
                return null;
            } else if (address instanceof InetSocketAddress) {
                addr = ((InetSocketAddress) address).getHostString() + ":" + ((InetSocketAddress) address).getPort();
            } else {
                addr = address.toString();
            }
            ctx.channel().attr(attributeKey).set(addr);
        }
        return addr;
    }

    private void notifyWrite(ChannelFuture future) {
        if (!future.isSuccess()) {
            InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
            if (address != null) {
                String ip = address.getAddress().getHostAddress();
                int port = address.getPort();
                LOGGER.warn(" write to dubbo client[" + ip + ":" + port + "] error", future.cause());
            }
        }
    }
}
