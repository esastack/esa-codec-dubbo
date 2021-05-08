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
package unix.domain.socket.demo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Ignore;

@Ignore
public class UdsClient {
    public static void main(String[] args) throws Exception {
        String path = "/data/uds/auds.sock";
        if (args != null && args.length > 0) {
            path = args[0];
        } else {
            System.out.println("no abstract namespace path,use default!");
        }
        path = "\0" + path;
        new UdsClient().start(path);
    }

    public void start(String path) throws Exception {
        final UdsClientHandler clientHandler = new UdsClientHandler();
        EventLoopGroup group = new EpollEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            //@formatter:off
            b.group(group)
                    .channel(EpollDomainSocketChannel.class)
                    .handler(new ChannelInitializer<DomainSocketChannel>() {
                        @Override
                        protected void initChannel(DomainSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(clientHandler);
                        }
                    });
            //@formatter:on
            ChannelFuture f = b.connect(new DomainSocketAddress(path)).sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    @Sharable
    private class UdsClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("client channel active!");
            // ctx.writeAndFlush(Unpooled.copiedBuffer("Netty uds!", CharsetUtil.UTF_8));
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            System.out.println("Client received: " + msg.toString(CharsetUtil.UTF_8));
            ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("Error occur when reading from Unix domain socket: " + cause.getMessage());
            cause.printStackTrace();
            ctx.close();
        }
    }
}
