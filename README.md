# Codec-dubbo
![Build](https://github.com/esastack/codec-dubbo/workflows/Build/badge.svg?branch=main)
[![codecov](https://codecov.io/gh/esastack/codec-dubbo/branch/main/graph/badge.svg?token=CCQBCBQJP6)](https://codecov.io/gh/esastack/codec-dubbo)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.esastack/codec-dubbo-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.esastack/codec-dubbo-parent/)
[![GitHub license](https://img.shields.io/github/license/esastack/codec-dubbo)](https://github.com/esastack/codec-dubbo/blob/main/LICENSE)

Codec-dubbo is a binary codec framework for dubbo protocol

## Features
- Fully compatible with Dubbo protocol
- Completely rewritten based on Netty, does not rely on native Dubbo
- Support only parsing metadata but not body (suitable for proxy scenarios)
- Support Dubbo Server
- Support Dubbo Client
- Multiple serialization protocols support

##  SDK instructions
#### 1、Introduce Maven dependencies
```xml   
<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>codec-dubbo-client</artifactId>
    <version>${mvn.version}</version>
</dependency>
<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>codec-dubbo-server</artifactId>
    <version>${mvn.version}</version>
</dependency>
```
 #### 2、Dubbo Client SDK instructions
 ```java
public class DubboSDKClient {

    public static void main(String[] args) throws Exception {
        // build client
        final Map<ChannelOption, Object> channelOptions = new HashMap<>();
        channelOptions.put(ChannelOption.SO_KEEPALIVE, true);
        channelOptions.put(ChannelOption.TCP_NODELAY, true);
        channelOptions.put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        final DubboClientBuilder.MultiplexPoolBuilder multiplexPoolBuilder = DubboClientBuilder.MultiplexPoolBuilder.newBuilder();
        final DubboClientBuilder builder = new DubboClientBuilder()
                .setMultiplexPoolBuilder(multiplexPoolBuilder)
                .setChannelOptions(channelOptions)
                .setHost("localhost")
                .setPort(20880);
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
        CompletableFuture<RpcResult> responseFuture = nettyDubboClient.sendRequest(request, String.class);

        responseFuture.whenComplete((r, t) -> {
            if (t != null || r.getException() != null || StringUtils.isNotEmpty(r.getErrorMessage())) {
                // handle exception
            }
            // handle return value r.getValue();
        });
    }
}
```

 #### 3、 Dubbo Server SDK instructions


```java
public class DubboSDKServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboSDKServer.class);

    static ExecutorService workerThreadPool =
            ThreadPools.builder()
                    .corePoolSize(200)
                    .maximumPoolSize(200)
                    .useSynchronousQueue()
                    .rejectPolicy((r, executor) -> LOGGER.error("rejectedExecution ")).build();

    public static void main(String[] args) {
        // build server
        DubboServerBuilder dubboServerBuilder = new DubboServerBuilder()
                .setPort(20880)
                .setBizHandler(new DubboServerBizHandler() { // handle request and return response
                    @Override
                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                        final RpcInvocation invocation;
                        try {
                            // parse request
                            invocation = ServerCodecHelper.toRpcInvocation(request);
                        } catch (Exception e) {
                            LOGGER.error("Failed to convert request to rpc invocation for {}", e);
                            dubboResponseHolder.end(null);
                            return;
                        }
                        workerThreadPool.execute(() -> {
                            String response = "requestId:" +
                                    invocation.getRequestId() +
                                    " Hello " + invocation.getArguments()[0] +
                                    ", response from provider. seriType:" +
                                    invocation.getSeriType();

                            DubboMessage dubboResponse = null;
                            try {
                                // build response
                                dubboResponse = ServerCodecHelper.toDubboMessage(
                                        RpcResult.success(
                                                invocation.getRequestId(),
                                                invocation.getSeriType(),
                                                response),
                                        request.getBody().alloc());
                            } catch (SerializationException e) {
                                LOGGER.error("Failed to serialize response for {}", e);
                                dubboResponseHolder.getChannelHandlerContext().channel().close();
                            }
                            // send response
                            dubboResponseHolder.end(dubboResponse);
                        });
                    }

                    @Override
                    public void shutdown() {

                    }
                });
        NettyDubboServer nettyDubboServer = new NettyDubboServer(dubboServerBuilder);

        // start server
        nettyDubboServer.start();
    }
}
```
