package io.esastack.codec.dubbo.client;

import io.esastack.codec.dubbo.core.codec.DubboMessageWrapper;

public interface DubboResponseInBizCallback extends DubboResponseCallback {
    void onBizResponse(DubboMessageWrapper messageWrapper);
}
