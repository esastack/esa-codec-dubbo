package io.esastack.codec.common.server;

import io.netty.channel.Channel;

public interface ServerConnectionInitializer {

    void initialize(final Channel channel);
}
