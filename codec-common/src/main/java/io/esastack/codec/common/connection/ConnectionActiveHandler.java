package io.esastack.codec.common.connection;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class ConnectionActiveHandler extends ChannelInboundHandlerAdapter {

    public static final ConnectionActiveHandler INSTANCE = new ConnectionActiveHandler();

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        ConnUtil.markAsActive(ctx.channel());
        super.channelActive(ctx);
    }


}
