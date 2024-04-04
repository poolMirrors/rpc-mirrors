#1.项目整体

## 1.做这个项目的原因

1. **两个不同的服务器上的服务提供的方法不在一个内存空间**，需要**网络编程才能传递方法调用所需要的参数**，方法调用的结果也需要通过网络编程来接收。
2. 如果**手动网络编程来实现这个调用过程的话工作量大**，因为需要**考虑底层传输方式(TCP 还是UDP)、序列化方式等等**方面。
3. RPC可以帮助我们调用远程计算机上某个服务的方法，这个过程就像调用本地方法一样简单。  

## 2.原理

1. **服务端启动**的时候**先扫描**，将**服务名称及其对应的地址(ip+port)注册到注册中心**，这样客户端才能根据服务名称找到对应的服务地址。
2. **客户端去注册中心找服务地址**，有了服务地址之后，客户端就可以通过网络请求服务端了。
3. 客户端**调用远程方法的时候，实际会通过创建代理对象**来传输网络请求。
4. 客户端生成request对象（类名、方法名以及相关参数）作为消息体，拼接消息头，然后**通过Netty传输过去**。
5. 当客户端发起请求的时候，多台服务器都可以处理这个请求。通过**负载均衡选一台服务器**。
6. **服务器收到客户端传过来的信息后进行解码**，看看客户端需要要干什么，然后**反射调用方法得到result**，把result封装成rpcMessage，再传回去。
7. **客户端收到rpcMessage，解码**，拿到自己想要的结果；

## 3.微服务之间如何通信

**单体项目**时：**一次服务调用**发生在**同一台机器**上的**同一个进程内部**，也就是说调用发生在本机内部，因此也被叫作本地方法调用。

**微服务项目**时：**服务提供者**和**服务消费者**运行在**两台不同物理机上的不同进程内**，它们之间的调用相比于本地方法调用，可称之为远程方法调用，简称 RPC

## 4.RPC了解多少？都有哪些？

**参考答案**：RPC全称称： Remote Procedure Calls 远程服务调用，是进行服务之间相互调用的。

**受限语言**的开源 RPC 框架

- **Dubbo**：**阿里**2011年开源，仅支持 Java 语言
- **Spring Cloud Feigh**：国外 Pivotal 公司 2014 年对外开源的 RPC 框架，仅支持 Java 语言(Github:[https://github.com/OpenFeign/feign)【后面又出现了SpringCloud](https://gw-c.nowcoder.com/api/sparta/jump/link?link=https%3A%2F%2Fgithub.com%2FOpenFeign%2Ffeign%29%25E3%2580%2590%25E5%2590%258E%25E9%259D%25A2%25E5%258F%2588%25E5%2587%25BA%25E7%258E%25B0%25E4%25BA%2586SpringCloud) Alibaba， Spring-Cloud-Alibaba 项目由阿里巴巴的开源组件和多个阿里云产品组成，旨在实现和公开众所周知的 Spring 框架模式和抽象，为使用阿里巴巴产品的 Java 开发者带来 Spring-Boot 和 Spring-Cloud 的好处。 】 

**跨语言平台**的开源 RPC 框架主要有以下几种。

- **GRPC**：**Google** 支持多种语言
- **Thrift**：最初**Facebook** 开发的内部框架

## 5.RPC包含哪些部分

一个RPC框架要包含

- 客户端和服务端建立网络连接模块( **server**模块、**client**模块 )
- 服务端**处理请求模块**
- **协议**模块
- **序列化**和**反序列**模块。

## 6.设计一个RPC会考虑哪些问题

设计一个RPC框架，可以从PRC包含的几个模块去考虑，对每一个模块分别进行设计。

- **客户端**和**服务端如何建立网络连接**？
- **服务端**如何**处理请求**？
- **数据传输**采用什么**协议**？
- **数据**该如何**序列化**和**反序列化**？

## 7.客户端调用、服务端响应的一个完整流程 

- 客户端调用远程方法
  - 代理对象发起远程调用
    - 客户端：连接到注册中心拉取服务信息（是否使用本地缓存）
    - 客户端：负载均衡取一个服务
    - 客户端：构建消息【消息头 + 消息体（**服务名，调用的方法名，参数类型，参数值**）】
      - 对象编码->ByteBuf + 序列化
      - ByteBuf解码->对象 + 反序列化
    - 服务端：拿到消息中的服务名称，根据服务名称拿到对应的Bean（本地缓存Service）
    - 服务端：根据消息中的**方法名+参数类型**，Bean.getClass().getMethod()拿到对应方法，反射调用得到结果
    - 服务端：响应结果给客户端
      - 对象编码->ByteBuf + 序列化
      - ByteBuf解码->对象 + 反序列化
    - 客户端：拿到结果返回
  - 返回

# 2.服务端

暴露-扫描-注册

## 1.怎么进行注册

**（1）:warning:自定义扫描注解 `@RpcComponentScan`** + **自定义注册器类`RpcBeanDefinitionRegistry`**

1. 自定义扫描注解`RpcComponentScan`中使用`@Import(RpcBeanDefinitionRegistry.class) `

2. 所有实现了`ImportBeanDefinitionRegistrar`该接口的类的都会被`ConfigurationClassPostProcessor`处理，而`ConfigurationClassPostProcessor`**实现了`BeanFactoryPostProcessor`接口**，所以`ImportBeanDefinitionRegistrar`中动态注册的bean是优先于依赖其的bean初始化

3. 执行流程

   ```java
   public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
       // 1.获取 @RpcComponentScan注解 的属性和值
       AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcComponentScan.class.getName()));
       String[] basePackages = {};
       if (annotationAttributes != null) {
           basePackages = annotationAttributes.getStringArray("basePackages"); // 拿到注解的属性值
       }
       // 如果没有指定包名，设置默认包路径
       if (basePackages.length == 0) {
           StandardAnnotationMetadata metadata = (StandardAnnotationMetadata) annotationMetadata;
           basePackages = new String[]{metadata.getIntrospectedClass().getPackage().getName()};
       }
       // 2.创建 @RpcService注解 的Scanner
       RpcClassPathBeanDefinitionScanner scanRpcService = new RpcClassPathBeanDefinitionScanner(registry, RpcService.class);
       if (resourceLoader != null) {
           scanRpcService.setResourceLoader(resourceLoader);
       }
   
       // 3.扫描包下所有被 @RpcService 标注的bean（scan方法会调用register方法，注册扫描到的类，并生成 BeanDefinition 注册到 spring 容器）
       int count = scanRpcService.scan(basePackages);
       log.info("The number of BeanDefinition scanned and annotated by RpcService is {}.", count);
   }
   ```

**（2）:warning:自定义`@RpcService`注解的`Scanner（RpcClassPathBeanDefinitionScanner）`**

1. `RpcClassPathBeanDefinitionScanner`继承了`ClassPathBeanDefinitionScanner`，`ClassPathBeanDefinitionScanner`作用就是**将指定包下的类**通过一定规则过滤后，**将 `Class信息 `包装成 `BeanDefinition` 的形式注册到`IOC`容器中。** 

**（3）:warning:自定义注解`@RpcService` + 自定义Bean后置处理器`RpcServerBeanPostProcessor`**

1. `RpcServerBeanPostProcessor`实现`BeanPostProcessor`接口

2. 执行流程

   ```java
   @Override
   public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
       // 1.判断当前bean是否被 @RpcService 标注
       if (bean.getClass().isAnnotationPresent(RpcService.class)) {
           log.info("[{}] is annotated with [{}].", bean.getClass().getName(), RpcService.class.getCanonicalName());
           // 2.获取@RpcService注解，并且从注解中获取 服务接口名，版本号
           RpcService rpcServiceAnnotation = bean.getClass().getAnnotation(RpcService.class);
           String interfaceName;
           if ("".equals(rpcServiceAnnotation.interfaceName())) {
               interfaceName = rpcServiceAnnotation.interfaceClass().getName();
           } else {
               interfaceName = rpcServiceAnnotation.interfaceName();
           }
           String version = rpcServiceAnnotation.version();
           // 3.构建服务名称
           String serviceKey = ServiceUtil.getServiceKey(interfaceName, version);
           // 4.构建 ServiceInfo 对象
           ServiceInfo serviceInfo = ServiceInfo.builder()
               .appName(properties.getAppName())
               .serviceName(serviceKey)
               .version(version)
               .ip(properties.getIp())
               .port(properties.getPort())
               .build();
           // 5.调用Nacos或Zk，进行服务注册【注意】
           serviceRegistry.register(serviceInfo);
           // 6.创建 服务 的本地缓存【注意：key是服务名称，value是Bean实例】
           ServiceLocalCache.addService(serviceKey, bean);
       }
       return bean;
   }
   ```

- **:warning:注意：服务端的本地缓存，value是服务Bean实例**，为什么服务端要有本地缓存？？？

- **:warning:注意：服务注册的信息是ServiceInfo**

  ```java
  public class ServiceInfo implements Serializable {
  
      // 应用名称
      private String appName;
  
      // 服务名；服务名称 = 服务名-版本号
      private String serviceName;
  
      // 版本号；服务名称 = 服务名-版本号
      private String version;
  
      // 服务端地址
      private String ip;
  
      // 服务端端口
      private Integer port;
  }
  ```

  



## 2.服务下线

服务下线包含了主动下线和系统宕机等异常方式的下线。

### 临时节点+长连接

- 在 zookeeper 中存在持久化节点和临时节点的概念。
- 持久化节点一经创建，只要不主动删除，便会一直持久化存在；临时节点的生命周期则是和客户端的连接同生共死的，应用连接到 zookeeper 时创建一个临时节点，使用长连接维持会话，这样无论何种方式服务发生下线，zookeeper 都可以感知到，进而删除临时节点。
- zookeeper 的这一特性和服务下线的需求契合的比较好，所以临时节点被广泛应用。

### 主动下线+心跳检测

- 并不是所有注册中心都有临时节点的概念，另外一种感知服务下线的方式是主动下线。
- Nacos下线原理？
- 例如在 eureka 中，会有 eureka-server 和 eureka-client 两个角色，其中 eureka-server 保存注册信息，地位等同于 zookeeper。当 eureka-client 需要关闭时，会发送一个通知给 eureka-server，从而让 eureka-server 摘除自己这个节点。但这么做最大的一个问题是，如果仅仅只有主动下线这么一个手段，一旦 eureka-client 非正常下线（如断电，断网），eureka-server 便会一直存在一个已经下线的服务节点，一旦被其他服务发现进而调用，便会带来问题。为了避免出现这样的情况，需要给 eureka-server 增加一个心跳检测功能，它会对服务提供者进行探测，比如每隔30s发送一个心跳，如果三次心跳结果都没有返回值，就认为该服务已下线。



## 3.如果zk中的服务节点退出了，服务还能访问吗，怎么做 

- 不能
- 本地缓存监听，临时节点被删除，列表发生变化，监听器将本地缓存的列表更新



## 4.如果一个正在被客户端请求的服务提供方准备下线，有什么方法能保证客户端这次能正常通信 

- **优雅关闭：服务提供方可以实现一个优雅关闭的过程，在关闭之前，首先停止接受新的请求**，然后等待？？？当前正在处理的请求完成。这样可以确保正在处理中的请求能够正常完成，而不会中断客户端的通信
- **熔断机制：在客户端实现熔断机制**，当检测到服务提供方的响应时间过长或请求失败的次数达到一定阈值时，可以暂时停止向该服务提供方发送请求，并转而请求其他可用的服务提供方。这样可以避免在服务提供方关闭时持续发送请求，减少通信中断的可能性。
- **请求转发 / 负载均衡：客户端可以将当前正在处理的请求转发给一个可用的备份服务提供方（可用的服务提供方）**。备份服务提供方可以是一个具有相同功能的服务，可以接受并处理转发的请求。这样，在服务提供方下线时，客户端可以将请求发送到备份服务提供方，确保当前请求能够正常执行。
- **客户端缓存：客户端可以在本地缓存服务提供方的响应结果？**。当服务提供方准备下线时，客户端可以继续使用缓存的响应结果，而不需要实时与服务提供方通信。这样可以确保当前请求能够正常完成，而不受服务提供方下线的影响。



## 5.当A调用B服务，B服务假设有10台节点，当其中一台宕机了，怎么做的服务调用的可靠性 

- 失败重试
- 本地服务缓存



##6.Properties配置文件，自定注册Bean

**（1）:warning:自定义配置文件类`RpcServerProperties`**，添加**注解`@ConfigurationProperties`**

1. `@ConfigurationProperties`是`Spring Boo`t新加入的注解，用于**配置文件中的指定键值对映射到一个java实体类**

**（2）自定义配置类`RpcServerAutoConfiguration` ，添加注解`@EnableConfigurationProperties`**

1. `@EnableConfigurationProperties`注解中有 `@Import(EnableConfigurationPropertiesRegistrar.class)`
2. `EnableConfigurationPropertiesRegistrar`**实现了`ImportBeanDefinitionRegistrar`接口**
   1. 所有实现了`ImportBeanDefinitionRegistrar`的都会被`ConfigurationClassPostProcessor`处理，**`ConfigurationClassPostProcessor`**实现了 **`BeanFactoryPostProcessor`** 接口
   2. `ConfigurationClassParser.doProcessConfigurationClass(...)`方法，在这个方法里调用了`processImports(...)`方法处理配置类的`@Import`注解，`getImports(sourceClass)`能从一个配置类上获取`@Import`注解配置的所有类形成一个集合 
3. **将`RpcServerAutoConfiguration`的类信息 转为`BeanDefinition`注册到Spring的IOC容器中**
4. 同时`RpcServerAutoConfiguration`类中**使用`@Bean`注解，创建返回各种配置文件的Bean**



## 7.服务端如何开启通信、退出

- **Bean后置处理器`RpcServerBeanPostProcessor`实现了`CommandLineRunner`接口**

- 重写了接口的 `run(String... args)`方法，在项目启动后运行，开启连接

  ```java
  @Override
  public void run(String... args) throws Exception {
      // 1.开启一个线程进行，让服务端RpcServer开始通信！
      new Thread(() -> {
          rpcServer.start(properties.getPort());
      }).start();
      // 2.jvm中的关闭钩子，在jvm关闭的时候（与注册中心断开连接）
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          try {
              serviceRegistry.disconnect();
          } catch (Exception e) {
              throw new RuntimeException(e);
          }
      }));
  }
  ```

  

#3.客户端

## 1.如何进行发现

- **在连接前**，从`ZooKeeperServiceDiscover`对象中，拿到服务列表；再负载均衡选一个

  ```java
  // 1.服务发现
  public List<ServiceInfo> getServiceInfos(String serviceName) throws Exception {
      // 当 缓存 中没有找到当前服务名
      if (!serviceListMap.containsKey(serviceName)) {
          // 从zookeeper获取服务列表缓存，构建本地缓存（由Curator提供）
          ServiceCache<ServiceInfo> serviceCache = serviceDiscovery.serviceCacheBuilder()
              .name(serviceName).build();
          // 添加服务监听，服务变化时，主动更新本地缓存并通知
          serviceCache.addListener(new ServiceCacheListener() {
              // 1.服务改变时
              @Override
              public void cacheChanged() {
                  log.info("The service [{}] map changed. The current number of service instances is {}.", serviceName, serviceCache.getInstances().size());
                  // 更新到本地缓存列表
                  List<ServiceInfo> serviceInfos = serviceCache.getInstances()
                      .stream()
                      .map(ServiceInstance::getPayload)
                      .collect(Collectors.toList());
                  serviceListMap.put(serviceName, serviceInfos);
              }
  
              // 2.连接状态改变时
              @Override
              public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
                  log.info("The client {} connection status has changed. The current status is: {}.", client, connectionState);
              }
          });
          // 开启监听
          serviceCache.start();
          // 将 服务缓存对象 加入本地
          serviceCacheMap.put(serviceName, serviceCache);
          // 将 服务列表 加入本地
          List<ServiceInfo> serviceInfos = serviceCacheMap.get(serviceName).getInstances()
              .stream()
              .map(ServiceInstance::getPayload)
              .collect(Collectors.toList());
          serviceListMap.put(serviceName, serviceInfos);
      }
      return serviceListMap.get(serviceName);
  }
  
  // 2.负载均衡
  public ServiceInfo select(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest) {
      // 服务列表为空，返回null
      if (serviceInfoList == null || serviceInfoList.isEmpty()) {
          return null;
      }
      // 只有一个服务，直接返回
      if (serviceInfoList.size() == 1) {
          return serviceInfoList.get(0);
      }
      // 多个服务；让子类实现doSelect，进行服务选择
      return doSelect(serviceInfoList, rpcRequest);
  }
  ```



## 2.客户端是否知道什么时候结果已经返回

- 不知道，只是等待
- `channel.writeAndFlush().addListener`添加监听器，
- `channel`发送后，进行超时时间内等待计算



## 3.如何进行代理

**（1）:warning:自定义注解`@RpcReference` + 自定义Bean后置处理器`RpcClientBeanPostProcessor`**

```java
public @interface RpcReference {
    // 接口类型
    Class<?> interfaceClass() default void.class;

    // 全限定接口名
    String interfaceName() default "";

    // 版本号，默认1.0
    String version() default "1.0";

    // 负载均衡策略
    String loadBalance() default "";

    // 服务调用超时时间
    int timeout() default 0;
}
```

```java
// RpcClientBeanPostProcessor后置处理器
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    // 1.获取 bean 的所有属性（不分修饰符）
    Field[] fields = bean.getClass().getDeclaredFields();
    // 2.遍历所有属性
    for (Field field : fields) {
        // 3.判断当前属性是否被 @RpcReference 标注
        if (field.isAnnotationPresent(RpcReference.class)) {
            // 获取注解
            RpcReference rpcReferenceAnnotation = field.getAnnotation(RpcReference.class);
            // 4.获取被 @RpcReference标注 的属性当前类型
            Class<?> clazz = field.getType();
            try {
                if (!"".equals(rpcReferenceAnnotation.interfaceName())) {
                    clazz = Class.forName(rpcReferenceAnnotation.interfaceName());
                }
                if (rpcReferenceAnnotation.interfaceClass() != void.class) {
                    clazz = rpcReferenceAnnotation.interfaceClass();
                }
                // 5.获取指定类型的代理对象【注意】 =》
                Object proxy = clientProxyFactory.getProxy(clazz, rpcReferenceAnnotation.version());
                // 关闭安全检测
                field.setAccessible(true);
                // 6.设置 bean 的代理对象【注意】
                field.set(bean, proxy);

            } catch (Exception e) {
                throw new RuntimeException(String.format("the type of field [%s] is [%s] and the proxy type is [%s]", field.getName(), field.getClass(), clazz), e);
            }
        }
    }
    // 返回bean
    return bean;
}
```

**（2）:warning:创建代理对象**

```java
// 【根据 clazz对象 创建】
public <T> T getProxy(Class<T> clazz, String version) {
    // 1.获取服务名称【根据被标注的属性，拿到clazz去到接口全限定类名！】
    String serviceKey1 = ServiceUtil.getServiceKey(clazz.getName(), version);
    // 2.computeIfAbsent：对 hashMap 中指定 key 的值进行重新计算，不存在这个 key，则添加到 hashMap 中
    Object proxy = proxyCacheMap.computeIfAbsent(serviceKey1, serviceKey2 -> {
        if (clazz.isInterface() || Proxy.isProxyClass(clazz)) {
            // 2.如果目标类是【一个接口】或者【java.lang.reflect.Proxy的子类】，则默认使用 JDK 动态代理
            return Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    new Class[]{clazz},
                    new JdkClientInvocationHandler(serviceDiscover, rpcClient, properties, serviceKey2)
            );
        } else {
            // 3.否则使用Cglib
            Enhancer enhancer = new Enhancer();
            enhancer.setClassLoader(clazz.getClassLoader());
            enhancer.setSuperclass(clazz);
            enhancer.setCallback(new CglibClientMethodInterceptor(serviceDiscover, rpcClient, properties, serviceKey2));
            return enhancer.create();
        }
    });
    // 返回代理
    return (T) proxy;
}
```



## 4.客户端如何进行通信，退出

**（1）:warning:客户端调用时，由于进行了代理，所以执行了代理对象的 :sunny:invoke(JDK) 或 intercept(Cglib)**

```java
@RpcReference // 远程调用服务
private HelloService helloService;

@RequestMapping("/hello/{name}")
public String hello(@PathVariable("name") String name) {
    String s = helloService.sayHello(name); // 代理对象，调用invoke / intercept
    return s;
}
```

**（2）:warning:代理对象进行远程调用（以jdk为例）**

```java
// serviceDiscover服务发现类，rpcClient客户端连接类，properties配置文件类，method调用的方法

@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // JDK远程调用
    return RemoteCall.remoteCall(serviceDiscover, rpcClient, serviceName, properties, method, args);
}
```

```java
public static Object remoteCall(ServiceDiscover serviceDiscover, RpcClient rpcClient, String serviceName, RpcClientProperties properties, Method method, Object[] args) {
    // 1.构建请求头
    RpcMessageHeader header = RpcMessageHeader.createBySerializer(properties.getSerialization());
    // 2.构建请求体（RpcRequest作为请求体）
    RpcRequest rpcRequest = new RpcRequest();
    rpcRequest.setServiceName(serviceName); 			 // 服务名
    rpcRequest.setMethodName(method.getName()); 		 // 调用的方法名
    rpcRequest.setParamTypes(method.getParameterTypes()); // 参数类型
    rpcRequest.setParamValues(args); 					// 参数值

    // 3.服务发现【从注册中心拉取 + 负载均衡 + 本地缓存】
    ServiceInfo serviceInfo = serviceDiscover.discover(rpcRequest);
    if (serviceInfo == null) {
        throw new RuntimeException(String.format("The service [%s] was not found in the remote registry center", serviceName));
    }

    // 4.构建Rpc发送的信息【请求头 + 请求体】
    RpcMessage rpcMessage = new RpcMessage();
    rpcMessage.setRpcMessageHeader(header);
    rpcMessage.setRpcMessageBody(rpcRequest);

    // 构建请求元数据
    RequestMetadata metadata = RequestMetadata.builder() 
        .rpcMessage(rpcMessage)				// Rpc消息
        .serverIp(serviceInfo.getIp())		 // 服务ip（服务发现）
        .serverPort(serviceInfo.getPort())	 // 服务端口（服务发现）
        .timeout(properties.getTimeout()) 
        .build();

    // 5.发送网络请求，返回结果 =》
    RpcMessage responseRpcMessage = rpcClient.sendRpcRequest(metadata);
    if (responseRpcMessage == null) {
        throw new RuntimeException("Remote procedure call timeout");
    }

    // 6.拿到 消息正文
    RpcResponse response = (RpcResponse) responseRpcMessage.getRpcMessageBody();
    if (response.getExceptionValue() != null) {
        throw new RuntimeException(response.getExceptionValue());
    }

    // 7.返回 远程调用响应结果
    return response.getReturnValue();
}
```

**（2.1）:warning:服务发现：从注册中心拉取 + 负载均衡 + 本地缓存（key=服务名，value=ServiceInfo服务信息）+ 本地监听注册中心**

```java
@Override
public ServiceInfo discover(RpcRequest rpcRequest) {
    try {
        // 1.从注册中心拉取，本地缓存，客户端监听注册中心
        List<ServiceInfo> serviceInfos = getServiceInfos(rpcRequest.getServiceName());
        // 2.负载均衡，从服务列表中选择一个
        return loadBalance.select(serviceInfos, rpcRequest);

    } catch (Exception e) {
        throw new RuntimeException("nacos did not find service", e);
    }
}
```

```java
@Override
public List<ServiceInfo> getServiceInfos(String serviceName) throws NacosException {
    // 1.当【本地缓存】中没有找到当前服务名
    if (!serviceMap.containsKey(serviceName)) {
        // 2.从nacos获取服务列表，将 服务端提供的服务实例 全部映射为 ServiceInfo列表
        List<ServiceInfo> serviceInfos = namingService.getAllInstances(serviceName)
            .stream()
            .map(instance -> ServiceUtil.toServiceInfo(instance.getMetadata()))
            .collect(Collectors.toList());
        // 3.加入本地缓存
        serviceMap.put(serviceName, serviceInfos);

        // 4.客户端创建 指定服务名称 的【监听事件】，实时监听更新本地缓存缓存列表（回调函数）
        namingService.subscribe(serviceName, event -> {
            NamingEvent namingEvent = (NamingEvent) event;
            log.info("The service [{}] map changed. The current number of service instances is {}.", serviceName, namingEvent.getInstances().size());
            // 5.更新本地服务列表缓存
            List<ServiceInfo> newServiceInfos = namingEvent.getInstances()
                .stream()
                .map(instance -> ServiceUtil.toServiceInfo(instance.getMetadata()))
                .collect(Collectors.toList());
            serviceMap.put(namingEvent.getServiceName(), newServiceInfos);
        });
    }
    
    // 返回服务列表
    return serviceMap.get(serviceName);
}
```

**（2.2）:warning:客户端建立连接（Netty为例）**

```java
@Override
public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
    try {
        // 1.创建 接受响应结果的 promise【Netty提供】
        Promise<RpcMessage> promise;
        // 2.获取连接的channel对象 =》
        Channel channel = getChannel(new InetSocketAddress(requestMetadata.getServerIp(), requestMetadata.getServerPort()));
        if (channel.isActive()) {
            //（1）连接成功，使用 promise 接受结果（指定 执行完成通知的线程）
            promise = new DefaultPromise<>(channel.eventLoop());
            // 获取 序列号id
            int sequenceId = requestMetadata.getRpcMessage().getRpcMessageHeader().getSequenceId();
            // 保存 还没处理请求结果的 promise！
            NettyRpcResponseHandler.UNPROCESSED_RESPONSE.put(sequenceId, promise);
            // 发送数据，监听发送状态
        channel.writeAndFlush(requestMetadata.getRpcMessage()).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.debug("the client send message successfully: [{}]", requestMetadata.getRpcMessage());
                } else {
                    future.channel().close(); // 发送失败，关闭channel
                    promise.setFailure(future.cause());
                    log.error("the client send message fail: ", future.cause());
                }
            });

            //（2）获取超时时间
            Integer timeout = requestMetadata.getTimeout();
            // 等待结果返回，让出cpu资源，同步阻塞调用线程main，其他线程去执行获取操作（eventLoop）
            if (timeout == null || timeout <= 0) {
                // 如果没有指定超时时间，则 await 直到 promise 完成
                promise.await();
            } else {
                // 在超时时间内等待结果返回
                boolean isSuccess = promise.await(timeout, TimeUnit.MILLISECONDS);
                if (!isSuccess) {
                    promise.setFailure(new TimeoutException(String.format("remote call exceeded timeout of %dms.", timeout)));
                }
            }

            //（3）已经等到响应结果，如果调用成功立即返回
            if (promise.isSuccess()) {
                return promise.getNow();
            } else {
                throw new RuntimeException(promise.cause());
            }

        } else {
            throw new RuntimeException("the channel is inactivate");
        }

    } catch (Exception e) {
        throw new RuntimeException("error occur while sending: ", e);
    }
}
```



## 5.重用Channel和Promise

**（1）:warning:在连接时，本地缓存Channel对象**

```java
/**
 * 保存和获取 连接的对应Channel对象
 * <p>
 * key=ip和端口
 * <p>
 * value=channel
 */
private final NettyChannelCache channelCache;


public Channel getChannel(InetSocketAddress socketAddress) {
    // 【重用Channel】，如果多次调用，直接从Map取即可，无需再次创建
    Channel channel = channelCache.get(socketAddress);
    if (channel == null) {
        // 如果没有找到连接的channel对象，说明还没进行连接；先进行连接 =》
        channel = connect(socketAddress);
        // 连接成功后，保存连接的channel对象
        channelCache.set(socketAddress, channel);
    }
    return channel;
}


public Channel connect(InetSocketAddress socketAddress) {
    try {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        // 1.客户端开始连接，添加监听器，接收到响应回调！
        bootstrap.connect(socketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("the client successfully connected to server [{}]", socketAddress.toString());
                // 传入结果future.channel()，表明已经执行完
                completableFuture.complete(future.channel());
            } else {
                throw new RuntimeException(String.format("the client failed to connect to [%s].", socketAddress.toString()));
            }
        });
        // 2.阻塞等待 future 返回 连接结果
        Channel channel = completableFuture.get();
        // 3.添加 异步 关闭之后的操作
        channel.closeFuture().addListener(future -> {
            log.info("the client disconnected from server [{}].", socketAddress.toString());
        });
        return channel;

    } catch (Exception e) {
        throw new RuntimeException("error occur while connecting: ", e);
    }
}
```

**（2）:warning:Promise模式**

```java
public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
    // 声明Promise
    Promise<RpcMessage> promise;
    // 连接成功，使用 promise 接受结果（指定执行完成通知的线程）
    promise = new DefaultPromise<>(channel.eventLoop());
    // 保存还没处理请求结果的 promise【重点】
    NettyRpcResponseHandler.UNPROCESSED_RESPONSE.put(sequenceId, promise); // key为信息的序列号
    
    // 如果没有指定超时时间，则 await 直到 promise 完成
    promise.await();
    // 已经等到响应结果，如果调用成功立即返回
    if (promise.isSuccess()) {
        return promise.getNow();
    } else {
        throw new RuntimeException(promise.cause());
    }
}
```



# 4.注册中心

## 1.Zookeeper知识

###1.为什么用zookeeper做注册中心

1. 作为注册中心而言，配置是不经常变动的，只有当**新版本发布或者服务器出故障时会变动**。
2. **CP 不合适于配置经常变动的**，而 AP 在遇到问题时可以牺牲其一致性来保证系统服务的高可用性，既返回旧数据。
3. **只是一个demo，环境稳定，流量小，不会遇到注册中心的实例（节点）半数以上都挂了的情况**。所以在实际生产环境中，选择 Zookeeper 还是选择 Eureka，这个就要取决于系统架构师对于业务环境的权衡了

### 2.对比zookeeper和Eureka

CP，AP；

强一致性，高可用；

3个角色，平等；

选取leader时不可用，不保证强一致性

### 3.数据节点

zk数据模型中的最小数据单元**，数据模型是一棵树，由斜杠（/）分割的路径名唯一标识**，数据节点可以存储数据内容及一系列属性信息，同时还可以挂载子节点，构成一个层次化的命名空间。 

### 4.会话

1. **zk客户端与zk服务器之间的一个TCP长连接**，通过这个长连接，客户端能够使用心跳检测与服务器保持有效的会话，也能向服务器发送请求并接收响应，还可接收服务器的Watcher事件通知
2. Session的sessionTimeout，是**会话超时时间，如果这段时间内，客户端未与服务器发生任何沟通（心跳或请求），服务器端会清除该session数据，客户端的TCP长连接将不可用**，这种情况下，客户端需要重新实例化一个Zookeeper对象。

### 5.事务 ZXID

1. **事务是指能够改变Zookeeper服务器状态的操作，一般包括数据节点的创建与删除、数据节点内容更新和客户端会话创建与失效等操作。**
2. 对于每个事务请求，zk都会为其分配一个全局唯一的事务ID，即ZXID，是一个64位的数字，高32位表示该事务发生的集群选举周期（集群每发生一次leader选举，值加1），低32位表示该事务在当前选择周期内的递增次序（leader每处理一个事务请求，值加1，发生一次leader选择，低32位要清0）

### 6.事务日志

1. 所有事务操作都是需要记录到日志文件中的，可通过 dataLogDir配置文件目录，文件是以写入的第一条事务zxid为后缀，方便后续的定位查找。
2. zk会采取“**磁盘空间预分配**”的策略，来**避免磁盘Seek频率**，提升zk服务器对事务请求的影响能力。
3. 默认设置下，**每次事务日志写入操作都会实时刷入磁盘，也可以设置成非实时**（写到内存文件流，定时批量写入磁盘），但那样断电时会带来丢失数据的风险。

### 7.数据快照

1. 数据快照用来**记录zk服务器上某一时刻的全量内存数据内容，并将其写入到指定的磁盘文件中**，可通过dataDir配置文件目录。
2. 可配置参数snapCount，**设置两次快照之间的事务操作个数**，zk节点记录完事务日志时，会统计判断是否需要做数据快照（距离上次快照，事务操作次数等于snapCount/2~snapCount 中的某个值时，会触发快照生成操作，**随机值是为了避免所有节点同时生成快照，导致集群影响缓慢）**

### 8.过半

1. 指大于集群机器数量的一半，即大于或等于（n/2+1），此处的“集群机器数量”不包括observer角色节点。
2. **leader广播一个事务消息后，当收到半数以上的ack信息时，就认为集群中所有节点都收到了消息，然后leader就不需要再等待剩余节点的ack，直接广播commit消息，提交事务**。
3. 选举中的投票提议及数据同步时，也是如此，leader不需要等到所有learner节点的反馈，只要收到过半的反馈就可进行下一步操作。 

### 9.zookeeper服务节点挂掉之后，怎么删除它

使用临时节点，会话失效，节点自动清除。 

### 10.zookeeper集群节点宕机了怎么发现剔除的

1. 发现：watcher机制
2. 剔除：临时节点
3. **zookeeper心跳检测更新列表并利用watcher机制发给客户端**

### 11.心跳机制

1. 服务提供者**定时向注册中心发送本机地址（心跳数据包）**
2. **注册中心的监控则维持一个channelId和具体地址的map，并且通过IdleHandler监听空闲事件**，到达一定的空闲次数则认为不活跃，当不活跃时（这里的不活跃条件是5分钟内3次以上没有发送心跳包），zookeeper删除相应的url节点，但后续的逻辑没有继续做。
3. 删掉结点后，通知客户端。



### 12.注册中心对于服务端掉线时怎么处理

**移出ip链表，发送给客户端，等待服务器上线，重新连接**

### 13.集群

1. 多装几个，修改配置文件文件；
2. 因为是在一台机器上模拟集群，所以端口不能重复，这里用2181~2183，2287~2289，以及3387~3389相互错开；
3. 在**每个zk server配置文件的dataDir所对应的目录下，必须创建一个名为myid的文件**，其中的内容必须与zoo.cfg中server.x 中的x相同；
4. 【启动】ZooKeeper zk = new ZooKeeper(" 172.28.20.102:2181, 172.28.20.102:2182, 172.28.20.102:2183", 300000, new DemoWatcher());

### 14.数据模型

zk维护的数据主要有：客户端的会话（session）状态及数据节点（dataNode）信息。 

### 15.一致性算法

ZAB 算法主要包含两个阶段：

1. **Leader Election（领导者选举）**：在 ZooKeeper 集群中，ZAB 算法会选举出一个节点作为领导者（Leader），负责协调所有节点的状态。领导者负责接收客户端的写请求，并将其广播给其他节点。
2. **Atomic Broadcast（原子广播）**：领导者接收客户端的写请求后，会将其转换成一个序列化的事务（Transaction），然后将该事务发送给所有的 ZooKeeper 节



## 2.Zookeeper使用

### 1.注册过程

【Curator】

1. **创建CuratorFramework对象zkClient**，zkClient：地址127.0.0.1:2181，**重试策略**，**start连接zookeeper**；
2. 先去**本地路径map里面看看有没有创建过**；
3. **创建结点**，根结点（完整的服务名；子节点：IP+port ）
4. 创建完了**把路径添加到map中去**

### 2.服务调用

1. **参数为RpcRequest对象，从中取出服务名**，创建对象zkClient，连接zookeeper；
2. 获取结点下面的子节点，存到本地map中去，返回string的list；
3. 在本地 Map<String, List\<String>> 中找；
4. **没找到，连接zookeeper，获取子节点路径列表，在map中保存；**
5. **负载均衡策略选出一个子节点服务地址进行连接**；

### 3.监听器

1. Zookeeper会通过心跳检测机制，来**判断服务提供端的运行状态**，来决定是否应该把这个服务从地址列表剔除。
2. 监听器的作用**在于监听某一节点，若该节点的子节点发生变化**，比如增加减少，更新操作的时候，我们可以**自定义回调函数**。
3. 一旦这个节点下的子节点发生变化，Zookeeper Server就会**发送一个事件通知客户端。**
4. 客户端收到事件以后，就会**把本地缓存的这个服务地址删除，这样后续就不会把请求发送到失败的节点上，完成服务下线感知**。

### 4.为什么不选择Redis作为注册中心

zookeeper临时节点自动宕机自动清除； 



### 5.zookeeper服务容灾？zookeeper服务节点挂掉之后，怎么删除它？

1. **容灾：在集群若干台故障后，整个集群仍然可以对外提供可用的服务**。
   1. 一般配置**奇数台**去构成集群，以避免资源的浪费。
   2. 三机房部署是最常见的、容灾性最好的部署方案。
2. **删除：使用临时节点，会话失效，节点自动清除**。

### 6.zookeeper的问题

崩溃恢复无法提供服务、写的性能瓶颈是一个问题、选举过程速度缓慢、无法进行有效的权限控制； 



## 3.Nacos

### 如何避免读写冲突 

- [Copy-On-Write 的思想是什么？](https://javaguide.cn/java/collection/copyonwritearraylist-source-code.html#copy-on-write-%E7%9A%84%E6%80%9D%E6%83%B3%E6%98%AF%E4%BB%80%E4%B9%88)
- Nacos在更新实例列表时，会采用**CopyOnWrite技术**，首先**将旧的实例列表拷贝一份，然后更新拷贝的实例列表**，再用**更新后的实例列表来覆盖旧的实例列表。**
- 这样在更新的过程中，就不会对读实例列表的请求产生影响，也不会出现脏读问题了。

### 数据更新通知的实现

- Nacos 客户端会**循环请求服务端变更的数据**，并且超时时间设置为30s，当**配置发生变化时，请求的响应会立即返回**，否则会一直等到 29.5s+ 之后再返回响应

- Nacos 客户端能够实时感知到服务端配置发生了变化。

- **实时感知是建立在 客户端"拉" 和 服务端"推" 的基础上**，但是这里的服务端“推”需要打上引号，因为服务端和客户端直接本质上还是通过 http 进行数据通讯的，之所以有“推”的感觉，是因为**服务端主动将变更后的数据通过 http 的 response 对象提前写入了。**

   

## 4.Nacos对比Zookeeper

- **一致性模型**：
  - ZooKeeper：ZooKeeper 是一个分布式协调服务，采用了**强一致性模型CP**，通过 ZAB（ZooKeeper Atomic Broadcast）协议实现数据的一致性和可靠性。ZooKeeper 提供了顺序一致性、原子性和持久性的保证，适用于对数据一致性要求较高的场景。
  - Nacos：Nacos 也是一个分布式协调和配置中心，但**它采用了 AP（可用性和分区容忍性）模型**，以保证服务的可用性和容错能力。Nacos 支持 AP 模型下的一致性，提供了最终一致性的保证，适用于高可用和分布式系统的场景。**（既支持AP，也支持CP ）**
- **功能特性**：
  - ZooKeeper：ZooKeeper 提供了**数据节点的创建、读取、更新和删除等基本操**作，可以作为分布式协调服务和配置中心使用。ZooKeeper 还提供了一些高级特性，**如临时节点**、顺序节点、观察者机制等。
  - Nacos：Nacos 提供了**更丰富的配置管理和服务发现功能，包括动态配置管理、服务注册与发现**、动态 DNS 解析等。Nacos 还支持多种配置格式，如 Properties、YAML、JSON 等，并且提供了命名空间、集群管理、配置推送等高级特性。

## 5.注册中心作用

1. **服务注册**：各个服务在启动时向注册中心注册自己的网络地址、服务实例信息和其他相关元数据。这样，其他服务就可以通过注册中心获取到当前可用的服务列表。
2. **服务发现**：客户端通过向注册中心查询特定服务的注册信息，获得可用的服务实例列表。这样客户端就可以根据需要选择合适的服务进行调用，实现了服务间的解耦。
3. **负载均衡**：注册中心可以对同一服务的多个实例进行负载均衡，将请求分发到不同的实例上，提高整体的系统性能和可用性。
4. **故障恢复**：注册中心能够监测和检测服务的状态，当服务实例发生故障或下线时，可以及时更新注册信息，从而保证服务能够正常工作。
5. **服务治理**：通过注册中心可以进行服务的配置管理、动态扩缩容、服务路由、灰度发布等操作，实现对服务的动态管理和控制

# 5.负载均衡

## 1.背景

1. 系统中的某个服务的访问量大，将这个服务部署在了多台服务器上，当客户端发起请求的时候，多台服务器都可以处理这个请求。
2. 如何**正确选择处理该请求的服务器就很关键**。
3. 负载均衡为了**避免单个服务器响应同一请求，容易造成服务器宕机、崩溃等问题**。



## 2.一致性哈希

- [9.4 什么是一致性哈希？ | 小林coding (xiaolincoding.com)](https://xiaolincoding.com/os/8_network_system/hash.html) 

1. 【原理】hash空间组成一个**虚拟的圆环**，将**各个服务器使用 Hash 函数进行哈希**，可以选择**服务器的IP或主机名**作为关键字进行哈希，从而确定每台机器在哈希环上的位置。将数据key使用相同的函数Hash计算出哈希值，并确定此数据在环上的位置，从此位置**沿环顺时针寻找，第一台遇到的服务器就是其应该定位到的服务器**。
2. 【优点】对于**节点的增减都只需重定位环空间中的一小部分数据，只有部分缓存会失效，不至于将所有压力都在同一时间集中到后端服务器上**，具有较好的容错性和可扩展性。
3. 【结构】一个服务名对应一棵树，ConcurrentHashMap<String，ConsistentHashSelector(TreeMap)>，服务名和TreeMap的对应。
4. 【插入】ConsistentHashSelector中有 TreeMap<Long, String>(<哈希值，地址>)。**传入一个装着地址的list，对每一个地址生成160个虚拟结点，求MD5，再求hash，最后存放在树中。**
5. 【查询】入参为rpcRequest，**拿到服务名，去Map里面找对应的selector**；
   1. 如果为空，则新建；
   2. 如果不为空，**对服务名求MD5，拿到hashcode。每个hashcode对应一棵树。**
6. 【TreeMap】**<哈希值Long，地址String>，到 TreeMap 中查找第一个节点值大于或等于当前 hash 的 String。找到了就返回ip + port，没找到就返回第一个。**



## 3.dubbo的负载均衡

1. RandomLoadBalance：根据权重随机选择（对加权随机算法的实现）；
2. LeastActiveLoadBalance：最小活跃数负载均衡。初始状态下所有服务提供者的活跃数均为 0（每个服务提供者的中特定方法都对应一个活跃数），每收到一个请求后，对应的服务提供者的活跃数 +1，当这个请求处理完之后，活跃数 -1。
3. Dubbo 就认为谁的活跃数越少，谁的处理速度就越快，性能也越好，这样的话，我就优先把请求给活跃数少的服务提供者处理；
4. ConsistentHashLoadBalance：一致性哈希；
5. RoundRobinLoadBalance：加权轮询负载均衡，加权轮询就是在轮询的基础上，让更多的请求落到权重更大的服务提供者上。

## 4.一致性哈希和普通哈希有什么区别

- **数据分布均匀性**：
  - 普通的哈希算法：普通的哈希算法将数据均匀地映射到固定数量的桶（或者节点）中，**但当增加或减少节点时，需要重新分配数据，可能会导致大量的数据迁移。**
  - 一致性哈希算法：一致性哈希算法将数据通过哈希函数映射到一个固定范围的环上，节点通过哈希函数映射到环上的位置。当增加或减少节点时，只需要重新映射和迁移部分数据，可以保持大部分数据的分布不变，减少数据迁移的开销。
- **节点的扩展性**：
  - 普通的哈希算法：**普通的哈希算法通常需要事先确定节点的数量，当节点数量变化时，需要重新计算数据的哈希值并进行数据迁移。**
  - 一致性哈希算法：一致性哈希算法支持动态扩展和收缩节点，当增加或减少节点时，只需要迁移一部分数据，不需要重新计算所有数据的哈希值。



## 5.一致性哈希在rpc调用中适合什么场景 

- **负载均衡**：一致性哈希可以用于实现 RPC 服务的负载均衡，将请求分布到不同的服务器节点上，实现请求的均衡分担。通过一致性哈希，可以保证同一个请求总是被分配到同一个服务器节点上，避免了服务器节点的频繁变动导致的缓存不命中和连接状态丢失等问题。 
- **数据分片**：在大规模的分布式系统中，一致性哈希可以用于将数据分片存储在不同的服务器节点上，实现数据的分布式存储和管理。通过一致性哈希，可以保证数据的分布均匀，并且能够动态扩展和收缩节点，提高了系统的可扩展性和灵活性。 

 # 6.序列化

## 1.序列化对比

- Kryo、Hessian、~~Protostuff~~、JDK、JSON
- 参考
  - [Kryo、Protostuff、Hessian序列化方式对比 - 掘金 (juejin.cn)](https://juejin.cn/post/7115642653718347783#heading-1) 
  - [深入浅出序列化（1）——JDK序列化和Hessian序列化 - 掘金 (juejin.cn)](https://juejin.cn/post/6991473304011800590#heading-3) 
  - [深入浅出序列化（2）——Kryo序列化 - 掘金 (juejin.cn)](https://juejin.cn/post/6993647089431347237#heading-2) 
  - [Hessian序列化、反序列化 - 掘金 (juejin.cn)](https://juejin.cn/post/7130152442008141838) 

【比较】

![](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/725bb9d08e12410dac80d0d5f7e3e569~tplv-k3u1fbpfcp-zoom-in-crop-mark:1512:0:0:0.awebp?)

- JDK
  - **包含类信息（额外的元数据）**：JDK 序列化会在序列化的数据中包含类的信息，例如**类名、字段名**等，以便在反序列化时恢复对象的结构。这些额外的类信息会增加序列化后数据的体积。
  - **不压缩**：JDK 序列化生成的数据通常不会进行额外的压缩处理，这意味着生成的数据可能会比较冗余，体积较大
  - **反射操作**：JDK 序列化机制在序列化和反序列化过程中会使用**反射来动态地访问对象的字段和方法**
  - JDK序列化选择将所有的信息保存下来，因此可靠性更好 
- JSON
  - **文本格式**：JSON 是一种文本格式，它使用**可读的文本**来表示数据。**相比于二进制格式，文本格式通常会更大**，因为它需要包含更多的字符来表示相同的数据。
  - **字符编码**：JSON 数据通常**使用 UTF-8 编码**，这是一种可变长度的字符编码，对于非 ASCII 字符，需要更多的字节来表示。因此，如果 JSON 中包含大量的非 ASCII 字符，那么数据体积就会更大。
  - **无压缩**：一般情况下，JSON 数据通常不会进行额外的压缩处理。虽然可以使用一些压缩算法来压缩 JSON 数据，但这通常需要额外的处理步骤，而原始的 JSON 数据本身并不会被压缩。
- Hessian
  - **二进制格式**：Hessian 使用二进制格式来表示数据，相比于文本格式，二进制格式通常会更加紧凑，因为它不需要额外的字符来表示数据结构和分隔符。 
  - **压缩算法**：Hessian 可能会使用压缩算法来进一步减小数据体积，尤其是对于较大的数据结构。压缩算法可以去除数据中的重复信息，从而减小数据体积。 
  - **使用固定长度存储int和long？** 
  - 序列化结果的结构：
    - 类定义开始标识 C 
    - 类名长度 + 类名 
    - 属性数量 
    - （逐个）属性名长度+属性名 
    - 开始实例化标识 
    - （按照属性名顺序，逐个设置）属性值（发现某个属性是一个对象，循环这个过程）
- Kryo
  - **二进制格式**：Kryo 使用二进制格式来表示数据，相比于文本格式，二进制格式通常会更加紧凑，因为它不需要额外的字符来表示数据结构和分隔符。例如使**用变长编码来表示整数，以及去除了字段名称和类型信息等冗余数据**。这样可以有效地减小序列化结果的体积 
  - **高效的数据压缩**：Kryo 在序列化过程中可以使用**高效的数据压缩算法**，例如 Snappy、LZ4 等，来进一步减小数据体积。这些压缩算法可以去除数据中的重复信息，并且在保持数据完整性的同时减小数据体积。
  - **不包含额外信息**：Kryo 序列化数据中只包含**对象的字段值**，而**不包含字段名、类型等额外的信息**。这样可以减小序列化数据中的冗余信息，从而减小数据体积。
  - **使用变长的int和long保证这种基本数据类型序列化后尽量小？** ，**利用一个标记来记录字段类型** 



## 2.为什么不使用JDK

1. **不支持跨语言调用**；
2. 性能差：相比于其他序列化框架性能更低，主要原因是**序列化之后的字节数组体积较大，导致传输成本加大**。



## 3.为什么选Kryo，Kryo特点

1. Kryo 是专门针对 Java 语言序列化方式并且性能非常好
2. Kryo由于其**变长存储特性**并使用了**字节码生成机制**，拥有**较高的运行速度和较小的字节码体积。**
3. 因为 **Kryo 不是线程安全的**。**使用 ThreadLocal 来存储 Kryo 对象，一个线程一个 Kryo 实例** 



## 4.序列化与反序列的方式有哪些

- **文本类**（XML / JSON）
- **二进制类**（Protobuf / Hessian / Kryo / JDK /……）



## 5.为什么要进行序列化和反序列化

- 程序在运行过程中，产生的数据，**不能一直保存在内存中，需要暂时或永久存储到介质**（如磁盘、数据库、文件）里进行保存，也可能**通过网络发送**给协作者。程序获取原数据，需要从介质，或网络传输获得。**传输的过程中，只能使用【二进制流】进行传输。**
- **压缩数据，加快网络传输**。【 网络传输耗时一方面取决于网络带宽大小，另一方面取决于数据传输量。想加快网络传输，要么提高带宽，要么减小数据传输量，而对数据进行编码的主要目的就是减小数据传输量。比如一部高清电影原始大小为 30GB，如果经过特殊编码格式处理，可以减小到 3GB，同样是 100MB/s 的网速，下载时间可以从 300s 减小到 30s。 



## 6.Dubbo默认使用Hessian2序列化算法，其原理是什么

参考：[Hessian序列化、反序列化 - 掘金 (juejin.cn)](https://juejin.cn/post/7130152442008141838#heading-13) 

- 序列化结果的结构：
  - **类定义开始标识 C** 
  - **类名长度+类名** 
  -  属性数量
  - （逐个）属性名长度+属性名 
  - 开始实例化标识
  - （**按照属性名顺序**，逐个设置）属性值（发现某个属性是一个对象，循环这个过程）

# 7.I/O

## 1.概述

1. 【I/O】linux系统内核read()、write()函数
   1. read把 **内核缓冲区中的数据 复制到 用户缓冲区**中
   2. write把 **用户缓冲区的数据 写入到 内核缓冲区**中。
2. 【阻塞和非阻塞】进程在**访问数据的时候**，根据IO操作的就绪状态来采取的不同方式，是一种读取或者写入操作函数的实现方式。
   1. 用户进程发起read操作，阻塞则会一直**等待内存缓冲区数据完整后再解除阻塞**；
   2. 非阻塞是指IO操作**被调用后立即返回给用户一个状态值**，无需等到IO操作彻底完成。
3. 【同步/异步】是用户线程与内核的交互方式。
   1. 同步是指**用户线程发起IO请求后需要等待或者轮询内核IO操作完成后**才能继续执行；
   2. 异步是指**用户线程发起IO请求后仍继续执行**，当内核IO操作完成后会通知用户线程，或者调用用户线程注册的回调函数。

## 2.非阻塞IO怎么判断数据是否准备好

- 轮询 

## 3.BIO、NIO、AIO

1. 【BIO】
   1. 服务器实现模式为**一个连接一个线程**，即**客户端有连接请求时服务器端就需要启动一个线程进行处理，如果这个连接不做任何事情会造成不必要的线程开销**。

   2. 应用程序**发起 read 调用后，会一直阻塞，直到内核把数据拷贝到用户空间**。

      > **客户端发一次请求，服务端生成一个对应线程去处理**。当客户端同时发起的请求很多时，服务端需要创建多个线程去处理每一个请求，当达到了系统最大的线程数时，新来的请求就无法处理了。 
2. 【NIO】
   1. 应用程序会**一直发起 read 调用，等待数据从内核空间拷贝到用户空间的这段时间里**

   2. **线程依然是阻塞的，直到在内核把数据拷贝到用户空间。**

      > **客户端发一次请求，服务端并不是每次都创建一个新线程来处理，而是通过 I/O 多路复用技术进行处理**。就是把多个 I/O 的阻塞复用到同一个 select 的阻塞上，从而使系统在**单线程的情况下可以同时处理多个客户端请求**。这种方式的优势是开销小，不用为每个请求创建一个线程，可以节省系统开销。
3. 【IO多路复用（异步阻塞）】
   1. 多路网络连接可以**复用一个I/O线程**。应用程序不断**进行 I/O 系统调用轮询**数据是否已经准备好是十分消耗 CPU 资源的。
   2. IO 多路复用模型中，线程**首先发起 select 调用**，询问**内核数据是否准备就绪**，**阻塞等待select系统调用返回**。
   3. 等内核把数据准备好了**返回一个ready，用户线程再发起 read 调用**。**read 调用的过程（数据从内核空间 -> 用户空间）还是阻塞的。**
4. 【AIO异步非阻塞】
   1. 基于**事件和回调机制**实现的，发起IO操作之后会直接返回，不会堵塞在那里，当后台处理完成，操作系统会通知相应的线程进行后续的操作，Proactor。

      > **客户端发起一个 I/O 操作然后立即返回，等 I/O 操作真正完成以后，客户端会得到 I/O 操作完成的通知**，此时客户端只需要对数据进行处理就好了，不需要进行实际的 I/O 读写操作，因为真正的 I/O 读取或者写入操作已经由内核完成了。这种方式的优势是客户端无需等待，不存在阻塞等待问题

## 4.适用场景

1. BIO方式适用于**连接数目比较小**，**长请求且固定的架构**，这种方式对服务器资源要求比较高，并发局限于应用中，下载一个大文件；
2. NIO的适用场景：**高并发数目多**，比较轻，**高访问量，短请求**，聊天服务器，弹幕系统，服务器间通讯；
3. AIO方式使用于**连接数目多**且**连接比较长(重操作)**的架构，比如相册服务器，充分调用OS参与并发操作，编程比较复杂。

## 5.多路复用

###1.概述

IO多路复用的本质就是**通过系统内核缓冲IO数据，让单个线程可以监视多个文件描述符（FD）**，一旦某个描述符读就绪或者写就绪，可以**通知程序进行相应的读写操作，也就是使用单个线程同时处理多个网络连接IO**，它的原理**就是select、poll、epoll不断轮询所负责的socket，当某个socket有数据达到了，就通知用户进程**。

### 2.select

1. 过程是阻塞的。
2. 仅知道有几个I/O事件发生了，但并不知道具体是哪几个socket连接有I/O事件，还需要轮询去查找，时间复杂度为O(n)，处理的请求数越多，所消耗的时间越长。
3. 【select函数执行流程】
   1. 从 **用户空间** 拷贝 **fd_set(注册的事件集合)** 到 **内核空间**；
   2. 遍历所有fd文件，并将**当前进程挂到每个fd的等待队列中**，**当某个fd文件设备收到消息后，会唤醒设备等待队列上睡眠的进程**，那么当前进程就会被唤醒；
   3. 如果**遍历完所有的fd没有I/O事件，则当前进程进入睡眠**，当有**某个fd文件有I/O事件或当前进程睡眠超时后**，当前进程重新唤醒再次遍历所有fd文件。
4. 【缺点】
   1. **单个进程所打开的FD是有限制的**，通过 FD_SETSIZE 设置，默认1024；
   2. 每次调用 select，都需要把 fd 集合从用户态拷贝到内核态，这个开销在 fd 很多时会很大。

### 3.poll

poll本质上和select没有区别，它将**用户传入的数组拷贝到内核空间**，然后查询每个fd对应的设备状态， 但是它没有最大连接数的限制，原因是它是基于链表来存储的。 

### 4.epoll epoll_create() , epoll_ctl() , epoll_wait()

1. **事件驱动机制，修改主动轮询为被动通知，当有事件发生时，被动接收通知**。所以epoll模型注册套接字后，主程序可做其他事情，当事件发生时，接收到通知后再去处理。**epoll会把哪个流发生哪种I/O事件通知我们**。
2. epoll是事件驱动，即**每个事件关联上fd，每当fd就绪，系统注册的回调函数就会被调用，将就绪的fd放到readyList里面，是基于红黑树实现的。**
3. 【流程】
   1. 通过 **epoll_create() 函数创建一个文件**，返回一个文件描述符fd（Linus系统一切对象皆为文件）
   2. 创建4号socket接口，绑定socket号与端口号，监听事件，标记为非阻塞。**通过epoll_ctl() 函数将 该socket号 以及 需要监听的事件（如listen事件）写入fd中**。
   3. 循环**调用epoll_wait() 函数进行监听，返回已经就绪事件序列的长度**（返回0则说明无状态，大于0则说明有n个事件已就绪）。
   4. 例如：如果有客户端进行连接，则，**再调用accept()函数与4号socket进行连接**，连接后**返回一个新的socket号，且需要监听读事件，则再通过epoll_ctl()将新的socket号以及对应的事件（如read读事件）写入fd中，epoll_wait()进行监听。循环往复**。
4. 【优点】
   1. **不需要再遍历所有的socket号来获取每一个socket的状态**，只需要**管理活跃的连接**。
   2. 即**监听在通过epoll_create()创建的文件中注册的socket号以及对应的事件**。
   3. 只有产生就绪事件，才会处理，所以操作都是有效的，为O(1)。

### 5.epoll的水平触发和边缘触发

1. LT(水平触发)：当**被监控的fd上有IO事件发生时，epoll_wait()会通知处理程序去读写**。如果这次没有把数据一次性全部读写完(如读写缓冲区太小)，那么下次调用 epoll_wait()时，**它还会通知你在上没读写完的文件描述符上继续读写**，当然如果你一直不去读写，它会一直通知你！
2. ET(边缘触发)：当**被监控的fd上有可读写事件发生时**，epoll_wait()会通知处理程序去读写。如果这次**没有把数据全部读写完(如读写缓冲区太小)**，**那么下次调用epoll_wait()时，它不会通知你，也就是它只会通知你一次，直到该文件描述符上出现第二次可读写事件才会通知你**。这种模式**比水平触发效率高，系统不会充斥大量你不关心的就绪文件描述符。**

## 6.NIO

### 1.NIO组件

1. **一个线程对应一个selector，一个selector对应多个channel(连接)，每个channel 都会对应一个Buffer；**
2. buffer：可以读写数据的内存块；
3. channel：用于数据的读写；
4. selector：

### 2.NIO的缺点

1. NIO的类库和API繁杂，使用麻烦，需要熟练掌握**Selector**、**ServerSocketChannel**、**SocketChannel**、**ByteBuffer**等。
2. 工作量和难度都非常大。
3. JDK NIO的BUG，例如臭名昭著的epoll bug，它会导致Selector空轮询，最终导致CPU 100%。官方声称在JDK 1.6版本修复了该问题，但是直到JDK 1.7版本该问题仍旧存在，只不过该BUG发生概率降低了一些而已，它并没有得到根本性解决。

## 7.Reactor线程模型

Reactor模式基于事件驱动，分为**单Reactor单线程**模型、**单Reactor多线程**模型、**主从Reactor多线程**模型。 

### 1.单reactor单线程

1. 【工作原理】
   1. 服务器端**用一个线程通过多路复用搞定所有的I0操作**(建立连接，读、写等)。
   2. 但是如果客户端连接数量较多，将无法支撑，NIO案例就属于这种模型。
2. 【特点】
   1. 模型简单，没有多线程、进程通信、竞争的问题，全部都在一个线程中完成。
   2. **只有一个线程，无法完全发挥多核CPU的性能**。
   3. Handler 在处理某个连接上的业务时，整个进程无法处理其他连接事件，很容易导致性能瓶颈。

### 2.单reactor多线程

1. 【原理】**一个线程负责管理连接，一组线程负责处理IO操作**。
2. 【特点】充分的**利用多核cpu** 的处理能力，多线程数据共享和访问比较复杂， **reactor 处理所有的事件的监听和响应**，在单线程运行，在高并发场景容易出现性能瓶颈。

### 3.主从Reactor多线程

1. **主线程 MainReactor 对象通过select 监听连接事件**, 收到事件后，**通过 Acceptor 处理连接事件；**
2. 当 Acceptor 处理连接事件后，**MainReactor 将连接分配给 SubReactor；** 
3. **SubReactor将连接加入到连接队列进行监听，并创建handler进行各种事件处理**；
4. 当有新事件发生时，subreactor 就会**调用对应的handler处理**；
5. **handler 通过 read 读取数据，分发给后面的 worker 线程处理；**
6. worker 线程池**分配独立的worker 线程进行业务处理，并返回结果**；
7. **handler 收到响应的结果后**，再**通过send 将结果返回给client；**

# 8.Netty

## 0.面试题，参考学习

- 【重点参考】
  - [Netty 常见面试题总结 (yuque.com)](https://www.yuque.com/snailclimb/mf2z3k/wlr1b0) 
  - [聊聊Netty那些事儿之从内核角度看IO模型 (qq.com)](https://mp.weixin.qq.com/s/zAh1yD5IfwuoYdrZ1tGf5Q) 【公众号：bin的技术小屋】



## 1.Netty理论知识

### 1.对Netty的认识

1. **基于NIO的client-server框架**，使用它可以快速简单地开发网络应用程序。
2. **简化了TCP和UDP套接字服务器等网络编程**，并且性能以及安全性等很多方面甚至都要更好。
3. **支持多种协议如**FTP，SMTP，HTTP 以及各种二进制和基于文本的传统协议。

### 2.Netty优点

1. **高并发**：Netty 是一款**基于 NIO开发**的网络通信框架，对比于 BIO，并发性能得到了很大提高，修复了已经发现 NIO BUG。
2. **传输快**：传输**依赖于零拷贝**特性，尽量减少不必要的内存拷贝，实现了更高效率的传输。
3. **封装好**：Netty 封装了 NIO 操作的很多细节，提供了易于使用调用接口。
4. **API使用简单**，开发门槛低；功能强大，预置了多种编解码功能，支持多种主流协议；

### 3.零拷贝

①Netty 的接收和发送 ByteBuffer 采用 DIRECT BUFFERS，使用**堆外直接内存**进行 Socket 读写，**不需要进行字节缓冲区的二次拷贝**。如果使用**传统的堆内存(HEAP BUFFERS)进行 Socket 读写，JVM 会将堆内存 Buffer 拷贝一份到直接内存中，然后才写入 Socket 中**。相比于堆外直接内存，消息在发送过程中多了一次缓冲区的内存拷贝。

②Netty 提供了**组合 Buffer 对象，可以聚合多个 ByteBuffer 对象，用户可以像操作一个 Buffer 那样方便的对组合 Buffer 进行操作，避免了传统通过内存拷贝的方式将几个小 Buffer 合并成一个大的 Buffer**。

③Netty 的**文件传输采用了 transferTo 方法，它可以直接将文件缓冲区的数据发送到目标 Channel，避免了传统通过循环 write 方式导致的内存拷贝问题**。

### 4.Netty的高性能

- [从7个角度解释Netty为什么这么快？深入实践基于Netty构建Dubbo服务 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/458666288) 

①**IO线程模型**：**同步非阻塞**，用最少的资源做更多的事。

②**内存零拷贝**：尽量减少不必要的内存拷贝，实现了更高效率的传输。

③**内存池设计**：**申请的内存可以重用，主要指直接内存**。内部实现是用一颗**二叉查找树**管理内存分配情况。

④可靠性，链路有效性检测：**链路空闲检测机制，读/写空闲超时机制**；

⑤内存保护机制：**通过内存池重用ByteBuf**；ByteBuf的**解码保护**；优雅停机：不再接收新消息、退出前的预处理操作、资源的释放操作。

⑥串行无锁化设计：**消息的处理尽可能在同一个线程内完成**，期间不进行线程切换，这样就避免了多线程竞争和同步锁。

⑦**高性能序列化协议**：支持 protostuff 等高性能序列化协议。

⑧安全性：SSL V2和V3，TLS，SSL单向认证、双向认证和第三方CA认证。

⑨TCP参数配置：SO_RCVBUF和SO_SNDBUF：通常建议值为128K或者256K；SO_TCPNODELAY：NAGLE算法通过将缓冲区内的小封包自动相连，组成较大的封包，阻止大量小封包的发送阻塞网络，从而提高网络应用效率。但是对于时延敏感的应用场景需要关闭该优化算法

### 5.Netty和Tomcat的区别

作用不同：**Tomcat 是 Servlet 容器**，可以视为 **Web 服务器**，而 **Netty 是异步事件驱动的网络应用程序框架和工具用于简化网络编程**，例如TCP和UDP套接字服务器。

协议不同：**Tomcat 是基于 http 协议的 Web 服务器**，而 **Netty 能通过编程自定义各种协议，因为 Netty 本身自己能编码/解码字节流**，所有 Netty 可以实现，HTTP 服务器、FTP 服务器、UDP 服务器、RPC 服务器、WebSocket 服务器、Redis 的 Proxy 服务器、MySQL 的 Proxy 服务器等等。

### 6.Netty和Socket的区别

【socket】

1. **Socket编程主要涉及到客户端和服务端两个方面，首先是在服务器端创建一个服务器套接字（ServerSocket），并把它附加到一个端口上，服务器从这个端口监听连接**。
2. 客户端请求与服务器进行连接的时候，根据服务器的域名或者IP地址，加上端口号，打开一个套接字。当**服务器接受连接后，服务器和客户端之间的通信就像输入输出流一样进行操作。**

【socket缺点】

1. **需对传输的数据进行解析，转化成应用级的数据；**
2. 对开发人员的开发水平要求高；
3. 相对于Http协议传输，增加了开发量；

### 7.Netty发送消息的方式

1. 直接写入 Channel 中，消息从 ChannelPipeline 当中尾部开始移动；
2. 【用的这个】**写入和 ChannelHandler 绑定的 ChannelHandlerContext 中**，消息从 **ChannelPipeline 中的下一个 ChannelHandler 中移动**。

### 8.时间轮

1. **时间槽划分**：时间轮将**时间划分为固定数量的时间槽**，每个时间槽代表一个时间间隔。任务被放置在对应的时间槽中等待执行。
2. **时间轮指针**：时间轮维护一个指针，指向当前时间槽。时间轮以固定的速度（一般为 1 毫秒）进行旋转，指针顺时针移动，依次指向下一个时间槽。
3. **任务调度**：当时间轮的指针指向某个时间槽时，时间轮会检查该时间槽中是否有待执行的任务。如果有任务，则执行任务，并将时间槽中的任务移除。
4. **延迟任务**：时间轮支持延迟任务的执行，即任务可以在指定的延迟时间之后执行。延迟任务被放置在对应的延迟时间槽中，并在时间轮旋转时逐渐向前移动，直到到达指定的延迟时间槽时执行任务。
5. **任务取消**：时间轮支持取消待执行的任务，可以根据任务的唯一标识符取消任务的执行。
6. **优化策略**：时间轮采用**哈希桶的方式存储任务，可以快速定位到指定的时间槽，提高了任务的查找效率**。同时，时间轮还采用了延迟加载的策略，即只有在指针指向某个时间槽时才会检查该时间槽中的任务，减少了不必要的遍历操作。

### 9.Future和Promise

- 参考：[Netty异步回调模式-Future和Promise剖析 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/385350359) 
- JDK的Future的任务结果获取需要**主动查询**，而Netty的Future通过添加**监听器Listener**，可以做到异步非阻塞处理任务结果，可以称为**被动回调**。 
- Netty的Future继承JDK的Future，通过 Object 的 **wait/notify机制**，实现了**线程间的同步**；使用**观察者设计模式**，实现了异步非阻塞回调处理 
- Promise\<V>作为一个特殊的Future\<V>，只是增加了一些状态设置方法。所以它常用于传入I/O业务代码中，用于I/O结束后设置成功（或失败）状态，并回调方法 





## 2.Netty线程模型

### 1.线程模型

Netty主要**基于主从Reactors多线程模型**做了一定的改进，其中**主从Reactor多线程模型有多个Reactor。**

1. Netty抽象出**两组线程池**，**Boss负责客户端的连接**，**Worker专门负责网络的读写**，都是【NioEventLoopGroup】；
2. **NioEventLoopGroup一个事件循环组**，一个**组中含有多个事件循环，每一个事件循环是NioEventLoop**；
3. **NioEventLoop 表示不断循环的执行处理任务的线程**，每个NioEventLoop**都有一个 selector**，用于**监听绑定在其上的socket的网络通讯**。还有一个**TaskQueue**；
4. **Boss里面的NioEventLoop循环【重点】**
   1. **轮询accept事件**；
   2. **处理accept事件，与client建立连接**，**生成 NioScocketChannel，并将其注册到某个workerNioEventLoop上的selector**；
   3. 处理任务队列的任务，即runAllTasks；
5. **Worker里面的NioEventLoop循环【重点】**
   1. **轮询read/write事件；**
   2. **处理i/o事件，在NioScocket**；
   3. **Channel处理；处理任务队列的任务**，即runAllTasks；
6. **每个WorkerNioEventLoop处理业务时，会使用pipeline，pipeline 中包含了channel**，即通过pipeline可以获取到对应通道，管道中维护了很多的处理器；



### 2.异步模型

1. 异步和同步的区别是：当一个异步过程开始时，调用者是不能立刻得到结果的。是调用完成之后通过回调来通知调用者的。
2. **Netty的IO操作是异步的，Bind、 Write、 Connect 等操作会返回一个Channel Future**。
3. 调用者虽然不能直接获得结果，但是**可以通过Future-Listener机制监听，让调用者获得结果。**
4. **Netty模型是建立在future - callback 机制的**，callback 就是回调。
5. **Future的核心思想是**，假设一个方法fun计算过程非常耗时，等待fun 返回显然不合适。那么可以**在调用fun 的时候，立马返回一个Future**, 后续可以**通过Future 去监控方法fun 的处理过程(即: Future-Listener 机制)。**



### 3.处理方法

1. 客户端：**通过ChannelFuture 接口的 addListener() 方法注册**一个ChannelFuture Listener ，当操作执行成功或者失败时，**监听就会自动触发返回结果**。
2. 服务器：可以**通过ChannelFuture接口的 sync() 方法让异步的操作编程同步的**。



## 3.Netty核心组件

**一个线程**  ->  **一个EventLoop**  ->  **多个channel**  ->  **一个pepiline** ->  **很多handler** 



### 1.Bytebuf 

（字节容器）通过**字节流**进行传输的。 



### 2.Channel 

（网络读写操作抽象类）**通过Channel可以进行I/O操作**，客户端成功**连接服务端，就会新建一个Channel 同该用户端进行绑定**，为**每个Channel分配一个EventLoop**。 



### 3.Bootstrap启动类

1. 【作用】
   1. **设置线程组**，
   2. **设置通道NioSocketChannel**，
   3. **连接超时时间**，
   4. **handler**（pipeline、心跳IdleStateHandler、编码器、解码器、处理器），
   5. **connect某个端口**（连接TCP），
   6. 添加监听器，
   7. **连接成功获得channel，放到CompletableFuture中**。
2. 【连接】Bootstrap **在调用 bind()（连接UDP）**和 **connect()（连接TCP）**方法时，会新创建一个 Channel，来实现所有的网络交换。



### 4.ServerBootstrap启动类

1. 【作用】
   1. **设置2个线程组**，
   2. **设置通道NioServerSocketChannel**，
   3. **设置保持活动连接状态**，
   4. childHandler（pipeline、心跳IdleStateHandler、编码器、解码器、处理器），
   5. 启动服务器(并绑定bind端口)，
   6. **异步改为同步，生成了一个 ChannelFuture 对象。**
2. 【连接】ServerBootstarp在**调用 bind()方法时会创建一个 ServerChannel 来接受来自客户端的连接**，并且**该 ServerChannel 管理了多个子 Channel 用于同客户端之间的通信**



### 5.EventLoopGroup，NioEventLoopGroup

1. EventLoop 的主要作用实际就是负责**监听网络事件并调用事件处理器进行相关I/0操作**(读写)的处理，处理连接的生命周期中所发生的事件。
2. **客户端一个线程组，服务器两个**（boss接收客户端的连接，worker专门负责网络的读写）



### 6.Channel-Pepiline-handler

1. **1个Channel 包含1个ChannelPipeline**。
2. **1个ChannelPipeline上可以有多个ChannelHandler**。
3. ChannelHandler是消息的具体处理器，主要**负责处理客户端/服务端接收和发送的数据**。 



### 7.ChannelFuture

1. **Netty中所有的I/O操作都为异步的，不能立刻得到操作结果**。
   1. 法一（客户端）：**通过ChannelFuture 接口的addListener() 方法注册一个ChannelFutureListener** ，当操作执行成功或者失败时，监听就会自动触发返回结果。
   2. 法二（服务器）：可以**通过ChannelFuture接口的sync() 方法让异步的操作编程同步的。**
2. **客户端channel中收到Response后，会自动先进入pipeline中add的Decoder()解码**（从bytebuf到Object），**解码后的object数据再进入handler强转成Response类型后续处**



# 9.自定义消息协议

## 1.本项目实现

```java
public RpcFrameDecoder() {
    // 数据帧的最大长度，消息长度字段的偏移字节数，长度域所占的字节数
    super(1024, 12, 4);
}

-----------------------------------------------------------------------
| 魔数 (4byte) | 版本号 (1byte)  | 序列化算法 (1byte) | 消息类型 (1byte) |
-----------------------------------------------------------------------
|    状态类型 (1byte)  |    消息序列号 (4byte)   |    消息长度 (4byte)   |
-----------------------------------------------------------------------
|                        消息内容 (不固定长度)                          |
-----------------------------------------------------------------------
```



## 2.自定义消息协议的优缺点 

优点：

1. **定制性强**：自定义通信协议可以根据具体的业务需求和场景来设计，可以满足特定的通信需求，包括数据格式、消息类型、协议版本、安全性等方面的定制。
2. **性能优化**：通过自定义通信协议，可以针对具体的业务特点进行性能优化，例如优化数据传输的格式和结构、减少数据包的大小、降低网络通信的延迟等，从而提高系统的性能和吞吐量。
3. **灵活性**：自定义通信协议可以根据业务需求和技术发展进行灵活调整和扩展，可以随着业务的变化和发展进行定制和优化，提高系统的适应性和灵活性。

缺点：

1. **兼容性和扩展性**：自定义通信协议可能存在兼容性和扩展性方面的问题，特别是在协议版本升级和跨平台通信时，需要考虑协议的兼容性和扩展性，避免影响系统的稳定性和可靠性。
2. **标准化问题**：自定义通信协议可能缺乏标准化和通用性，与其他系统或者开发框架的集成和交互可能存在难题。在跨团队、跨组织或者跨平台通信时，需要考虑与其他系统的协议兼容性和一致性。
3. **安全风险**：自定义通信协议可能存在安全风险，包括数据泄露、数据篡改、拒绝服务攻击等安全问题。因此，在设计和实现自定义通信协议时，需要加入相应的安全机制和防护措施，保护通信数据的安全性和完整性。



## 3.自定义消息协议和传统的TCP通信有什么区别 

- 头不携带一些校验 控制信息
- 三种方式解决粘包
- 最后都是读成字节流，解析成头体      



## 4.HTTP如何解决粘包半包

- [聊聊TCP协议的粘包、拆包以及http是如何解决的？_tcp 粘包/拆包-CSDN博客](https://blog.csdn.net/cj_eryue/article/details/131046881) 
- Content-Length ？

# 10.通信

## 1.Netty、Socket、Http 三种通信方式的区别 

1. **Socket**：
   - **基本原理**：Socket是一种**传输层通信协议，通过TCP/IP协议进行通信**。它提供了一种在网络上进行**双向数据**传输的机制，允许客户端和服务器之间建立连接，并进行数据的收发。
2. **HTTP**：
   - **基本原理**：HTTP是一种**应用层协议，基于TCP/IP协议**，用于在客户端和服务器之间传输数据。它建立在**请求-响应模式**的基础上，客户端发送HTTP请求，服务器返回HTTP响应。
   - **特点**：HTTP是一种**无状态协议，每个请求都是独立的**，服务器不会保存客户端的状态信息。它使用简单、易于理解的文本格式进行通信，通常在Web开发中使用。HTTP协议也支持加密和安全性，可以使用HTTPS进行加密传输 
3. **Netty**：
   - **基本原理**：Netty是一个**基于Java NIO的网络通信框架**，提供了**高性能、回调机制来实现异步、非阻塞的网络通信 **驱动的网络编程模型。它封装了底层的网络通信细节，提供了简单易用的API，使得开发人员可以更轻松地实现网络通信功能。
   - **特点**：Netty提供了高度可定制的、**基于事件驱动的网络编程模型，支持TCP、UDP等多种协议**，以及各种高级特性，如流量控制、拥塞控制、安全认证等。

## 2.应用场景

1. **Socket**：
   - **实时通信**：Socket适用于需要实时性较高的通信场景，如聊天应用、在线游戏等。由于Socket可以直接控制网络通信细节，因此能够实现低延迟、高并发的实时通信。
   - **自定义协议**：对于需要自定义通信协议的场景，如物联网设备之间的通信、传感器数据采集等，Socket提供了灵活的方式来实现**自定义的数据传输格式和通信协议**。
2. **HTTP**：
   - **Web应用开发**：HTTP是构建**Web应用最常用的通信协议，适用于网页浏览、Web服务、RESTful API等场景**。由于HTTP使用简单易懂的**文本格式进行通信**，因此非常适合用于构建Web应用。
   - **静态资源传输**：对于**传输静态资源（如HTML、CSS、JavaScript文件）的场景**，HTTP具有良好的支持。通过HTTP服务器可以方便地向客户端提供各种类型的文件。
   - **跨平台通信**：由于HTTP是一种标准的应用层协议，因此可以跨平台、跨语言地进行通信，适用于各种不同环境下的通信需求。
3. **Netty**：
   - **分布式系统**：在构建分布式系统时，通常需要处理**大量的网络通信和异步事件**。Netty提供了异步事件驱动的编程模型，使得构建分布式系统变得更加容易。
   - **网络协议开发**：对于需要开发复杂网络协议或实现特定通信需求的场景，Netty提供了丰富的功能和灵活的扩展性，可以满足各种定制化的通信需求。

## 3.如何用Socket实现Netty线程模型

1. 创建一个**ServerSocket实例来监听指定的端口，接受客户端的连接请求。**
2. 创建一个**【线程池】，用于处理客户端的连接**。线程池可以使用Java的Executor框架来创建，例如ThreadPoolExecutor。
3. **在主线程中，使用一个【循环】来接受客户端的连接请求**。
4. 每当有客户端连接请求到来时，将该连接交给线程池中的一个线程进行处理。
5. 在线程中，**可以使用Socket实例来与客户端进行通信，包括读取和写入数据。**
6. 在处理客户端请求的过程中，可以使用事件驱动的方式，例如使用Selector类来实现非阻塞IO，以提高处理效率。
7. 在处理完客户端请求后，关闭Socket连接。

## 4.Socket的阻塞和线程阻塞有什么不同

1. **阻塞的对象不同**：Socket阻塞是指Socket的**读取和写入操**作在网络通信时的阻塞，而线程阻塞是指**线程在执行过程中**的阻塞。
2. **阻塞的原因不同**：Socket阻塞是由于**网络通信的特性，需要等待数据的到达或空闲的缓冲区可用**。线程阻塞是由于线程执行过程中的某种条件或事件导致的，例如**等待锁、等待I/O操作完成等。**
3. **阻塞的效果不同**：Socket阻塞会**暂停当前Socket的读取或写入操作，直到满足条件或事件发生**。线程阻塞会暂停当前线程的执行，直到满足条件或事件发生。
4. **处理方式不同：Socket阻塞通常使用循环等待的方式**，直到**数据可用或缓冲区可写入**。线程阻塞通常使用线程的阻塞方法，例如**Object.wait()或Thread.sleep()**，等待条件或事件的发生。

## 5.Socket通信流程

- [Socket通信流程_socket流程-CSDN博客](https://blog.csdn.net/fightsyj/article/details/86251421#:~:text=1%20%E6%9C%8D%E5%8A%A1%E7%AB%AF%E8%BF%99%E8%BE%B9%E9%A6%96%E5%85%88%E5%88%9B%E5%BB%BA%E4%B8%80%E4%B8%AASocket%EF%BC%88Socket%20%28%29%EF%BC%89%EF%BC%8C%E7%84%B6%E5%90%8E%E7%BB%91%E5%AE%9AIP%E5%9C%B0%E5%9D%80%E5%92%8C%E7%AB%AF%E5%8F%A3%E5%8F%B7%EF%BC%88Bind%20%28%29%EF%BC%89%EF%BC%8C%E4%B9%8B%E5%90%8E%E6%B3%A8%E5%86%8C%E7%9B%91%E5%90%AC%EF%BC%88Listen%20%28%29%EF%BC%89%EF%BC%8C%E8%BF%99%E6%A0%B7%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%B0%B1%E5%8F%AF%E4%BB%A5%E7%9B%91%E5%90%AC%E6%8C%87%E5%AE%9A%E7%9A%84Socket%E5%9C%B0%E5%9D%80%E4%BA%86%EF%BC%9B%202%20%E5%AE%A2%E6%88%B7%E7%AB%AF%E8%BF%99%E8%BE%B9%E4%B9%9F%E5%88%9B%E5%BB%BA%E4%B8%80%E4%B8%AASocket%EF%BC%88Socket%20%28%29%EF%BC%89%E5%B9%B6%E6%89%93%E5%BC%80%EF%BC%8C%E7%84%B6%E5%90%8E%E6%A0%B9%E6%8D%AE%E6%9C%8D%E5%8A%A1%E5%99%A8IP%E5%9C%B0%E5%9D%80%E5%92%8C%E7%AB%AF%E5%8F%A3%E5%8F%B7%E5%90%91%E6%9C%8D%E5%8A%A1%E5%99%A8Socket%E5%8F%91%E9%80%81%E8%BF%9E%E6%8E%A5%E8%AF%B7%E6%B1%82%EF%BC%88Connect,%28%29%E5%87%BD%E6%95%B0%E6%8E%A5%E6%94%B6%E8%AF%B7%E6%B1%82%EF%BC%8C%E8%BF%99%E6%A0%B7%E5%AE%A2%E6%88%B7%E7%AB%AF%E5%92%8C%E6%9C%8D%E5%8A%A1%E5%99%A8%E4%B9%8B%E9%97%B4%E7%9A%84%E8%BF%9E%E6%8E%A5%E5%B0%B1%E5%BB%BA%E7%AB%8B%E5%A5%BD%E4%BA%86%EF%BC%9B%204%20%E6%88%90%E5%8A%9F%E5%BB%BA%E7%AB%8B%E8%BF%9E%E6%8E%A5%E4%B9%8B%E5%90%8E%E5%B0%B1%E5%8F%AF%E4%BB%A5%E4%BD%A0%E4%BE%AC%E6%88%91%E4%BE%AC%E4%BA%86%EF%BC%8C%E5%AE%A2%E6%88%B7%E7%AB%AF%E5%92%8C%E6%9C%8D%E5%8A%A1%E5%99%A8%E8%BF%9B%E8%A1%8C%E6%95%B0%E6%8D%AE%E4%BA%A4%E4%BA%92%EF%BC%88Receive%20%28%29%E3%80%81Send%20%28%29%EF%BC%89%EF%BC%9B%205%20%E5%9C%A8%E8%85%BB%E6%AD%AA%E5%AE%8C%E4%B9%8B%E5%90%8E%EF%BC%8C%E5%90%84%E8%87%AA%E5%85%B3%E9%97%AD%E8%BF%9E%E6%8E%A5%EF%BC%88Close%20%28%29%EF%BC%89%EF%BC%8C%E4%BA%A4%E4%BA%92%E7%BB%93%E6%9D%9F%EF%BC%9B) 

## 6.本地方法调用和远程过程调用有什么区别 

1. **通信方式**：
   - 本地方法调用：调用方直接调用被调用方的方法，方法调用是通过内存中的函数调用栈实现的，通信开销很小。
   - 远程过程调用：调用方通过网络向被调用方发送请求消息，被调用方接收请求消息并执行相应的方法，然后将结果返回给调用方。这涉及到网络通信、序列化和反序列化等额外的开销。
2. **安全性**：
   - 本地方法调用：由于是在同一台计算机上执行，因此调用方可以直接访问被调用方的内存空间，存在较小的安全风险。
   - 远程过程调用：由于涉及到网络通信，需要考虑网络安全问题，如数据加密、身份认证等，以防止数据被窃取、篡改或伪造。
3. **性能**：
   - 本地方法调用：由于不涉及网络通信，通常具有较低的延迟和较高的性能。
   - 远程过程调用：由于涉及到网络通信和额外的序列化、反序列化等开销，通常具有较高的延迟和较低的性能。
4. **可靠性**：
   - 本地方法调用：由于是在同一台计算机上执行，因此通常具有较高的可靠性，不容易受到网络故障或者网络延迟的影响。
   - 远程过程调用：由于涉及到网络通信，可能会受到网络故障、网络延迟等因素的影响，导致调用的不可靠性，需要进行相应的容错处理。

## 7.能说下HTTP和RPC的区别吗

在微服务体系里，**基于HTTP风格的远程调用通常使用框架如Feign来实现**，**基于RPC的远程调用通常使用框架如Dubbo来实现。**

- 定义
  - HTTP（超文本传输协议）是一种用于**传输超文本**的协议。
  - RPC（远程过程调用）是一种用于实现分布式系统中**不同节点之间通信**的协议。
- **通信方式**
  - HTTP基于**请求-响应**模型，客户端发送请求，服务器**返回响应**。
  - RPC基于**方法调用**模型，客户端**调用远程方法并等待结果**。
- **传输协议**
  - **HTTP基于TCP协议**，可使用其他**传输层协议如TLS**/SSL进行安全加密。
  - RPC可以使用多种传输协议，如TCP、UDP等（位于传输层？）
- **数据格式**
  - HTTP**基于文本**，常用的数据格式有JSON、XML等。
  - RPC可以使用**各种数据格式**，如二进制、JSON、Protocol Buffers等。
- 灵活性
  - HTTP更加灵活，适用于不同类型的应用场景，如Web开发、API调用等。
  - RPC更加高效，适用于需要高性能和低延迟的分布式系统。



## 8.为什么要用异步

- :sunny:参考：[(3 封私信 / 80 条消息) netty到底是同步还是异步？ - 知乎 (zhihu.com)](https://www.zhihu.com/question/469794520) 
- 异步RPC使得**调用端和被调用端可以同时进行其他操作**。这大大提高了系统的并发性能和吞吐量。
- 在异步RPC中，当客户端发起远程过程调用时，它**不需要等待调用完成就可以继续执行其他任务**。被调用端在完成后会将结果通知给客户端。这种模型使得客户端可以**同时发起多个远程过程调用**，并且可以在等待结果的同时执行其他任务。 





















