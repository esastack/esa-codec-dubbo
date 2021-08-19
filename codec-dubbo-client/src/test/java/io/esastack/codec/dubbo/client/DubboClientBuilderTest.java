package io.esastack.codec.dubbo.client;

import io.esastack.codec.common.connection.NettyConnectionConfig;
import org.junit.Test;

import static org.junit.Assert.*;

public class DubboClientBuilderTest {
    @Test
    public void test() {

        NettyConnectionConfig connectionConfig = new NettyConnectionConfig()
                .setWriteBufferHighWaterMark(10)
                .setConnectTimeout(1000)
                .setPayload(8 * 1024 * 1024)
                .setUnixDomainSocketFile("demo")
                .setUseNativeTransports(true)
                .setHeartbeatTimeoutSeconds(60)
                .setDefaultRequestTimeout(1000)
                .setChannelOptions(null)
                .setChannelHandlers(null)
                .setConnectionInitializer(null)
                .setSslContextBuilder(null)
                .setTlsFallback2Normal(true)
                .setMultiplexPoolBuilder(NettyConnectionConfig.MultiplexPoolBuilder.newBuilder());


        assertEquals(10, connectionConfig.getWriteBufferHighWaterMark());
        assertEquals(1000, connectionConfig.getConnectTimeout());
        assertEquals(8 * 1024 * 1024, connectionConfig.getPayload());
        assertEquals("demo", connectionConfig.getUnixDomainSocketFile());
        assertTrue(connectionConfig.isUseNativeTransports());
        assertEquals(60, connectionConfig.getHeartbeatTimeoutSeconds());
        assertEquals(1000, connectionConfig.getDefaultRequestTimeout());
        assertNull(connectionConfig.getChannelOptions());
        assertNull(connectionConfig.getChannelHandlers());
        assertNull(connectionConfig.getConnectionInitializer());
        assertNull(connectionConfig.getSslContextBuilder());
        assertTrue(connectionConfig.isTlsFallback2Normal());

        DubboClientBuilder builder = new DubboClientBuilder()
                .setWriteTimeout(1000)
                .setReadTimeout(1000)
                .setConnectionConfig(connectionConfig);
        assertEquals(1000, builder.getWriteTimeout());
        assertEquals(1000, builder.getReadTimeout());

        NettyDubboClient client = builder.build();
    }

}
