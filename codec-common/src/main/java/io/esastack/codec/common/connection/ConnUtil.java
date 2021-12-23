package io.esastack.codec.common.connection;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class ConnUtil {

    private static final AttributeKey<NettyConnection> CONN_KEY = AttributeKey.newInstance("CONN_KEY");

    public static void setConnectionAttr(final Channel channel, final NettyConnection connection) {
        channel.attr(CONN_KEY).set(connection);
    }

    public static void markAsActive(final Channel channel) {
        final NettyConnection connection = channel.attr(CONN_KEY).get();
        if (connection == null) {
            throw new IllegalStateException("NettyConnection should be set as an attribute of Channel!");
        }
        connection.handleConnectActive();
    }

}
