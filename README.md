# ESA dubbo-lite

背景概述

针对Dubbo协议 二进制编码和解码。
功能特性

    完全兼容Dubbo协议
    基于Netty完全重写，不依赖原生Dubbo
    支持只解析元数据，不解析Body(适合代理场景）
    支持Dubbo Server
    支持Dubbo Client
    多种序列化支持

##  SDK使用说明
#### 1、 引入Maven依赖
```xml   
<dependency>
	<groupId>io.esastack</groupId>
	<artifactId>codec-dubbo-client</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
	<groupId>io.esastack</groupId>
	<artifactId>codec-dubbo-server</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```
 #### 2、Dubbo Client SDK使用说明
 ```java
public class DubboSDKClient {
    
    public static void main(String[] args) throws Exception {
        // 构建client
        DubboClientBuilder clientConfig = new DubboClientBuilder()
                .setHost("localhost")
                .setPort(20880)
                .setConnectTimeout(1000)
                .setWriteTimeout(1000);
        NettyDubboClient dubboNettyClient = new NettyDubboClient(clientConfig);

        //构建request
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new String[]{"dubbo"});
        rpcInvocation.setInterfaceName("org.apache.dubbo.demo.DemoService");
        rpcInvocation.setReturnType(String.class);

        Map<String, String> attachments = new HashMap<>();
        rpcInvocation.setAttachments(attachments);

        DubboMessage request = ClientCodecHelper.toDubboMessage(rpcInvocation);

        //发送请求并处理返回值
        CompletableFuture<RpcResult> responseFuture = dubboNettyClient.sendRequest(request, String.class);

        responseFuture.whenComplete((r, t) -> {
            if (t != null || r.getException() != null || StringUtils.isNotEmpty(r.getErrorMessage())) {
                // 异常处理
            }
            // 没有异常，返回值为String，直接强转，其他返回值类型酌情处理，获取返回值以后，自行处理返回值
            String result = (String) r.getValue();
        });
    }
}
```

 #### 3、 Dubbo Server SDK使用说明


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
        NettyDubboServer dubboServer = NettyDubboServer.newBuilder()
                .setPort(20880)
                .setBizHandler(new DubboServerBizHandler() {
                    @Override
                    public void process(DubboMessage request, DubboResponseHolder dubboResponseHolder) {
                        final RpcInvocation invocation;
                        try {
                            invocation = ServerCodecHelper.toRpcInvocation(request);
                        } catch (Exception e) {
                            //TODO 返回错误
                            dubboResponseHolder.end(null);
                            return;
                        }
                        workerThreadPool.execute(() -> {
                            String response = "requestId:" +
                                    invocation.getRequestId() +
                                    " Hello " + invocation.getArguments()[0] +
                                    ", response from provider. seriType:" +
                                    invocation.getSeriType();

                            DubboMessage dubboResponse;
                            try {
                                dubboResponse = ServerCodecHelper.toDubboMessage(
                                        RpcResult.success(
                                                invocation.getRequestId(),
                                                invocation.getSeriType(),
                                                response),
                                        request.getBody().alloc());
                            } catch (SerializationException e) {
                                e.printStackTrace();
                                dubboResponseHolder.getChannelHandlerContext().channel().close();
                            }
                            dubboResponseHolder.end(dubboResponse);
                        });
                    }

                    @Override
                    public void shutdown() {

                    }
                })
                .build();

        dubboServer.start();
    }
}
```


 #### 4、 序列化支持
 
    hessian2	
    fastjson
    json	
    kryo	
    fst	
    protobuf
    protostuff
