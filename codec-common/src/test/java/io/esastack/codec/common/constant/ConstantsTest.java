package io.esastack.codec.common.constant;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstantsTest {
    @Test
    public void testConstant() {
        assertEquals("TTFB", Constants.TRACE.TTFB_KEY);
        assertEquals("TTFB_COMPLETE", Constants.TRACE.TTFB_COMPLETE_KEY);
        assertEquals("TIME_OF_REQ_FLUSH", Constants.TRACE.TIME_OF_REQ_FLUSH_KEY);
        assertEquals("TIME_OF_RSP_DESERIALIZE_BEGIN", Constants.TRACE.TIME_OF_RSP_DESERIALIZE_BEGIN_KEY);
        assertEquals("TIME_OF_RSP_DESERIALIZE_COST", Constants.TRACE.TIME_OF_RSP_DESERIALIZE_COST_KEY);
        assertEquals("CONNECTION_NAME", Constants.CHANNEL_ATTR_KEY.CONNECTION_NAME.name());
    }
}
