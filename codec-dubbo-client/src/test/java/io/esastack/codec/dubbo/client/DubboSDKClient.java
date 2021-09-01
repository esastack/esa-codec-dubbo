package io.esastack.codec.dubbo.client;

import esa.commons.StringUtils;
import io.esastack.codec.common.connection.NettyConnectionConfig;
import io.esastack.codec.common.connection.NettyConnectionConfig.MultiplexPoolBuilder;
import io.esastack.codec.dubbo.core.RpcInvocation;
import io.esastack.codec.dubbo.core.DubboRpcResult;
import io.esastack.codec.dubbo.core.codec.DubboMessage;
import io.esastack.codec.dubbo.core.codec.helper.ClientCodecHelper;
import io.netty.channel.ChannelOption;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DubboSDKClient {

    public static void main(String[] args) throws Exception {
        // build client
        final Map<ChannelOption, Object> channelOptions = new HashMap<>();
        channelOptions.put(ChannelOption.SO_KEEPALIVE, true);
        channelOptions.put(ChannelOption.TCP_NODELAY, true);
        channelOptions.put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        final MultiplexPoolBuilder multiplexPoolBuilder =
                MultiplexPoolBuilder.newBuilder();
        final NettyConnectionConfig connectionConfig = new NettyConnectionConfig()
                .setMultiplexPoolBuilder(multiplexPoolBuilder)
                .setChannelOptions(channelOptions)
                .setHost("localhost")
                .setPort(20880);
        final DubboClientBuilder builder = new DubboClientBuilder().setConnectionConfig(connectionConfig);
        NettyDubboClient nettyDubboClient = new NettyDubboClient(builder);

        // build request
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new String[]{"dubbo"});
        rpcInvocation.setInterfaceName("org.apache.dubbo.demo.DemoService");
        rpcInvocation.setReturnType(String.class);

        Map<String, String> attachments = new HashMap<>();
        rpcInvocation.setAttachments(attachments);

        DubboMessage request = ClientCodecHelper.toDubboMessage(rpcInvocation);

        // Send the request and handle the return value
        CompletableFuture<DubboRpcResult> responseFuture = nettyDubboClient.sendRequest(request, String.class);

        responseFuture.whenComplete((r, t) -> {
            if (t != null || r.getException() != null || StringUtils.isNotEmpty(r.getErrorMessage())) {
                // handle exception
            }
            // handle return value r.getValue();
        });
    }
}
