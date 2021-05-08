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

Dubbo协议入门

注意：目前开发使用的是Dubbo 2.7.3版本，请求和响应Header格式是相同的，状态位有差异。
1565610741707_57_image.png
Header 一共是16个字节（byte[16] ）
2byte magic:类似java字节码文件里的魔数，用来判断是不是dubbo协议的数据包。魔数是常量0xdabb
1byte 的消息标志位（分为高4位和底四位）: 17请求或响应 18 two way 19 event 20-24 序列化id
1byte 状态，当消息类型为响应时，设置响应状态。24-31位。状态位, 设置请求响应状态，
8byte 消息ID,long类型，32-95位。每一个请求的唯一识别id（由于采用异步通讯的方式，用来把请求request和返回的response对应上）
4byte 消息长度，96-127位。消息体 body 长度, int 类型，即记录Body Content有多少个字节。
Java计算机位计算

已知byte[] header=byte[16]字节，我们对消息标识位byte[2]数组进行示例,每一个byte[]数组对应计算机8位（bit），采用“|”进行计算
规则是a、b对应位都为1时,c对应位为1；反之为0。
image.png
Int serializeType=2;
byte FLAG_REQUEST = (byte) 0x80;
byte FLAG_TWOWAY = (byte) 0x40;
byte FLAG_EVENT = (byte) 0x20;
header[2] = (byte) (FLAG_REQUEST|serializeType|FLAG_TWOWAY|FLAG_EVENT);
计算步骤：

    -128=10000000
    -128|2=10000010
    -128|2|64=11000010
    -128|2|64|32=11100010

1110 0010 分为高四位和低四位 因为是第三个8字节所以
17请求或响应 18 twoway 19 event 20-24 序列化id
可以算出结果 header[2]=-30
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
static NettyDubboClient dubboNettyClient;
static {
    DubboClientBuilder clientConfig = new DubboClientBuilder()
        .setHost("localhost").setPort(20880)
        .setConnectionPoolSize(64)
        .setConnectTimeout(1000)
        .setConnectionPoolWaitQueueSize(500)
        .setWriteTimeout(1000);

    // this client is better than dubbo
    dubboNettyClient = new NettyDubboClient(clientConfig);
}

@Test
public static void testSend() throws Exception {
    DubboRequest dubboRequest = buildDubboRequest(null);
    CompletableFuture<DubboResponse> response = dubboNettyClient.sendRequest(dubboRequest);
    String result = handleDubboResponse(response.get());
}

public static RpcInvocation buildRpcInvocation() {     
    RpcInvocation rpcInvocation = new RpcInvocation();
    rpcInvocation.setMethodName("sayHello");
    rpcInvocation.setParameterTypes(new Class[]{String.class});
    rpcInvocation.setArguments(new String[]{"dubbo"});
    rpcInvocation.setInterfaceName("org.apache.dubbo.demo.DemoService");
    rpcInvocation.setReturnType(String.class);

    Map<String, String> attachments = new HashMap<>();
    rpcInvocation.setAttachments(attachments);

    return rpcInvocationBuilder;
}

public static DubboRequest buildDubboRequest() throws IOException {
    RpcInvocation rpcInvocation = buildRpcInvocation();
    byte[] body = rpcInvocation.toByteArray(SerializeConstants.HESSIAN2_SERIALIZATION_ID);

    DubboRequest dubboRequest = new DubboRequest()
        .setSeriType(SerializeConstants.HESSIAN2_SERIALIZATION_ID);
    .setBody(body);
    return dubboRequest;
}

public static <T> T handleDubboResponse(DubboResponse response) throws IOException, ClassNotFoundException {
    if (response == null || response.getBody() == null) {
        throw new IllegalArgumentException("response data is null ");
    }
    byte[] body = response.getBody();
    RpcResult rpcResult = new RpcResult();
    rpcResult.fromByteArray(body, response.getSeriType());
    return (T) rpcResult.getResult();
}
```

 #### 3、 Dubbo Server SDK使用说明


```java
static ExecutorService workerThreadPool = ThreadPools.builder()
    .corePoolSize(200)
    .maximumPoolSize(200)
    .useSynchronousQueue()
    .rejectPolicy(new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            LoggerUtils.DUBBO_SERVER_LOG.error("rejectedExecution ");
        }
    }).build();

public static void main(String[] args) throws Exception {
    NettyDubboServer dubboServer = NettyDubboServer.newBuilder()
        .setPort(20881)
        .setBossThreadCount(2)
        .setIoThreadCount(20)
        .setUseNativeTransports(false)
        .setBizHandler(new DubboServerBizHandler() {
            @Override
            public void process(DubboRequest request, DubboResponseHolder dubboResponseHolder) {
                workerThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        long requestId = request.getRequestId();
                        byte seriType = request.getSeriType();

                        //读取元数据，包括类名、方法名、附件等内容
                        DubboRequestMetaData requestMetaData = DubboUtils.readRequestMetaData(request.getBody(), seriType);

                        //返回结果
                        RpcResult result = RpcResult.success("hello  world from client " + requestId + " " + seriType);
                        byte[] body = result.toByteArray((byte) 2);

                        DubboResponse dubboMessage = new DubboResponse()
                            .setRequestId(requestId)
                            .setSeriType(seriType)
                            .setStatus(DubboConstants.RESPONSE_STATUS.OK)
                            .setBody(body);
                        dubboResponseHolder.end(dubboMessage);
                    }
                });

            }

            @Override
            public void shutdown() {

            }
        })
        .build();

    dubboServer.start();
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

