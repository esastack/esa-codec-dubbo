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
