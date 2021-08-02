package io.esastack.codec.dubbo.core.codec;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DubboMessageWrapper {
    private final DubboMessage message;
    private final ConcurrentHashMap<String, String> attachment = new ConcurrentHashMap<>(16);

    public DubboMessageWrapper(final DubboMessage message) {
        this.message = message;
    }

    public DubboMessage getMessage() {
        return message;
    }

    public Map<String, String> getAttachments() {
        return Collections.unmodifiableMap(attachment);
    }

    public void addAttachment(final String key, final String value) {
        this.attachment.put(key, value);
    }

    public void addAttachments(final Map<String, String> map) {
        this.attachment.putAll(map);
    }

    public String getAttachment(final String key) {
        return this.attachment.get(key);
    }
}
