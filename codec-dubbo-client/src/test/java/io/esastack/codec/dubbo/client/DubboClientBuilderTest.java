package io.esastack.codec.dubbo.client;

import org.junit.Test;

import static org.junit.Assert.*;

public class DubboClientBuilderTest {
    @Test
    public void test() {
        DubboClientBuilder builder = new DubboClientBuilder()
                .setWriteBufferHighWaterMark(10)
                .setConnectTimeout(1000)
                .setWriteTimeout(1000)
                .setPayload(8 * 1024 * 1024)
                .setUnixDomainSocketFile("demo")
                .setUseNativeTransports(true)
                .setHeartbeatTimeoutSeconds(60)
                .setReadTimeout(1000)
                .setDefaultRequestTimeout(1000)
                .setChannelOptions(null)
                .setChannelHandlers(null)
                .setHandlerSuppliers(null)
                .setDubboSslContextBuilder(null)
                .setTlsFallback2Normal(true)
                .setMultiplexPoolBuilder(DubboClientBuilder.MultiplexPoolBuilder.newBuilder());
        assertEquals(10, builder.getWriteBufferHighWaterMark());
        assertEquals(1000, builder.getConnectTimeout());
        assertEquals(1000, builder.getWriteTimeout());
        assertEquals(8 * 1024 * 1024, builder.getPayload());
        assertEquals("demo", builder.getUnixDomainSocketFile());
        assertTrue(builder.isUseNativeTransports());
        assertEquals(60, builder.getHeartbeatTimeoutSeconds());
        assertEquals(1000, builder.getReadTimeout());
        assertEquals(1000, builder.getDefaultRequestTimeout());
        assertNull(builder.getChannelOptions());
        assertNull(builder.getChannelHandlers());
        assertNull(builder.getHandlerSuppliers());
        assertNull(builder.getDubboSslContextBuilder());
        assertTrue(builder.isTlsFallback2Normal());
        NettyDubboClient client = builder.build();
    }

}
