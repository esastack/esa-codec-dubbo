/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2021 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.esastack.codec.dubbo.core.codec;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

import java.io.Serializable;

public class DubboMessage implements Serializable, ReferenceCounted {
    private static final long serialVersionUID = 3184025272681915293L;
    private DubboHeader header;

    /**
     * 需要传递的二进制数据
     */
    private ByteBuf body;

    public ByteBuf getBody() {
        return body;
    }

    public DubboMessage setBody(ByteBuf body) {
        this.body = body;
        return this;
    }

    public DubboHeader getHeader() {
        return header;
    }

    public DubboMessage setHeader(DubboHeader header) {
        this.header = header;
        return this;
    }

    @Override
    public int refCnt() {
        if (body == null) {
            return 0;
        }
        return body.refCnt();
    }

    @Override
    public DubboMessage retain() {
        if (body != null) {
            body.retain();
        }

        return this;
    }

    @Override
    public DubboMessage retain(int increment) {
        if (body != null) {
            body.retain(increment);
        }

        return this;
    }

    @Override
    public DubboMessage touch() {
        if (body != null) {
            body.touch();
        }

        return this;
    }

    @Override
    public DubboMessage touch(Object hint) {
        if (body != null) {
            body.touch(hint);
        }

        return this;
    }

    @Override
    public boolean release() {
        if (body == null) {
            return true;
        }

        return body.release();
    }

    @Override
    public boolean release(int decrement) {
        if (body == null) {
            return true;
        }

        return body.release(decrement);
    }
}
