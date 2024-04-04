# 3.rpc-server服务端模块

## 1.pom文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!--父项目-->
    <parent>
        <artifactId>rpc-mirrors</artifactId>
        <groupId>com.mirrors</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>rpc-server</artifactId>
    <packaging>jar</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <encoding>UTF-8</encoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>
	
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <!--引入 rpc-core 的jar包-->
        <dependency>
            <groupId>com.mirrors</groupId>
            <artifactId>rpc-core</artifactId>
            <optional>true</optional>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-core</artifactId>
            <version>${tomcat.version}</version>
            <optional>true</optional>
        </dependency>
        <!--注解解析器-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

## 2.辅助类

### 1.本地缓存cache

```java
package com.mirrors.server.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务本地缓存，将服务实体类缓存在本地
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 22:42
 */
@Slf4j
public class ServiceLocalCache {
    /**
     * 服务本地缓存
     */
    private static final Map<String, Object> serviceLocalCacheMap = new ConcurrentHashMap<>();

    /**
     * 添加服务到本地缓存
     *
     * @param serviceKey
     * @param object
     */
    public static void addService(String serviceKey, Object object) {
        serviceLocalCacheMap.put(serviceKey, object);
        log.info("[{}] was successfully added to the local cache.", serviceKey);
    }

    /**
     * 获取服务的本地缓存实体类
     *
     * @param serviceKey
     * @return
     */
    public static Object getService(String serviceKey) {
        return serviceLocalCacheMap.get(serviceKey);
    }

    /**
     * 删除本地缓存的实体类
     *
     * @param serviceKey
     */
    public static void removeService(String serviceKey) {
        serviceLocalCacheMap.remove(serviceKey);
        log.info("[{}] was removed from local cache", serviceKey);
    }
}
```

### 2.反射调用方法handler

```java
package com.mirrors.server.handler;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.server.cache.ServiceLocalCache;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 根据 RpcRequest 中的信息，反射调用本地服务实例
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 15:16
 */
@Slf4j
public class RpcRequestHandler {

    /**
     * 处理 RpcRequest，反射调用 ServiceLocalCache 中的本地缓存，返回调用结果
     *
     * @param rpcRequest
     * @return
     */
    public Object invokeService(RpcRequest rpcRequest) throws Exception {
        // 获取服务实例
        Object service = ServiceLocalCache.getService(rpcRequest.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("the service [%s] is not exist", rpcRequest.getServiceName()));
        }
        // 获取 RpcRequest请求 要调用的方法
        Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
        // 调用方法，返回结果
        Object result = method.invoke(service, rpcRequest.getParamValues());
        
        return result;
    }

}
```

## 3.自定义配置类

### 1.RpcServerProperties

```java
package com.mirrors.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 服务配置类，要提供getter和setter，注入属性
 * 提供给 server端 使用（读取properties文件注入到类属性中）
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 20:57
 */
@Data
@ConfigurationProperties(prefix = "rpc.server")
public class RpcServerProperties {
    /**
     * 服务提供方的ip
     */
    private String ip;
    /**
     * 服务提供方的端口
     */
    private Integer port;
    /**
     * 应用名称，如provider1
     */
    private String appName;
    /**
     * 注册中心，如zookeeper和nacos
     */
    private String registry;
    /**
     * 通信方式，如http，socket，netty
     */
    private String communicate;
    /**
     * 注册中心的地址，如localhost:2181
     */
    private String registryIpAndPort;

    /**
     * 默认值初始化
     */
    public RpcServerProperties() {
        port = 8080;
        appName = "provider1";
        registry = "zookeeper";
        communicate = "netty";
        registryIpAndPort = "127.0.0.1:2181";
    }
}
```

### 2.RpcServerAutoConfiguration

在 **springboot **运行时，**SpringFactoriesLoader** 类会去寻找 **spring.factories** 文件，从文件中**读取自动装配的类**

```java
// resources目录下
public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";
```

rpc-server模块的 **spring.factories** 文件

```properties
# Auto Configuration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.mirrors.server.config.RpcServerAutoConfiguration
```

RpcServerAutoConfiguration类

```java
package com.mirrors.server.config;

import com.mirrors.core.registry.ServiceRegistry;
import com.mirrors.core.registry.zookeeper.ZooKeeperServiceRegistry;
import com.mirrors.server.communicate.RpcServer;
import com.mirrors.server.communicate.http.HttpRpcServer;
import com.mirrors.server.communicate.netty.NettyRpcServer;
import com.mirrors.server.communicate.socket.SocketRpcServer;
import com.mirrors.server.spring.RpcServerBeanPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * server端的自动装配类，
 * 使得 @ConfigurationProperties 注解生效，将该类注入到 IOC 容器中,交由 IOC 容器进行管理
 * <pre>
 *     1. ConditionalOnBean：是否存在某个某类或某个名字的Bean
 *     2. ConditionalOnMissingBean：是否缺失某个某类或某个名字的Bean
 *     3. ConditionalOnSingleCandidate：是否符合指定类型的Bean只有⼀个
 *     4. ConditionalOnClass：是否存在某个类
 *     5. ConditionalOnMissingClass：是否缺失某个类
 *     6. ConditionalOnExpression：指定的表达式返回的是true还是false
 *     7. ConditionalOnJava：判断Java版本
 *     8. ConditionalOnJndi：JNDI指定的资源是否存在
 *     9. ConditionalOnWebApplication：当前应⽤是⼀个Web应⽤
 *     10. ConditionalOnNotWebApplication：当前应⽤不是⼀个Web应⽤
 *     11. ConditionalOnProperty：Environment中是否存在某个属性
 *     12. ConditionalOnResource：指定的资源是否存在
 *     13. ConditionalOnWarDeployment：当前项⽬是不是以War包部署的⽅式运⾏
 *     14. ConditionalOnCloudPlatform：是不是在某个云平台上
 * </pre>
 *
 * @author mirrors
 * @version 1.0
 * @Configuration 交给spring管理（有@Component）
 * @date 2023/12/13 21:00
 */
@Configuration
@EnableConfigurationProperties(RpcServerProperties.class)
public class RpcServerAutoConfiguration {

    /**
     * 配置类对象
     */
    @Autowired
    RpcServerProperties properties;

    /**
     * 创建ServiceRegistry实例bean，没有配置时返回zookeeper
     *
     * @return
     */
    @Bean(name = "serviceRegistry") // bean的名称
    @Primary // 有多个相同类型的bean时，使用@Primary来赋予bean更高的优先级
    @ConditionalOnMissingBean // 保证Spring容器中只有一个Bean类型的实例
    @ConditionalOnProperty(prefix = "rpc.server", name = "registry", havingValue = "zookeeper", matchIfMissing = true)
    // @ConditionalOnProperty：根据属性值来控制类或某个方法是否需要加载。它既可以放在类上也可以放在方法上
    public ServiceRegistry zookeeperServiceRegistry() {
        return new ZooKeeperServiceRegistry(properties.getRegistryIpAndPort());
    }

    /**
     * 创建ServiceRegistry实例bean，配置为nacos时
     *
     * @return
     */
    @Bean(name = "serviceRegistry")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "registry", havingValue = "nacos")
    public ServiceRegistry nacosServiceRegistry() {
        return new ZooKeeperServiceRegistry(properties.getRegistryIpAndPort());
    }

    /**
     * 创建通信方式，没有配置时就是用netty
     *
     * @return
     */
    @Bean(name = "rpcServer")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "communicate", havingValue = "netty", matchIfMissing = true)
    public RpcServer nettyRpcServer() {
        //返回netty通信方式
        return new NettyRpcServer();
    }

    /**
     * 创建通信方式，配置http
     *
     * @return
     */
    @Bean(name = "rpcServer")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "communicate", havingValue = "http")
    @ConditionalOnClass(name = {"org.apache.catalina.startup.Tomcat"})
    // @ConditionalOnClass标识在 Bean方法 上，只有只有存在@ConditionalOnClass中value/name配置的类方法才会生效
    public RpcServer httpRpcServer() {
        //返回socket通信方式
        return new SocketRpcServer();
    }

    /**
     * 创建通信方式，配置socket
     *
     * @return
     */
    @Bean(name = "rpcServer")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "communicate", havingValue = "socket")
    @ConditionalOnClass(name = {"org.apache.catalina.startup.Tomcat"})
    public RpcServer socketRpcServer() {
        //返回http通信方式
        return new HttpRpcServer();
    }

    /**
     * 配置这个类（RpcServerBeanPostProcessor使得 被@RpcService标注的类 暴露）
     *
     * @param registry
     * @param rpcServer
     * @param properties
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ServiceRegistry.class, RpcServer.class})
    public RpcServerBeanPostProcessor rpcServerBeanPostProcessor(@Autowired ServiceRegistry registry,
                                                                 @Autowired RpcServer rpcServer,
                                                                 @Autowired RpcServerProperties properties) {
        return new RpcServerBeanPostProcessor(registry, rpcServer, properties);
    }
}
```

## 4.自定义注解

### 1.@RpcService

**标注需要暴露的服务实现类，被标注的类将会被注入到 Spring 容器中，同时将对应服务信息注册到远程注册中心**

```java
package com.mirrors.server.annotation;

import java.lang.annotation.*;

/**
 * RpcService注解：标注该类为服务实现类
 * 提供给 Server端(provider) 使用provider
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 15:29
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RpcService {
    /**
     * 版本号
     *
     * @return
     */
    String version() default "1.0";

    /**
     * 暴露服务的全限定接口名
     *
     * @return
     */
    String interfaceName() default "";

    /**
     * 暴露服务的接口类型
     *
     * @return
     */
    Class<?> interfaceClass() default void.class;
}
```

### 2.@RpcComponentScan

 **扫描被 @RpcService 标注的组件并将对应的 BeanDefiniton 对象注册到Spring**

```java
package com.mirrors.server.annotation;

import com.mirrors.server.spring.RpcBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 自定义 rpc 组件扫描注解（扫描被 RpcService注解 标注的类）
 * 自定义注解式组件扫描的关键逻辑：
 * <p> 1.引入了RpcBeanDefinitionRegistry类，这个类是一个ImportBeanDefinitionRegistrar的实现类 </p>
 * <p> 2.Spring 容器在解析该类型的 Bean 时会调用该实现类中的registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)方法，</p>
 * <p> 3.RpcComponentScan 注解上的信息被提取成 AnnotationMetadata，容器注册器对象 作为此方法的参数 </p>
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 15:36
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(RpcBeanDefinitionRegistry.class) // 引入了RpcBeanDefinitionRegistry类
public @interface RpcComponentScan {

    /**
     * 扫描包路径
     *
     * @return
     */
    String[] basePackages() default {};
}
```

## 5.自定义Spring的Bean处理

### 1.BeanDefinition

BeanDefinition相关：[超详细分析Spring的BeanDefinition - 掘金 (juejin.cn)](https://juejin.cn/post/7202172872369815612) 

​				[Spring（四）核心容器 - BeanDefinition 解析 - 龙四丶 - 博客园 (cnblogs.com)](https://www.cnblogs.com/loongk/p/12262101.html) 

​				[想真正玩懂Spring，先搞定让你眼花缭乱的BeanDefinition吧 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/189896257) 

```java
package com.mirrors.server.spring;

import com.mirrors.server.annotation.RpcComponentScan;
import com.mirrors.server.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

/**
 * 自定义 rpc框架 BeanDefinition 注册器类，
 * 主要用于处理 @RpcComponentScan 注解 和 @RpcService注解，
 * 先找到 @RpcComponentScan注解属性上的 的包位置，再扫描包下 被 @RpcService注解 标注的类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 15:49
 */
@Slf4j
public class RpcBeanDefinitionRegistry implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    /**
     * 接口：统一资源定位器
     */
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 此方法会在 spring 自定义扫描执行之后执行，这个时候 beanDefinitionMap 已经有扫描到的 beanDefinition 对象了
     *
     * @param annotationMetadata
     * @param registry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        // 获取 RpcComponentScan 注解的属性和值
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcComponentScan.class.getName()));
        String[] basePackages = {};
        if (annotationAttributes != null) {
            basePackages = annotationAttributes.getStringArray("basePackages");
        }
        // 如果没有指定包名，设置默认包路径
        if (basePackages.length == 0) {
            StandardAnnotationMetadata metadata = (StandardAnnotationMetadata) annotationMetadata;
            basePackages = new String[]{metadata.getIntrospectedClass().getPackage().getName()};
        }
        // 创建 扫描@RpcService注解 的Scanner
        RpcClassPathBeanDefinitionScanner scanRpcService = new RpcClassPathBeanDefinitionScanner(registry, RpcService.class);
        if (resourceLoader != null) {
            scanRpcService.setResourceLoader(resourceLoader);
        }

        // 扫描包下所有 被RpcService标注的bean，返回注册成功的数量（scan方法会调用register方法，注册扫描到的类，并生成 BeanDefinition 注册到 spring 容器）
        int count = scanRpcService.scan(basePackages);
        log.info("The number of BeanDefinition scanned and annotated by RpcService is {}.", count);
    }
}
```

### 2.包扫描器

参考：[Spring 的类扫描器分析 - ClassPathBeanDefinitionScanner - 简书 (jianshu.com)](https://www.jianshu.com/p/d5ffdccc4f5d) 

​	  [深入理解ClassPathBeanDefinitionScanner - Xianuii - 博客园 (cnblogs.com)](https://www.cnblogs.com/Xianhuii/p/17051837.html) 

```java
package com.mirrors.server.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

/**
 * 自定义 类路径下的包扫描器
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 16:28
 */
public class RpcClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    /**
     * 必须继承了Annotation的注解类型
     */
    private Class<? extends Annotation> annotationType;

    public RpcClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry);
        // 过滤指定的注解类型（过滤器）
        if (annotationType != null) {
            this.addIncludeFilter(new AnnotationTypeFilter(this.annotationType));
        } else {
            this.addIncludeFilter(((metadataReader, metadataReaderFactory) -> true));
        }
    }

    /**
     * 自定义构造函数
     *
     * @param registry
     * @param annotationType
     */
    public RpcClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annotationType) {
        super(registry);
        this.annotationType = annotationType;
        // 过滤指定的注解类型（过滤器）
        if (annotationType != null) {
            this.addIncludeFilter(new AnnotationTypeFilter(annotationType));
        } else {
            this.addIncludeFilter(((metadataReader, metadataReaderFactory) -> true));
        }
    }

    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }
}
```

### 3.注册暴露

参考：[SpringBoot2.x基础篇：使用CommandLineRunner或ApplicationRunner-腾讯云开发者社区-腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/1656704) 

​	  [谈谈Spring中的BeanPostProcessor接口 - 特务依昂 - 博客园 (cnblogs.com)](https://www.cnblogs.com/tuyang1129/p/12866484.html) 

```java
package com.mirrors.server.spring;

import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.registry.ServiceRegistry;
import com.mirrors.core.utils.ServiceUtil;
import com.mirrors.server.annotation.RpcService;
import com.mirrors.server.cache.ServiceLocalCache;
import com.mirrors.server.communicate.RpcServer;
import com.mirrors.server.config.RpcServerProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;

/**
 * 该类用于 spring 容器启动时，将被 @RpcService 标注的服务进行注册并暴露
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 20:48
 */
@Slf4j
public class RpcServerBeanPostProcessor implements BeanPostProcessor, CommandLineRunner {
    /**
     * 自定义的服务注册
     */
    private final ServiceRegistry serviceRegistry;
    /**
     * 通信方式
     */
    private final RpcServer rpcServer;
    /**
     * 服务配置类
     */
    private final RpcServerProperties properties;

    public RpcServerBeanPostProcessor(ServiceRegistry serviceRegistry, RpcServer rpcServer, RpcServerProperties properties) {
        this.serviceRegistry = serviceRegistry;
        this.rpcServer = rpcServer;
        this.properties = properties;
    }

    /**
     * 在应用启动后执行，并且在整个应用生命周期只搞一次
     *
     * @param args 命令行参数
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        // 开启一个线程进行，让服务端RpcServer开始通信！
        new Thread(() -> rpcServer.start(properties.getPort())).start();
        // jvm中的关闭钩子，在jvm关闭的时候（将服务从注册中心移除）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serviceRegistry.disconnect();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    /**
     * 重写：在bean初始化后，（遍历所有bean）检测标注有 @RpcService 注解的类，将对应的服务类进行注册，对外暴露服务，同时进行本地服务注册
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @SneakyThrows
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 判断当前bean是否被 @RpcService 注解
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with [{}].", bean.getClass().getName(), RpcService.class.getCanonicalName());
            // 获取@RpcService注解，并且从注解中获取 服务接口名，版本号
            RpcService rpcServiceAnnotation = bean.getClass().getAnnotation(RpcService.class);
            String interfaceName;
            if ("".equals(rpcServiceAnnotation.interfaceName())) {
                interfaceName = rpcServiceAnnotation.interfaceClass().getName();
            } else {
                interfaceName = rpcServiceAnnotation.interfaceName();
            }
            String version = rpcServiceAnnotation.version();
            // 构建服务名称
            String serviceKey = ServiceUtil.getServiceKey(interfaceName, version);
            // 构建 ServiceInfo对象
            ServiceInfo serviceInfo = ServiceInfo.builder()
                    .appName(properties.getAppName())
                    .serviceName(serviceKey)
                    .version(version)
                    .ip(properties.getIp())
                    .port(properties.getPort())
                    .build();
            // 远程服务注册
            serviceRegistry.register(serviceInfo);
            // 缓存 服务注册 到本地
            ServiceLocalCache.addService(serviceKey, bean);
        }
        return bean;
    }
}
```

## 6.连接通信

### 1.共同接口

```java
package com.mirrors.server.communicate;

/**
 * rpc服务类， 接受客户端信息，调用客户端请求的方法，返回结果给客户端
 * 具体由子类实现
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 20:54
 */
public interface RpcServer {

    /**
     * 开启服务
     *
     * @param port
     */
    void start(Integer port);
}
```

### 2.netty

#### 1.消息处理器

客户端每次请求都要重新与服务端建立 netty 连接，非常耗时，增加心跳检查机制，保持长连接，复用 channel 连接；

- 长连接：避免了每次调用新建TCP连接，提高了调用的响应速度；
- Channel 连接复用：避免重复连接服务端；
- 多路复用：单个TCP连接可交替传输多个请求和响应的消息，降低了连接的等待闲置时间，从而减少了同样并发数下的网络连接数，提高了系统吞吐量。

参考：[Netty中入站处理器（SimpleChannelInboundHandler 和 ChannelInboundHandlerAdapter）_netty5 channelread0-CSDN博客](https://blog.csdn.net/weixin_42030357/article/details/111173091) 

​	  [netty 中的心跳检测机制_netty usereventtriggered-CSDN博客](https://blog.csdn.net/qq_38887189/article/details/127262185) 

​	  [Netty——ChannelHandlerContext_channelhandlercontext.channel()-CSDN博客](https://blog.csdn.net/cold___play/article/details/106760348) 

​	  [Netty-源码分析ChannelFutureListener添加异步回调事件-CSDN博客](https://blog.csdn.net/nimasike/article/details/95338414) 

```java
package com.mirrors.server.communicate.netty;

import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.bean.RpcMessageHeader;
import com.mirrors.core.constants.RpcConstant;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.enums.MessageStatus;
import com.mirrors.core.enums.MessageType;
import com.mirrors.core.factory.SingletonFactory;
import com.mirrors.server.handler.RpcRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于 netty 的请求消息入站处理器
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 15:29
 */
@Slf4j
public class NettyRpcRequestHandler extends SimpleChannelInboundHandler {

    /**
     * 线程池，数组阻塞队列
     */
    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));
    /**
     * 反射调用服务方法
     */
    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcRequestHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }


    /**
     * 当收到对方发来的数据后，就会触发，参数o 就是发来的信息，
     * 利用线程池处理每个任务，不阻塞！
     *
     * @param channelHandlerContext
     * @param o
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        // 转为RpcMessage
        RpcMessage request = (RpcMessage) o;
        // 线程池，不阻塞执行
        threadPool.submit(() -> {
            try {
                RpcMessage response = new RpcMessage();
                RpcMessageHeader header = request.getRpcMessageHeader();
                MessageType messageType = MessageType.getByType(header.getMessageType());
                log.debug("The message received by the server is: {}", request.getRpcMessageBody());
                // 判断消息类型
                if (messageType == MessageType.HEARTBEAT_REQUEST) {
                    //（1）如果是心跳检测请求
                    header.setMessageType(MessageType.HEARTBEAT_RESPONSE.getType());
                    header.setMessageStatus(MessageStatus.SUCCESS.getType());
                    // 设置响应头信息
                    response.setRpcMessageHeader(header);
                    response.setRpcMessageBody(RpcConstant.PONG);
                } else {
                    //（2）不是心跳信息，rpc信息处理
                    RpcRequest rpcRequest = (RpcRequest) request.getRpcMessageBody();
                    RpcResponse rpcResponse = new RpcResponse();
                    // 设置信息类型
                    header.setMessageType(MessageType.RESPONSE.getType());
                    // 反射调用
                    try {
                        Object result = rpcRequestHandler.invokeService(rpcRequest);
                        rpcResponse.setReturnValue(result);
                        header.setMessageStatus(MessageStatus.SUCCESS.getType());
                    } catch (Exception e) {
                        log.error("The service [{}], the method [{}] invoke failed!", rpcRequest.getServiceName(), rpcRequest.getMethodName());
                        // 压缩错误信息，不然堆栈信息过多，导致报错
                        rpcResponse.setExceptionValue(new RuntimeException("Error in remote procedure call, " + e.getMessage()));
                        header.setMessageStatus(MessageStatus.FAIL.getType());
                    }
                    // 设置响应信息
                    response.setRpcMessageHeader(header);
                    response.setRpcMessageBody(rpcResponse);
                }
                log.debug("response: {}.", response);
                // 传递下一个处理器，如果写响应失败，将通道关闭
                channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } finally {
                // 释放资源
                ReferenceCountUtil.release(o);
            }
        });
    }

    /**
     * 当服务端 设定时间内 没有接收到客户端发送的读事件，就触发 userEventTriggered 方法。
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            // 如果触发的空闲事件是 读空闲，关闭Channel
            if (state == IdleState.READER_IDLE) {
                log.warn("idle check happen");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 捕获到异常时
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
```

#### 2.基于netty连接

参考：[Netty教程-ServerBootstrap-CSDN博客](https://blog.csdn.net/woaixiaopangniu521/article/details/70256018) 

​	  [揭秘ServerBootstrap神秘面纱（服务端ServerBootstrap）-CSDN博客](https://blog.csdn.net/moneywenxue/article/details/116246462) 

```java
package com.mirrors.server.communicate.netty;

import com.mirrors.core.codec.RpcFrameDecoder;
import com.mirrors.core.codec.SharableRpcMessageCodec;
import com.mirrors.server.communicate.RpcServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 16:24
 */
@Slf4j
public class NettyRpcServer implements RpcServer {
    @Override
    public void start(Integer port) {
        // boss 处理 accept事件，worker 处理读写事件
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();

        try {
            // 获取到本机ip地址
            InetAddress localHost = InetAddress.getLocalHost();
            // 配置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据块，减少网络传输
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开始 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 日志记录
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    // 添加处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // 超过 30s 内如果没有从客户端读数据，触发 读空闲
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            // 添加 粘包拆包 解码器
                            pipeline.addLast(new RpcFrameDecoder());
                            // 添加 编解码器
                            pipeline.addLast(new SharableRpcMessageCodec());
                            // 添加 rpc请求消息处理器
                            pipeline.addLast(new NettyRpcRequestHandler());
                        }
                    });
            // 绑定端口，同步等到绑定成功
            ChannelFuture channelFuture = serverBootstrap.bind(localHost, port).sync();
            log.debug("Rpc server add {} started on the port {}.", localHost, port);
            // 同步等待 服务端 监听 端口关闭，当channel没有断开时，线程阻塞，直到断开为止
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            log.error("Error occurred while starting the rpc service.", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
```

### 3.socket

#### 1.消息处理器

```java
package com.mirrors.server.communicate.socket;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.factory.SingletonFactory;
import com.mirrors.server.handler.RpcRequestHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 基于 socket 的请求消息处理器，实现Runnable接口，可以多线程
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 20:06
 */
@Slf4j
public class SocketRpcRequestHandler implements Runnable {

    private final Socket socket;
    /**
     * 反射调用方法
     */
    private final RpcRequestHandler rpcRequestHandler;

    public SocketRpcRequestHandler(Socket socket) {
        this.socket = socket;
        rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    /**
     * 注意 客户端 发送的是 RpcRequest，所以服务端发送RpcResponse，
     * 无需消息协议
     */
    @Override
    public void run() {
        // 得到socket输入流，try自动关闭
        try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {
            // 得到socket输出流
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            // 直接强转，不需要编解码和消息协议
            RpcRequest rpcRequest = (RpcRequest) inputStream.readObject();
            log.debug("The socket server received message is {}.", rpcRequest);
            // 返回RpcResponse
            RpcResponse rpcResponse = new RpcResponse();
            // 处理请求信息，调用 服务的本地缓存，返回结果
            try {
                Object result = rpcRequestHandler.invokeService(rpcRequest);
                rpcResponse.setReturnValue(result);
            } catch (Exception e) {
                log.error("The service [{}], the method [{}] invoke failed!", rpcRequest.getServiceName(), rpcRequest.getMethodName());
                // 压缩错误信息，不然堆栈信息过多，导致报错
                rpcResponse.setExceptionValue(new RuntimeException("Error in remote procedure call, " + e.getMessage()));
            }
            log.debug("response: {}.", rpcResponse);
            outputStream.writeObject(rpcResponse);

        } catch (Exception e) {
            throw new RuntimeException("The socket server failed to handle rpc request", e);
        }
    }
}
```

#### 2.socket通信

参考：[CPU 密集型 和 IO密集型 的区别，如何确定线程池大小？-腾讯云开发者社区-腾讯云 (tencent.com)](https://cloud.tencent.com/developer/article/1806245) 

```java
package com.mirrors.server.communicate.socket;

import com.mirrors.server.communicate.RpcServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于socket通信
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 20:21
 */
@Slf4j
public class SocketRpcServer implements RpcServer {

    /**
     * 当前可用处理器数量（逻辑数量， 8 内核？）
     */
    private final int cpuNum = Runtime.getRuntime().availableProcessors();

    /**
     * 线程池的线程数大小，看我们执行的任务是cpu密集型，还是io密集型；
     * 如果是计算，cpu密集型，线程大小应该设置为：cpuNum + 1；
     * 如果是网络传输，数据库等，io密集型，cpuNum * 2
     */
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(cpuNum * 2, cpuNum * 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));

    @Override
    public void start(Integer port) {
        // 建立ServerSocket连接，try自动关闭
        try (ServerSocket serverSocket = new ServerSocket()) {
            // 获取本地ip
            String localhost = InetAddress.getLocalHost().getHostAddress();
            // ServerSocket绑定端口
            serverSocket.bind(new InetSocketAddress(localhost, port));
            // ServerSocket循环接受socket连接，socket为空时，没有连接
            Socket socket;
            while ((socket = serverSocket.accept()) != null) {
                log.debug("The client connected [{}].", socket.getInetAddress());
                // 线程池异步执行 信息处理
                threadPool.execute(new SocketRpcRequestHandler(socket));
            }
            // 服务端口连接断开，关闭线程池
            threadPool.shutdown();

        } catch (Exception e) {
            throw new RuntimeException(String.format("The socket server failed to start on port %d.", port), e);
        }
    }
}
```

### 4.http

参考：[嵌入式 Tomcat (Embedded Tomcat) - develon - 博客园 (cnblogs.com)](https://www.cnblogs.com/develon/p/11602969.html) 

#### 1.消息处理器

```java
package com.mirrors.server.communicate.http;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.factory.SingletonFactory;
import com.mirrors.server.handler.RpcRequestHandler;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 基于http连接的消息处理器
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 21:14
 */
@Slf4j
public class HttpRpcRequestHandler {

    /**
     * 反射调用 服务的本地缓存，返回结果
     */
    private final RpcRequestHandler rpcRequestHandler;

    public HttpRpcRequestHandler() {
        rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    /**
     * 处理 http请求信息
     *
     * @param request
     * @param response
     */
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        try {
            // 拿到 request和response 的输入输出流
            ObjectInputStream inputStream = new ObjectInputStream(request.getInputStream());
            ObjectOutputStream outputStream = new ObjectOutputStream(response.getOutputStream());
            // 输入流读取客户端请求，不需要编解码和自定义信息协议
            RpcRequest rpcRequest = (RpcRequest) inputStream.readObject();
            log.debug("The http server received message is {}.", rpcRequest);
            // 响应RpcResponse
            RpcResponse rpcResponse = new RpcResponse();
            try {
                // 反射调用方法
                Object result = rpcRequestHandler.invokeService(rpcRequest);
                rpcResponse.setReturnValue(result);

            } catch (Exception e) {
                log.error("The service [{}], the method [{}] invoke failed!", rpcRequest.getServiceName(), rpcRequest.getMethodName());
                // 压缩错误信息，不然堆栈信息过多，导致报错
                rpcResponse.setExceptionValue(new RuntimeException("Error in remote procedure call, " + e.getMessage()));
            }
            // 输出流
            log.debug("response: {}.", rpcResponse);
            outputStream.writeObject(rpcResponse);

        } catch (Exception e) {
            throw new RuntimeException("The http server failed to handle rpc request", e);
        }
    }
}
```

#### 2.处理http请求

参考：[JavaWeb 之 继承 HttpServlet 实现 Servlet - 格物致知_Tony - 博客园 (cnblogs.com)](https://www.cnblogs.com/niujifei/p/15107560.html) 

```java
package com.mirrors.server.communicate.http;

import com.mirrors.core.factory.SingletonFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 用于接受http请求
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 21:05
 */
public class DispatcherServlet extends HttpServlet {

    /**
     * 当前可用处理器数量（逻辑数量， 8 内核？）
     */
    private final int cpuNum = Runtime.getRuntime().availableProcessors();

    /**
     * 线程池的线程数大小，看我们执行的任务是cpu密集型，还是io密集型；
     * 如果是计算，cpu密集型，线程大小应该设置为：cpuNum + 1；
     * 如果是网络传输，数据库等，io密集型，cpuNum * 2
     */
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(cpuNum * 2, cpuNum * 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));

    /**
     * service方法，当客户端进行访问服务端时就执行
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 创建 HttpRpcRequestHandler 的单例
        HttpRpcRequestHandler httpRpcRequestHandler = SingletonFactory.getInstance(HttpRpcRequestHandler.class);
        // 接受到http请求就 使用线程池调用 消息处理器
        threadPool.submit(new Thread(() -> httpRpcRequestHandler.handle(request, response)));
    }
}
```

#### 4.基于tomcat建立连接

```java
package com.mirrors.server.communicate.http;

import com.mirrors.server.communicate.RpcServer;
import org.apache.catalina.Context;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;

import java.net.InetAddress;

/**
 * 基于 http 通信
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 22:00
 */
public class HttpRpcServer implements RpcServer {

    /**
     * 创建一个简单的Tomcat服务器，并配置了一个简单的Web应用
     *
     * @param port
     */
    @Override
    public void start(Integer port) {
        try {
            // 创建Tomcat实例
            Tomcat tomcat = new Tomcat();
            // 获取Tomcat的服务器实例
            Server server = tomcat.getServer();
            // 获取服务器中Tomcat的服务
            Service service = server.findService("Tomcat");

            // 获取本机的IP地址作为主机名
            String localhost = InetAddress.getLocalHost().getHostAddress();
            // 创建Connector实例，处理网络连接
            Connector connector = new Connector();
            connector.setPort(port);

            // 创建Tomcat的引擎
            StandardEngine engine = new StandardEngine();
            engine.setDefaultHost(localhost);
            // 创建Tomcat的主机
            StandardHost host = new StandardHost();
            host.setName(localhost);

            // 设置Context的路径
            String contextPath = "";
            // 创建Web应用的上下文
            Context context = new StandardContext();
            context.setPath(contextPath);
            // 添加生命周期监听器
            context.addLifecycleListener(new Tomcat.FixContextListener());

            // 将Context添加到Host中
            host.addChild(context);
            // 将Host添加到Engine中
            engine.addChild(host);

            // 将Engine设置到Service中
            service.setContainer(engine);
            // 将Connector添加到Service中
            service.addConnector(connector);

            // 向Tomcat添加Servlet实例，如自定义DispatcherServlet
            tomcat.addServlet(contextPath, "dispatcher", new DispatcherServlet());
            // 将Servlet映射到路径"/*"
            context.addServletMappingDecoded("/*", "dispatcher");

            // 启动Tomcat
            tomcat.start();
            // 等待Tomcat服务器的关闭
            tomcat.getServer().await();

        } catch (Exception e) {
            throw new RuntimeException(String.format("The http server failed to start on port %d.", port), e);
        }

    }
}
```

# 4.rpc-client客户端模块

## 1.pom文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!--父项目-->
    <parent>
        <artifactId>rpc-mirrors</artifactId>
        <groupId>com.mirrors</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>rpc-client</artifactId>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <encoding>UTF-8</encoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>cglib</groupId>
            <artifactId>cglib</artifactId>
            <version>${cglib.version}</version>
        </dependency>
        <!--引入 rpc-core 的jar包-->
        <dependency>
            <groupId>com.mirrors</groupId>
            <artifactId>rpc-core</artifactId>
            <version>1.0-SNAPSHOT</version>
            <optional>true</optional>
            <scope>compile</scope>
        </dependency>
        <!--注解解析器-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

## 2.辅助类

### 1.元数据bean

```java
package com.mirrors.client.bean;

import com.mirrors.core.bean.RpcMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求的元数据类，远程调用时并非将此类直接发送；
 * 而是发送 RpcMessage属性 或 RpcMessage属性的一部分；
 * 其他属性是传递给 RpcClient 方便处理
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 9:27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetadata {
    /**
     * 发送的消息（消息头+消息正文）
     */
    private RpcMessage rpcMessage;

    /**
     * 远程服务提供的地址
     */
    private String serverIp;

    /**
     * 远程服务提供的端口
     */
    private Integer serverPort;

    /**
     * 调用超时时间
     */
    private Integer timeout;
}
```

### 2.远程调用handler

```java
package com.mirrors.client.handler;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.client.communicate.RpcClient;
import com.mirrors.client.config.RpcClientProperties;
import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.bean.RpcMessageHeader;
import com.mirrors.core.discover.ServiceDiscover;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.dto.ServiceInfo;

import java.lang.reflect.Method;

/**
 * 远程调用的工具类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 15:07
 */
public class RemoteCall {

    /**
     * 发起远程调用
     *
     * @param serviceDiscover 服务发现
     * @param rpcClient       客户端
     * @param serviceName     服务名称
     * @param properties      配置属性
     * @param method          调用方法名
     * @param args            方法参数
     * @return 返回方法调用结果
     */
    public static Object remoteCall(ServiceDiscover serviceDiscover, RpcClient rpcClient, String serviceName, RpcClientProperties properties, Method method, Object[] args) {
        // 构建请求头
        RpcMessageHeader header = RpcMessageHeader.createBySerializer(properties.getSerialization());
        // 构建请求体（RpcRequest作为请求体）
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceName(serviceName);
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParamTypes(method.getParameterTypes());
        rpcRequest.setParamValues(args);

        // 服务发现，从服务列表负载均衡选择一个返回
        ServiceInfo serviceInfo = serviceDiscover.discover(rpcRequest);
        if (serviceInfo == null) {
            throw new RuntimeException(String.format("The service [%s] was not found in the remote registry center", serviceName));
        }

        // 构建Rpc发送的信息
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setRpcMessageHeader(header);
        rpcMessage.setRpcMessageBody(rpcRequest);

        // 构建请求元数据
        RequestMetadata metadata = RequestMetadata.builder()
                .rpcMessage(rpcMessage)
                .serverIp(serviceInfo.getIp())
                .serverPort(serviceInfo.getPort())
                .timeout(properties.getTimeout())
                .build();

        // 发送网络请求，返回结果
        RpcMessage responseRpcMessage = rpcClient.sendRpcRequest(metadata);
        if (responseRpcMessage == null) {
            throw new RuntimeException("Remote procedure call timeout");
        }

        // 拿到 消息正文
        RpcResponse response = (RpcResponse) responseRpcMessage.getRpcMessageBody();
        if (response.getExceptionValue() != null) {
            // 远程调用 发生错误
            throw new RuntimeException(response.getExceptionValue());
        }

        // 返回 远程调用响应结果
        return response.getReturnValue();
    }
}
```



## 3.自定义配置类

### 1.RpcClientProperties

```Java
package com.mirrors.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 客户端的配置属性类，自动读取文件注入
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 9:33
 */
@Data
@ConfigurationProperties(prefix = "rpc.client")
public class RpcClientProperties {

    /**
     * 负载均衡算法
     */
    private String loadBalance;

    /**
     * 序列化算法
     */
    private String serialization;

    /**
     * 通信方式
     */
    private String communicate;

    /**
     * 注册中心
     */
    private String registry;

    /**
     * 注册中心的ip和端口号
     */
    private String registryIpAndPort;

    /**
     * 连接超时时间
     */
    private Integer timeout;

    /**
     * 无参默认初始化
     */
    public RpcClientProperties() {
        this.loadBalance = "random";
        this.serialization = "hessian";
        this.communicate = "netty";
        this.registry = "zookeeper";
        this.registryIpAndPort = "127.0.0.1:2181";
        this.timeout = 5000;
    }
}
```

### 2.RpcClientAutoConfiguration

在 **springboot **运行时，**SpringFactoriesLoader** 类会去寻找 **spring.factories** 文件，从文件中**读取自动装配的类**

```java
// resources目录下
public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";
```

rpc-server模块的 **spring.factories** 文件

```properties
# Auto Configuration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.mirrors.client.config.RpcClientAutoConfiguration
```

RpcClientAutoConfiguration类

```java
package com.mirrors.client.config;

import com.mirrors.client.communicate.RpcClient;
import com.mirrors.client.communicate.http.HttpRpcClient;
import com.mirrors.client.communicate.netty.NettyRpcClient;
import com.mirrors.client.communicate.socket.SocketRpcClient;
import com.mirrors.client.proxy.ClientProxyFactory;
import com.mirrors.client.spring.RpcClientBeanPostProcessor;
import com.mirrors.client.spring.RpcClientExitDisposableBean;
import com.mirrors.core.discover.ServiceDiscover;
import com.mirrors.core.discover.nacos.NacosServiceDiscover;
import com.mirrors.core.discover.zookeeper.ZooKeeperServiceDiscover;
import com.mirrors.core.loadbalance.LoadBalance;
import com.mirrors.core.loadbalance.impl.ConsistentHashLoadBalance;
import com.mirrors.core.loadbalance.impl.RandomLoadBalance;
import com.mirrors.core.loadbalance.impl.RoundRobinLoadBalance;
import com.mirrors.core.registry.zookeeper.ZooKeeperServiceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 客户端自动装配类
 * <pre>
 *     1. ConditionalOnBean：是否存在某个某类或某个名字的Bean
 *     2. ConditionalOnMissingBean：是否缺失某个某类或某个名字的Bean
 *     3. ConditionalOnSingleCandidate：是否符合指定类型的Bean只有⼀个
 *     4. ConditionalOnClass：是否存在某个类
 *     5. ConditionalOnMissingClass：是否缺失某个类
 *     6. ConditionalOnExpression：指定的表达式返回的是true还是false
 *     7. ConditionalOnJava：判断Java版本
 *     8. ConditionalOnJndi：JNDI指定的资源是否存在
 *     9. ConditionalOnWebApplication：当前应⽤是⼀个Web应⽤
 *     10. ConditionalOnNotWebApplication：当前应⽤不是⼀个Web应⽤
 *     11. ConditionalOnProperty：Environment中是否存在某个属性
 *     12. ConditionalOnResource：指定的资源是否存在
 *     13. ConditionalOnWarDeployment：当前项⽬是不是以War包部署的⽅式运⾏
 *     14. ConditionalOnCloudPlatform：是不是在某个云平台上
 * </pre>
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 9:41
 */
@Configuration
@EnableConfigurationProperties(RpcClientProperties.class)
public class RpcClientAutoConfiguration {

    /**
     * 配置属性类对象
     */
    @Autowired
    RpcClientProperties properties;

    /**
     * 配置负载均衡算法，当不指定属性值，则值默认为当前创建的类（随机策略）
     *
     * @return
     */
    @Bean(name = "loadBalance")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "loadBalance", havingValue = "random", matchIfMissing = true)
    public LoadBalance randomLoadBalance() {
        return new RandomLoadBalance();
    }

    /**
     * 配置负载均衡算法（轮询策略）
     *
     * @return
     */
    @Bean(name = "loadBalance")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "loadBalance", havingValue = "roundRobin")
    public LoadBalance roundRobinLoadBalance() {
        return new RoundRobinLoadBalance();
    }

    /**
     * 配置负载均衡算法（一致性哈希策略）
     *
     * @return
     */
    @Bean(name = "loadBalance")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "loadBalance", havingValue = "consistentHash")
    public LoadBalance consistentHashLoadBalance() {
        return new ConsistentHashLoadBalance();
    }

    /**
     * 配置服务发现（zookeeper）
     *
     * @return
     */
    @Bean(name = "serviceDiscover")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnBean(LoadBalance.class)
    @ConditionalOnProperty(prefix = "rpc.client", name = "discover", havingValue = "zookeeper")
    public ServiceDiscover zookeeperServiceDiscover(@Autowired LoadBalance loadBalance) {
        return new ZooKeeperServiceDiscover(properties.getRegistryIpAndPort(), loadBalance);
    }

    /**
     * 配置服务发现（nacos）
     *
     * @return
     */
    @Bean(name = "serviceDiscover")
    @ConditionalOnMissingBean
    @ConditionalOnBean(LoadBalance.class)
    @ConditionalOnProperty(prefix = "rpc.client", name = "discover", havingValue = "nacos")
    public ServiceDiscover nacosServiceDiscover(@Autowired LoadBalance loadBalance) {
        return new NacosServiceDiscover(properties.getRegistryIpAndPort(), loadBalance);
    }

    /**
     * 配置客户端通信连接（基于netty）
     *
     * @return
     */
    @Bean(name = "rpcClient")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "communicate", havingValue = "netty", matchIfMissing = true)
    public RpcClient nettyRpcClient() {
        // 返回netty客户端
        return new NettyRpcClient();
    }

    /**
     * 配置客户端通信连接（基于http）
     *
     * @return
     */
    @Bean(name = "rpcClient")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "communicate", havingValue = "http")
    public RpcClient httpRpcClient() {
        // 返回http客户端
        return new HttpRpcClient();
    }

    /**
     * 配置客户端通信连接（基于socket）
     *
     * @return
     */
    @Bean(name = "rpcClient")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "communicate", havingValue = "socket")
    public RpcClient socketRpcClient() {
        // 返回socket客户端
        return new SocketRpcClient();
    }

    /**
     * 创建并配置 代理对象
     *
     * @param serviceDiscover
     * @param rpcClient
     * @param rpcClientProperties
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ServiceDiscover.class, RpcClient.class})
    public ClientProxyFactory clientProxyFactory(@Autowired ServiceDiscover serviceDiscover,
                                                 @Autowired RpcClient rpcClient,
                                                 @Autowired RpcClientProperties rpcClientProperties) {
        return new ClientProxyFactory(serviceDiscover, rpcClient, rpcClientProperties);
    }

    /**
     * 创建配置 bean后置处理器
     *
     * @param clientProxyFactory
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RpcClientBeanPostProcessor rpcClientBeanPostProcessor(@Autowired ClientProxyFactory clientProxyFactory) {
        return new RpcClientBeanPostProcessor(clientProxyFactory);
    }

    /**
     * 创建配置 客户端退后的额外操作类
     *
     * @param serviceDiscover
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RpcClientExitDisposableBean rpcClientExitDisposableBean(@Autowired ServiceDiscover serviceDiscover) {
        return new RpcClientExitDisposableBean(serviceDiscover);
    }
}
```

## 4.自定义注解

### 1.RpcReference

**服务注入注解，被标注的属性将自动注入服务的实现类（基于动态代理实现）**

```java
package com.mirrors.client.annotation;

import java.lang.annotation.*;

/**
 * 自动注入对应的实现类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 9:13
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RpcReference {
    /**
     * 接口类型
     *
     * @return
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 全限定接口名
     *
     * @return
     */
    String interfaceName() default "";

    /**
     * 版本号，默认1.0
     *
     * @return
     */
    String version() default "1.0";

    /**
     * 负载均衡策略
     *
     * @return
     */
    String loadBalance() default "";

    /**
     * 服务调用超时时间
     *
     * @return
     */
    int timeout() default 0;
}
```

## 5.动态代理

RPC 框架怎么做到像调用本地接口一样调用远端服务呢？这必须依赖动态代理来实现。**需要创建一个代理对象，在代理对象中完成数据报文编码，然后发起调用发送数据给服务提供方，以此屏蔽 RPC 框架的调用细节**。因为代理类是在运行时生成的，所以代理类的生成速度、生成的字节码大小都会影响 RPC 框架整体的性能和资源消耗，所以需要慎重选择动态代理的实现方案。动态代理比较主流的实现方案有以下几种：JDK 动态代理、Cglib、Javassist、ASM、Byte Buddy。

- **JDK 动态代理：**在运行时可以**动态创建**代理类，但是 JDK 动态代理的功能比较局限，**代理对象必须实现一个接口**，否则抛出异常。因为代理类会继承 Proxy 类，然而 Java 是不支持多重继承的，只能通过接口实现多态。JDK 动态代理所生成的代理类是接口的实现类，不能代理接口中不存在的方法。JDK 动态代理是通过反射调用的形式代理类中的方法，比直接调用肯定是性能要慢的。
- **Cglib 动态代理：**Cglib 是**基于 ASM 字节码生成框架实现的**，通过字节码技术生成的代理类，所以**代理类的类型是不受限制的**。而且 Cglib 生成的代理类是继承于被代理类，所以可以提供更加灵活的功能。在代理方法方面，Cglib 是有优势的，它**采用了 FastClass 机制，为代理类和被代理类各自创建一个 Class，这个 Class 会为代理类和被代理类的方法分配 index 索引，FastClass 就可以通过 index 直接定位要调用的方法，并直接调用，这是一种空间换时间的优化思路**。
- **Javassist 和 ASM：**二者都是 Java 字节码操作框架，使用起来难度较大，需要开发者对 Class 文件结构以及 JVM 都有所了解，但是它们都比反射的性能要高。Byte Buddy 也是一个字节码生成和操作的类库，Byte Buddy 功能强大，相比于 Javassist 和 ASM，Byte Buddy 提供了更加便捷的 API，用于创建和修改 Java 类，无须理解字节码的格式，而且 Byte Buddy 更加轻量，性能更好。

参考：[JDK动态代理和CGLIB动态代理 - 掘金 (juejin.cn)](https://juejin.cn/post/7011357346018361375) 

### 1.Jdk

```java
package com.mirrors.client.proxy;

import com.mirrors.client.communicate.RpcClient;
import com.mirrors.client.config.RpcClientProperties;
import com.mirrors.client.handler.RemoteCall;
import com.mirrors.core.discover.ServiceDiscover;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 基于 JDK 动态代理
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 10:50
 */
public class JdkClientInvocationHandler implements InvocationHandler {

    /**
     * 服务发现中心
     */
    private final ServiceDiscover serviceDiscover;

    /**
     * rpc客户端，有不同的通信方式实现
     */
    private final RpcClient rpcClient;

    /**
     * rpc 客户端配置属性
     */
    private final RpcClientProperties properties;

    /**
     * 服务名称：接口-版本
     */
    private final String serviceName;

    public JdkClientInvocationHandler(ServiceDiscover serviceDiscover, RpcClient rpcClient, RpcClientProperties properties, String serviceName) {
        this.serviceDiscover = serviceDiscover;
        this.rpcClient = rpcClient;
        this.properties = properties;
        this.serviceName = serviceName;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 远程调用
        return RemoteCall.remoteCall(serviceDiscover, rpcClient, serviceName, properties, method, args);
    }
}
```

###2.Cglib

```java
package com.mirrors.client.proxy;

import com.mirrors.client.communicate.RpcClient;
import com.mirrors.client.config.RpcClientProperties;
import com.mirrors.client.handler.RemoteCall;
import com.mirrors.core.discover.ServiceDiscover;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * 基于 Cglib 动态代理
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 10:51
 */
public class CglibClientMethodInterceptor implements MethodInterceptor {

    /**
     * 服务发现中心
     */
    private final ServiceDiscover serviceDiscover;

    /**
     * rpc客户端，有不同的通信方式实现
     */
    private final RpcClient rpcClient;

    /**
     * rpc 客户端配置属性
     */
    private final RpcClientProperties properties;

    /**
     * 服务名称：接口-版本
     */
    private final String serviceName;

    public CglibClientMethodInterceptor(ServiceDiscover serviceDiscover, RpcClient rpcClient, RpcClientProperties properties, String serviceName) {
        this.serviceDiscover = serviceDiscover;
        this.rpcClient = rpcClient;
        this.properties = properties;
        this.serviceName = serviceName;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        // 远程调用
        return RemoteCall.remoteCall(serviceDiscover, rpcClient, serviceName, properties, method, args);
    }
}
```

### 3.代理工厂类

```java
package com.mirrors.client.proxy;

import com.mirrors.client.communicate.RpcClient;
import com.mirrors.client.config.RpcClientProperties;
import com.mirrors.core.discover.ServiceDiscover;
import com.mirrors.core.utils.ServiceUtil;
import net.sf.cglib.proxy.Enhancer;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端代理工厂类，返回服务代理类，可采用Cglib和Jdk两个不同方式代理
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 11:00
 */
public class ClientProxyFactory {

    /**
     * 服务发现中心
     */
    private final ServiceDiscover serviceDiscover;

    /**
     * Rpc客户端，有不同的通信方式实现
     */
    private final RpcClient rpcClient;

    /**
     * Rpc 客户端配置属性
     */
    private final RpcClientProperties properties;

    public ClientProxyFactory(ServiceDiscover serviceDiscover, RpcClient rpcClient, RpcClientProperties properties) {
        this.serviceDiscover = serviceDiscover;
        this.rpcClient = rpcClient;
        this.properties = properties;
    }

    /**
     * 缓存代理对象
     */
    private static final Map<String, Object> proxyCacheMap = new ConcurrentHashMap<>();

    /**
     * 获取代理对象
     *
     * @param clazz
     * @param version
     * @param <T>
     * @return
     */
    public <T> T getProxy(Class<T> clazz, String version) {
        // 获取服务名称
        String serviceKey1 = ServiceUtil.getServiceKey(clazz.getName(), version);
        // computeIfAbsent：对 hashMap 中指定 key 的值进行重新计算，如果不存在这个 key，则添加到 hashMap 中
        Object proxy = proxyCacheMap.computeIfAbsent(serviceKey1, serviceKey2 -> {
            if (clazz.isInterface() || Proxy.isProxyClass(clazz)) {
                // 如果目标类是一个接口或者 是 java.lang.reflect.Proxy 的子类 则默认使用 JDK 动态代理
                return Proxy.newProxyInstance(
                        clazz.getClassLoader(),
                        new Class[]{clazz},
                        new JdkClientInvocationHandler(serviceDiscover, rpcClient, properties, serviceKey2)
                );
            } else {
                // 否则使用Cglib
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
}
```

##6.自定义Spring的Bean处理

###1.客户端退出后的额外操作

```java
package com.mirrors.client.spring;

import com.mirrors.core.discover.ServiceDiscover;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

/**
 * 实现DisposableBean接口，在客户端推出后，释放资源
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 16:07
 */
@Slf4j
public class RpcClientExitDisposableBean implements DisposableBean {

    /**
     * 服务发现
     */
    private final ServiceDiscover serviceDiscover;

    public RpcClientExitDisposableBean(ServiceDiscover serviceDiscover) {
        this.serviceDiscover = serviceDiscover;
    }

    /**
     * 客户端退出后的操作
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        try {
            if (serviceDiscover != null) {
                serviceDiscover.disconnect();
            }
            log.info("Rpc client resource released and exited successfully.");
        } catch (Exception e) {
            log.error("An exception occurred while destroying: {}.", e.getMessage());
        }
    }
}
```

###2.扫描注解替换代理对象

```java
package com.mirrors.client.spring;

import com.mirrors.client.annotation.RpcReference;
import com.mirrors.client.proxy.ClientProxyFactory;
import com.mirrors.core.discover.ServiceDiscover;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

/**
 * 客户端 Bean 后置处理器，扫描创建的 bean 中有被 @RpcReference注解 标注的属性，获取对应的代理对象并进行替换
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 16:09
 */
@Slf4j
public class RpcClientBeanPostProcessor implements BeanPostProcessor {

    /**
     * 代理工厂
     */
    private final ClientProxyFactory clientProxyFactory;

    public RpcClientBeanPostProcessor(ClientProxyFactory clientProxyFactory) {
        this.clientProxyFactory = clientProxyFactory;
    }

    /**
     * 在 bean 实例化完后，扫描 bean 中需要进行 rpc 注入的属性；
     * 将对应的属性使用 代理对象 进行替换
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取 bean 的所有属性（不分修饰符）
        Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历所有属性
        for (Field field : fields) {
            // 判断是否被 @RpcReference 标注
            if (field.isAnnotationPresent(RpcReference.class)) {
                // 获取注解
                RpcReference rpcReferenceAnnotation = field.getAnnotation(RpcReference.class);
                // 获取 被@RpcReference标注 的属性当前类型
                Class<?> clazz = field.getType();
                try {
                    if (!"".equals(rpcReferenceAnnotation.interfaceName())) {
                        clazz = Class.forName(rpcReferenceAnnotation.interfaceName());
                    }
                    if (rpcReferenceAnnotation.interfaceClass() != void.class) {
                        clazz = rpcReferenceAnnotation.interfaceClass();
                    }
                    // 获取指定类型的代理对象！
                    Object proxy = clientProxyFactory.getProxy(clazz, rpcReferenceAnnotation.version());
                    // 关闭安全检测
                    field.setAccessible(true);
                    // 设置 bean属性 为代理对象！
                    field.set(bean, proxy);

                } catch (Exception e) {
                    throw new RuntimeException(String.format("the type of field [%s] is [%s] and the proxy type is [%s]", field.getName(), field.getClass(), clazz), e);
                }
            }
        }
        // 返回bean
        return bean;
    }
}
```

## 7.连接通信

### 1.共同接口

```java
package com.mirrors.server.communicate;

/**
 * rpc服务类， 接受客户端信息，调用客户端请求的方法，返回结果给客户端
 * 具体由子类实现
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 20:54
 */
public interface RpcServer {

    /**
     * 开启服务
     *
     * @param port
     */
    void start(Integer port);
}
```

###2.socket

```java
package com.mirrors.client.communicate.socket;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.client.communicate.RpcClient;
import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.dto.RpcResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于socket的客户端；
 * 发送接受的数据类型为 RpcRequest 和 RpcResponse（从RpcMetadata中获取）
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 17:06
 */
public class SocketRpcClient implements RpcClient {
    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        // 获取服务地址和端口
        InetSocketAddress socketAddress = new InetSocketAddress(requestMetadata.getServerIp(), requestMetadata.getServerPort());
        try (Socket socket = new Socket()) {
            // 利用socket与服务端连接
            socket.connect(socketAddress);
            // socket输入流 发送RpcRequest（从requestMetadata中取）
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(requestMetadata.getRpcMessage().getRpcMessageBody());
            outputStream.flush();
            // 阻塞等待 服务端响应数据
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            RpcResponse rpcResponse = (RpcResponse) inputStream.readObject();
            // 封装成 RpcMessage 对象返回给 调用者
            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setRpcMessageBody(rpcResponse);
            return rpcMessage;

        } catch (Exception e) {
            throw new RuntimeException("the socket client failed to read or write", e);
        }
    }
}
```

### 3.http

```java
package com.mirrors.client.communicate.http;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.client.communicate.RpcClient;
import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.dto.RpcResponse;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

/**
 * 基于http的客户端；
 * 发送接受的数据类型为 RpcRequest 和 RpcResponse（从RpcMetadata中获取）
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 17:06
 */
public class HttpRpcClient implements RpcClient {
    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        try {
            // 构建 http 请求
            URL url = new URL("http", requestMetadata.getServerIp(), requestMetadata.getServerPort(), "/");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);

            // 发送请求
            OutputStream connectionOutputStream = httpURLConnection.getOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(connectionOutputStream);
            outputStream.writeObject(requestMetadata.getRpcMessage().getRpcMessageBody());
            outputStream.flush();
            outputStream.close();

            // 构建接受响应数据的输入流
            RpcMessage rpcMessage = new RpcMessage();
            InputStream connectionInputStream = httpURLConnection.getInputStream();
            ObjectInputStream inputStream = new ObjectInputStream(connectionInputStream);

            // 阻塞读取
            RpcResponse rpcResponse = (RpcResponse) inputStream.readObject();
            rpcMessage.setRpcMessageBody(rpcResponse);

            return rpcMessage;

        } catch (Exception e) {
            throw new RuntimeException("the http client failed to read or write", e);
        }
    }
}
```

### 4.netty

参考：[异步编程利器：CompletableFuture详解 ｜Java 开发实战 - 掘金 (juejin.cn)](https://juejin.cn/post/6970558076642394142#heading-20) 

​	  [CompletableFuture用法详解 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/344431341) 

#### 1.channel缓存

```java
package com.mirrors.client.communicate.netty;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存和获取 连接的对应Channel对象
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 20:59
 */
public class NettyChannelCache {

    /**
     * 存储channel；
     * key = ip:port（String类型）；
     * value = channel对象
     */
    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    /**
     * 根据ip和端口，获取连接的channel对象
     *
     * @param ip
     * @param port
     * @return
     */
    public Channel get(String ip, Integer port) {
        String key = ip + ":" + port;
        // 判断 ip:port 是否已经连接
        if (channelMap.containsKey(key)) {
            // 获取对应channel
            Channel channel = channelMap.get(key);
            if (channel != null && channel.isActive()) {
                // 如果channel存在且活跃
                return channel;
            } else {
                // 否则从缓存中删除
                channelMap.remove(key);
            }
        }
        return null;
    }

    /**
     * 根据InetSocketAddress，获取连接的channel对象
     *
     * @param socketAddress
     * @return
     */
    public Channel get(InetSocketAddress socketAddress) {
        return get(socketAddress.getHostName(), socketAddress.getPort());
    }

    /**
     * 根据ip和端口，保存连接的channel对象
     *
     * @param ip
     * @param port
     * @param channel
     */
    public void set(String ip, Integer port, Channel channel) {
        String key = ip + ":" + port;
        channelMap.put(key, channel);
    }

    /**
     * 根据InetSocketAddress，保存连接的channel对象
     *
     * @param socketAddress
     * @param channel
     */
    public void set(InetSocketAddress socketAddress, Channel channel) {
        set(socketAddress.getHostName(), socketAddress.getPort(), channel);
    }
}
```

#### 2.消息处理器

```java
package com.mirrors.client.communicate.netty;

import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.bean.RpcMessageHeader;
import com.mirrors.core.constants.RpcConstant;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.enums.MessageType;
import com.mirrors.core.enums.SerializerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端响应信息处理器，指定RpcMessage类型
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 17:28
 */
@Slf4j
public class NettyRpcResponseHandler extends SimpleChannelInboundHandler<RpcMessage> {

    /**
     * 保存 还未处理的请求
     */
    public static final Map<Integer, Promise<RpcMessage>> UNPROCESSED_RESPONSE = new ConcurrentHashMap<>();

    /**
     * 触发读
     *
     * @param channelHandlerContext
     * @param rpcMessage
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage) throws Exception {
        try {
            MessageType messageType = MessageType.getByType(rpcMessage.getRpcMessageHeader().getMessageType());
            // 判断接受的消息类型 是rpc请求 还是心跳检测
            if (messageType == MessageType.RESPONSE) {
                // rpc返回请求
                int sequenceId = rpcMessage.getRpcMessageHeader().getSequenceId();
                // 拿到 还未处理的 promise 对象
                Promise<RpcMessage> promise = UNPROCESSED_RESPONSE.remove(sequenceId);
                if (promise != null) {
                    // 强转RpcResponse信息
                    RpcResponse rpcResponse = (RpcResponse) rpcMessage.getRpcMessageBody();
                    Exception exception = rpcResponse.getExceptionValue();
                    // 根据 远程调用返回的结果是否出错 设置promise的结果！
                    if (exception != null) {
                        promise.setFailure(exception);
                    } else {
                        promise.setSuccess(rpcMessage);
                    }
                }

            } else if (messageType == MessageType.HEARTBEAT_RESPONSE) {
                // 心跳检测
                log.debug("heartbeat message: {}.", rpcMessage.getRpcMessageBody());
            }
        } finally {
            // 释放资源
            ReferenceCountUtil.release(rpcMessage);
        }
    }

    /**
     * 触发写空闲，当检测到写空闲发生自动发送一个心跳检测数据包
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断是否触发空闲世界
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            // 如果触发写空闲，发送心跳检测
            if (idleStateEvent.state() == IdleState.WRITER_IDLE) {
                log.warn("write idle happen [{}].", ctx.channel().remoteAddress());
                // 构建心跳检测信息
                RpcMessageHeader header = RpcMessageHeader.createBySerializer(SerializerType.KRYO.name());
                header.setMessageType(MessageType.HEARTBEAT_REQUEST.getType());

                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setRpcMessageHeader(header);
                rpcMessage.setRpcMessageBody(RpcConstant.PING);
                // 发送心跳检测请求，异常时关闭
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }

        } else {
            // 没有触发空闲，放行
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 捕获到异常时
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("client catch exception: ", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
```

#### 3.netty客户端

```java
package com.mirrors.client.communicate.netty;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.client.communicate.RpcClient;
import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.codec.RpcFrameDecoder;
import com.mirrors.core.codec.SharableRpcMessageCodec;
import com.mirrors.core.factory.SingletonFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于netty的客户端
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 17:06
 */
@Slf4j
public class NettyRpcClient implements RpcClient {

    /**
     * netty客户端 启动类
     */
    private final Bootstrap bootstrap;

    /**
     * 保存和获取 连接的对应Channel对象
     */
    private final NettyChannelCache channelCache;

    /**
     * 无参构造函数，初始化客户端（还未连接）
     */
    public NettyRpcClient() {
        bootstrap = new Bootstrap();
        channelCache = SingletonFactory.getInstance(NettyChannelCache.class);
        // 事件循环对象组，每一个事件循环对象对应一个线程（维护一个 Selector），用来处理 channel 上的 io 事件
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        // 配置启动类
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        // 超过 15s 内如果没有向服务器写数据，触发 写空闲
                        pipeline.addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS));
                        // 添加 粘包拆包 解码器
                        pipeline.addLast(new RpcFrameDecoder());
                        // 添加 编解码器
                        pipeline.addLast(new SharableRpcMessageCodec());
                        // 添加 rpc响应消息处理器
                        pipeline.addLast(new NettyRpcResponseHandler());
                    }
                });
    }

    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        try {
            // 创建 接受响应结果的 promise
            Promise<RpcMessage> promise;
            // 获取连接的channel对象
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

    /**
     * 获取 该连接的channel对象；
     * 如果没有找到连接的channel对象，说明还没进行连接；先进行连接，保存后返回channel
     *
     * @param socketAddress
     * @return
     */
    public Channel getChannel(InetSocketAddress socketAddress) {
        Channel channel = channelCache.get(socketAddress);
        if (channel == null) {
            // 如果没有找到连接的channel对象，说明还没进行连接；先进行连接
            channel = connect(socketAddress);
            // 连接成功后，保存连接的channel对象
            channelCache.set(socketAddress, channel);
        }
        return channel;
    }

    /**
     * 根据 ip:port 与服务端进行连接，返回channel；
     * 使用 CompletableFuture 异步接受（重点知识）
     *
     * @param socketAddress
     * @return
     */
    public Channel connect(InetSocketAddress socketAddress) {
        try {
            CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
            // 客户端开始连接，添加监听器，接收到响应回调！
            bootstrap.connect(socketAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.debug("the client successfully connected to server [{}]", socketAddress.toString());
                    completableFuture.complete(future.channel());
                } else {
                    throw new RuntimeException(String.format("the client failed to connect to [%s].", socketAddress.toString()));
                }
            });
            // 阻塞等待 future 返回 连接结果
            Channel channel = completableFuture.get();
            // 添加 异步 关闭之后的操作
            channel.closeFuture().addListener(future -> {
                log.info("the client disconnected from server [{}].", socketAddress.toString());
            });
            return channel;

        } catch (Exception e) {
            throw new RuntimeException("error occur while connecting: ", e);
        }
    }
}
```

























