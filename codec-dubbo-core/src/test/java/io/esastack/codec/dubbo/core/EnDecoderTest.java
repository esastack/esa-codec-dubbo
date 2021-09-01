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

import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.DubboMessageDecoder;
import io.esastack.codec.dubbo.core.codec.DubboMessageEncoder;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.esastack.codec.serialization.api.SerializeConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EnDecoderTest {

    @Test
    public void encodeTest() throws Exception {
        RpcInvocation rpcInvocation = getRpcInvocation();
        DubboMessage requestMessage = ClientCodecHelper.toDubboMessage(rpcInvocation);

        EmbeddedChannel channel = new EmbeddedChannel(new DubboMessageEncoder());
        channel.writeOneOutbound(requestMessage);
        channel.flushOutbound();
        ByteBuf byteBuf = channel.readOutbound();
        Assert.assertNotNull(byteBuf);

        EmbeddedChannel decoderChannel = new EmbeddedChannel(new DubboMessageDecoder());

        Assert.assertTrue(decoderChannel.writeInbound(byteBuf));
        DubboMessage dubboMessage = decoderChannel.readInbound();
        Assert.assertNotNull(dubboMessage);
        Assert.assertEquals(requestMessage.getHeader().getRequestId(), dubboMessage.getHeader().getRequestId());
    }

    private RpcInvocation getRpcInvocation() {
        Map<String, String> attachments = new HashMap<>();
        attachments.put("tt", System.currentTimeMillis() + "");
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setRequestId(100L);
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new String[]{"dubbo"});
        rpcInvocation.setInterfaceName("org.apache.dubbo.demo.DemoService");
        rpcInvocation.setReturnType(String.class);
        rpcInvocation.setAttachments(attachments);
        rpcInvocation.setSeriType(SerializeConstants.HESSIAN2_SERIALIZATION_ID);
        rpcInvocation.setVersion("0.0.0");
        return rpcInvocation;
    }
}
