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
package io.esastack.codec.dubbo.core;

import io.esastack.codec.dubbo.core.codec.DubboHeader;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Collections;

import static io.esastack.codec.serialization.api.SerializeConstants.FST_SERIALIZATION_ID;
import static org.junit.Assert.*;

public class DubboRequestMetaDataHelperTest {

    @Test
    public void testReadRequestMetaData() throws Exception {
        // 构造dubboMessage
        DubboHeader header = new DubboHeader();
        header.setSeriType((byte) 0);
        DubboMessage message = new DubboMessage();
        message.setHeader(header);
        message.setBody(null);

        message.setBody(new byte[16]);

        assertNull(DubboRequestMetaDataHelper.readRequestMetaData(message));

        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setOneWay(false);
        rpcInvocation.setInterfaceName("DemoService");
        rpcInvocation.setSeriType(FST_SERIALIZATION_ID);
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setRequestId(1);
        rpcInvocation.setArguments(new Object[]{"test"});
        rpcInvocation.setAttachments(Collections.singletonMap("test", "test"));
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setVersion("2.0.2");
        rpcInvocation.setReturnType(String.class);
        message = ClientCodecHelper.toDubboMessage(rpcInvocation);
        DubboRequestMetaData dubboRequestMetaData = DubboRequestMetaDataHelper.readRequestMetaData(message);
        assert dubboRequestMetaData != null;
        assertEquals(message.getHeader().getSeriType(), dubboRequestMetaData.getSeriType());
    }

}
