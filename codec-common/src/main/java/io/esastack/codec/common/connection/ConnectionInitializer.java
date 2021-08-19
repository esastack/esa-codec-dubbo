package io.esastack.codec.common.connection;

import io.esastack.codec.common.ResponseCallback;
import io.netty.channel.Channel;

import java.util.Map;

public interface ConnectionInitializer {

    void initialize(final Channel channel, final String connectionName, final Map<Long, ResponseCallback> callbackMap);
}
