package io.esastack.codec.dubbo.core.codec;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DubboMessageWrapperTest {
    @Test
    public void test() {
        DubboMessage dubboMessage = new DubboMessage();
        DubboMessageWrapper messageWrapper = new DubboMessageWrapper(dubboMessage);
        assertEquals(dubboMessage, messageWrapper.getMessage());
        messageWrapper.addAttachment("foo", "bar");
        messageWrapper.addAttachments(Collections.singletonMap("foo1", "bar1"));
        assertEquals("bar1", messageWrapper.getAttachment("foo1"));
        assertEquals(2, messageWrapper.getAttachments().size());
    }

}
