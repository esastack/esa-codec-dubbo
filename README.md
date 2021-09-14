# Codec-dubbo
![Build](https://github.com/esastack/esa-codec-dubbo/workflows/Build/badge.svg?branch=main)
[![codecov](https://codecov.io/gh/esastack/esa-codec-dubbo/branch/main/graph/badge.svg?token=CCQBCBQJP6)](https://codecov.io/gh/esastack/codec-dubbo)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.esastack/codec-dubbo-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.esastack/codec-dubbo-parent/)
[![GitHub license](https://img.shields.io/github/license/esastack/esa-codec-dubbo)](https://github.com/esastack/esa-codec-dubbo/blob/main/LICENSE)

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
<!-- commons.version >= 0.1.1 --> 
<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>commons</artifactId>
    <version>${commons.version}</version>
</dependency>
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

<!--netty-->
<!-- netty.version >= 4.1.52.Final, netty-tcnative.version >= 2.0.34.Final -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
    <version>${netty.version}</version>
</dependency>
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-tcnative</artifactId>
    <version>${netty-tcnative.version}</version>
</dependency>
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-tcnative-boringssl-static</artifactId>
    <version>${netty-tcnative.version}</version>
</dependency>
```
 #### 2、Dubbo Client SDK instructions
 ```java
public class DubboSDKClient {
    public static void main(String[] args) throws Exception {

        // build client config
        final Map<ChannelOption, Object> channelOptions = new HashMap<>();
        channelOptions.put(ChannelOption.SO_KEEPALIVE, true);
        channelOptions.put(ChannelOption.TCP_NODELAY, true);
        channelOptions.put(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        NettyConnectionConfig nettyConnectionConfig = new NettyConnectionConfig();
        NettyConnectionConfig.MultiplexPoolBuilder multiplexPoolBuilder =
                NettyConnectionConfig.MultiplexPoolBuilder.newBuilder();
        nettyConnectionConfig.setMultiplexPoolBuilder(multiplexPoolBuilder)
                .setChannelOptions(channelOptions)
                .setHost("localhost")
                .setPort(20880);
        final DubboClientBuilder builder = new DubboClientBuilder().setConnectionConfig(nettyConnectionConfig);

        // build client
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
            if (t != null || r.getException() != null ||
                    (r.getErrorMessage() != null && !"".equals(r.getErrorMessage()))) {
                // handle exception
            }
            // handle return value r.getValue();
        });
    }
}
```

 #### 3、 Dubbo Server SDK instructions


```java
public class ServerDemo {

    static ExecutorService workerThreadPool = new ThreadPoolExecutor(200, 200, 60, TimeUnit.SECONDS, 
            new SynchronousQueue<>(),
            new ThreadFactory() {
                final AtomicInteger index = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("thread-" + index.getAndIncrement());
                    return thread;
                }
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    public static void main(String[] args) {

        // build server config
        final Map<ChannelOption, Object> options = new HashMap<>();
        options.put(ChannelOption.SO_BACKLOG, 128);
        options.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        final Map<ChannelOption, Object> childOptions = new HashMap<>();
        childOptions.put(ChannelOption.SO_REUSEADDR, true);
        childOptions.put(ChannelOption.SO_KEEPALIVE, true);
        childOptions.put(ChannelOption.TCP_NODELAY, true);
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setBindIp("localhost")
                .setPort(20880)
                .setIoThreads(1)
                .setBossThreads(4)
                .setChannelOptions(options)
                .setChildChannelOptions(childOptions)
                .setHeartbeatTimeoutSeconds(60);

        // build server
        DubboServerBuilder dubboServerBuilder = new DubboServerBuilder()
                .setServerConfig(nettyServerConfig)
                .setBizHandler(new DubboServerBizHandler() { // handle request and return response
                    @Override
                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                        final RpcInvocation invocation;
                        try {
                            // parse request
                            invocation = ServerCodecHelper.toRpcInvocation(request);
                        } catch (Exception e) {
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
                                        DubboRpcResult.success(
                                                invocation.getRequestId(),
                                                invocation.getSeriType(),
                                                response),
                                        request.getBody().alloc());
                            } catch (SerializationException e) {
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
