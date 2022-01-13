# Codec-dubbo
![Build](https://github.com/esastack/esa-codec-dubbo/workflows/Build/badge.svg?branch=main)
[![codecov](https://codecov.io/gh/esastack/esa-codec-dubbo/branch/main/graph/badge.svg?token=CCQBCBQJP6)](https://codecov.io/gh/esastack/codec-dubbo)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.esastack/codec-dubbo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.esastack/codec-dubbo/)
[![GitHub license](https://img.shields.io/github/license/esastack/esa-codec-dubbo)](https://github.com/esastack/esa-codec-dubbo/blob/main/LICENSE)

Codec-dubbo is a binary codec framework for dubbo protocol

## Features
- Fully compatible with Dubbo protocol
- Completely rewritten based on Netty, does not rely on native Dubbo
- Support only parsing metadata but not body (suitable for proxy scenarios)
- Support Dubbo Server
- Support Dubbo Client
- Multiple serialization protocols support

##  Quick Start
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
 #### 2、Quick Start for Dubbo Client
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
        // set serialization type
        // rpcInvocation.setSeriType(KRYO_SERIALIZATION_ID);

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

 #### 3、 Quick Start for Dubbo Server
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
## Serialization
#### 1、 Supported serialization
The default serialization method is **hessian2**, and the following serialization methods are also supported:
- fastjson
- fst
- json(jackson)
- kryo        
- protobuf    
- protostuff 

#### 2、 Examples of supported serialization usage(take kryo as an example)
1. Introduce Maven dependencies
```xml
<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>codec-serialization-kryo</artifactId>
    <version>${mvn.version}</version>
</dependency>
```

2. Set serialization method
Before we send a request, we need to set serialization type of this request as follows:
```java
rpcInvocation.setSeriType(KRYO_SERIALIZATION_ID);
```
We can get the numbers corresponding to all serialization methods from the **SerializeConstants** interface
#### 3、 Custom serialization
If the existing serialization method cannot meet the needs, you can customize the serialization method through SPI.
1. Implement serialization class
```java
public class TestSerialization implements Serialization {

    @Override
    public byte getSeriTypeId() {
        return TEST_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "x-application/test";
    }

    @Override
    public String getSeriName() {
        return "test";
    }

    // We need to implement TestDataOutputStream and TestDataInputStream
    @Override
    public DataOutputStream serialize(OutputStream out) throws IOException {
        return new TestDataOutputStream(out);
    }

    @Override
    public DataInputStream deserialize(InputStream is) throws IOException {
        return new TestDataInputStream(is);
    }
}
```


2. Add SPI file

Add the SPI configuration file in the module's resources to activate this serialization, the file path and file name: 
`META-INF/esa/io.esastack.codec.serialization.api.Serialization`
the file content is as follows:
```
test=xxx.xxx.xxx.TestSerialization
```
