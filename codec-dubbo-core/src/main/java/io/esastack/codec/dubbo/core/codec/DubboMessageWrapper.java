package io.esastack.codec.dubbo.core.codec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DubboMessageWrapper {
    private final DubboMessage message;
    private final Map<String, String> attachment = new HashMap<>(16);

    public DubboMessageWrapper(final DubboMessage message) {
        this.message = message;
    }

    public DubboMessage getMessage() {
        return message;
    }

    public Map<String, String> getAttachment() {
        return Collections.unmodifiableMap(attachment);
    }

    public void addAttachment(final String key, final String value) {
        this.attachment.put(key, value);
    }

    public void addAllAttachment(final Map<String, String> map) {
        this.attachment.putAll(map);
    }

    public String getAttachment(final String key) {
        return this.attachment.get(key);
    }
}
