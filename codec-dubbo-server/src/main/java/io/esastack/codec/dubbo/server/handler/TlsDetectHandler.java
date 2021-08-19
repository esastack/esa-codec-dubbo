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
import io.esastack.codec.common.server.NettyServerConfig;
import io.esastack.codec.common.utils.NettyUtils;
import io.esastack.codec.dubbo.core.codec.DubboMessageDecoder;
import io.esastack.codec.dubbo.core.codec.DubboMessageEncoder;
import io.esastack.codec.dubbo.core.codec.TTFBLengthFieldBasedFrameDecoder;
import io.esastack.codec.dubbo.server.DubboServerBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.SSLEngine;
import java.util.List;

/**
 * TLS detect handler
 */
public class TlsDetectHandler extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsDetectHandler.class);

    private final SslContext sslContext;
    private final DubboServerBuilder builder;
    private final NettyServerConfig serverConfig;

    public TlsDetectHandler(DubboServerBuilder builder) {
        this(builder, null);
    }

    public TlsDetectHandler(DubboServerBuilder builder, SslContext sslContext) {
        this.sslContext = sslContext;
        this.builder = builder;
        this.serverConfig = builder.getServerConfig();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received connection from client({})", NettyUtils.parseRemoteAddress(ctx.channel()));
            ctx.channel().closeFuture().addListener(future -> {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Disconnected from client({})", NettyUtils.parseRemoteAddress(ctx.channel()));
                }
            });
        }
        super.channelActive(ctx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 使用前5字节检测协议
        if (in.readableBytes() < 5) {
            return;
        }

        if (isSSL(in)) {
            addSSLHandler(ctx);
        }

        addDubboHandler(ctx);

        //移除协议探测Handler
        ctx.pipeline().remove(this);

    }

    /**
     * 是否支持SSL协议
     */
    private boolean isSSL(ByteBuf in) {
        return sslContext != null && SslHandler.isEncrypted(in);
    }

    /**
     * 添加SSL支持
     *
     * @param ctx ctx
     */
    private void addSSLHandler(final ChannelHandlerContext ctx) {
        SSLEngine engine = sslContext.newEngine(ctx.alloc());
        String[] enabledProtocols = serverConfig.getSslContextBuilder().getEnabledProtocols();
        if (enabledProtocols != null && enabledProtocols.length > 0) {
            engine.setEnabledProtocols(enabledProtocols);
        }

        SslHandler sslHandler = new SslHandler(engine);

        //设置握手超时时间
        sslHandler.setHandshakeTimeoutMillis(serverConfig.getSslContextBuilder().getHandshakeTimeoutMillis());

        ctx.pipeline().addLast(sslHandler);
    }

    private void addDubboHandler(final ChannelHandlerContext ctx) {
        DubboServerNettyHandler dubboServerNettyHandler = new DubboServerNettyHandler(builder.getBizHandler());
        ctx.pipeline()
                .addLast("IdleStateHandler", new IdleStateHandler(0, 0, serverConfig.getHeartbeatTimeoutSeconds()))
                .addLast(new TTFBLengthFieldBasedFrameDecoder(serverConfig.getPayload(), 12, 4, 0, 0))
                .addLast(new DubboMessageDecoder())
                .addLast(new DubboMessageEncoder())
                .addLast(dubboServerNettyHandler);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.warn("Exception occurred in connection: remote[" +
                NettyUtils.parseRemoteAddress(ctx.channel()) + "]", cause);
        ctx.channel().close();
    }
}
