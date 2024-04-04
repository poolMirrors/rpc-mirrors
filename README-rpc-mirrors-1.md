# 1.搭建项目

## 1.介绍

1. 基于**Netty / Http / Socket 三种方式**进行网络通信
2. 自定义通信协议，自定义编解码器，实现5种序列化算法（Hessian/ JDK / JSON / ProtoStuff / Kryo）
3. 实现Netty心跳检测，并重用Channel，避免重复连接
4. 引入 Zookeeper / Nacos 作为注册中心，实现服务本地缓存与监听，以及3种负载均衡算法（一致性哈希等）
5. 集成Spring Boot，通过自定义注解扫描服务组件，注册服务，实现自动装配
6. 单机测量，基于 JMH 压测在 10000 并发量下的吞吐量在 29300 上下

## 2.RPC概述

RPC 又称远程过程调用（Remote Procedure Call），用于解决**分布式系统中服务之间的调用问题**。通俗地讲，就是开发者能够像调用本地方法一样调用远程的服务。一个最基本的RPC框架的基本架构如下图所示：

![](/images/简单RPC架构图.png)

RPC框架一般必须包含三个组件，分别是**客户端、服务端**以及**注册中心**，一次完整的 RPC 调用流程一般为：

1. **服务端**启动服务后，将他提供的**服务列表发布到注册中心（服务注册）**；
2. **客户端**会向注册中心**订阅相关的服务地址（服务订阅）**；
3. **客户端**通常会利用**本地代理模块 Proxy 向服务端发起远程过程调用**，Proxy 负责**将调用的方法、参数等数据转化为网络字节流**；
4. **客户端**从服务列表中**根据负载均衡策略选择一个服务地址**，并**将数据通过网络发送给服务端**；
5. **服务端**得到数据后，**调用**对应的服务，然后将结果通过网络**返回给客户端**。

## 3.项目结构

![](/images/项目结构.png)

1. `consumer`模块：服务的消费者，依赖于 `rpc-client-spring-boot-starter` 模块；
2. `provider`模块：服务的提供者，依赖于 `rpc-server-spring-boot-starter` 模块；
3. `provider-api`模块：服务提供者暴露的API；
4. `rpc-client`模块：rpc 客户端模块，封装客户端发起的请求过程，提供服务发现、动态代理，网络通信等功能；
5. `rpc-client-spring-boot-stater`模块：是`rpc-client-spring-boot`的stater模块，负责引入相应依赖进行自动配置；
6. `rpc-core`模块：是rpc核心依赖，提供负载均衡、服务注册发现、消息协议、消息编码解码、序列化算法；
7. `rpc-server`模块：rpc 服务端模块，负责启动服务，接受和处理RPC请求，提供服务发布、反射调用等功能；
8. `rpc-server-spring-boot-stater`模块：是`rpc-server-spring-boot`的stater模块，负责引入相应依赖进行自动配置；

## 4.运行项目

1. 首先需要安装并启动 zookeeper；
2. 修改 Consumer 和 Provider 模块下的 application.yml / application.properties 的注册中心地址，即 rpc.client.registry-addr=???，服务端则配置 rpc.server.registry-addr属性；
3. 先启动 Provider 模块，正常启动 SpringBoot 项目即可，本项目使用基于 SpringBoot 的自动配置，运行后会自动向 SpringIOC 容器中创建需要的 Bean 对象。
4. 然后启动 Consumer 模块，通过 Controller 去访问服务进行 rpc 调用了。

## 5.父工程pom文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.mirrors</groupId>
    <artifactId>rpc-mirrors</artifactId>
    <version>1.0-SNAPSHOT</version>
    <!--打包pom文件-->
    <packaging>pom</packaging>

    <!--模块管理-->
    <modules>
        <module>rpc-server</module>
        <module>rpc-core</module>
        <module>rpc-client</module>
        <module>rpc-server-spring-boot-starter</module>
        <module>rpc-client-spring-boot-starter</module>
        <module>provider</module>
        <module>provider-api</module>
        <module>consumer</module>
    </modules>

    <!--声明版本信息-->
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <encoding>UTF-8</encoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <spring-boot.version>2.5.2</spring-boot.version>
        <!--自定义-->
        <rpc.version>1.0.0</rpc.version>
        <netty.version>4.1.65.Final</netty.version>
        <lombok.version>1.18.24</lombok.version>
        <logback-classic.version>1.2.11</logback-classic.version>
        <curator.version>4.0.0</curator.version>
        <gson.version>2.8.9</gson.version>
        <hessian.version>4.0.65</hessian.version>
        <kryo.version>4.0.2</kryo.version>
        <protostuff.version>1.8.0</protostuff.version>
        <junit.version>3.8.1</junit.version>
        <tomcat.version>9.0.22</tomcat.version>
        <cglib.version>3.1</cglib.version>
        <nacos.version>2.1.1</nacos.version>
    </properties>

    <!--依赖管理-->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!--配置构建定制-->
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                    <executions>
                        <execution>
                            <goals>
                                <goal>repackage</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                        <source>${maven.compiler.source}</source>
                        <target>${maven.compiler.target}</target>
                        <encoding>${project.build.sourceEncoding}</encoding>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>


</project>
```



# 2.rpc-core核心模块

## 1.pom文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!--父模块-->
    <parent>
        <artifactId>rpc-mirrors</artifactId>
        <groupId>com.mirrors</groupId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>rpc-core</artifactId>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
    </properties>
    <!--依赖-->
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
        </dependency>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>${netty.version}</version>
        </dependency>
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback-classic.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-framework</artifactId>
            <version>${curator.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-recipes</artifactId>
            <version>${curator.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-x-discovery</artifactId>
            <version>${curator.version}</version>
        </dependency>
        <!--序列化工具-->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>${gson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.caucho</groupId>
            <artifactId>hessian</artifactId>
            <version>${hessian.version}</version>
        </dependency>
        <dependency>
            <groupId>com.esotericsoftware</groupId>
            <artifactId>kryo</artifactId>
            <version>${kryo.version}</version>
        </dependency>
        <dependency>
            <groupId>io.protostuff</groupId>
            <artifactId>protostuff-core</artifactId>
            <version>${protostuff.version}</version>
        </dependency>
        <dependency>
            <groupId>io.protostuff</groupId>
            <artifactId>protostuff-runtime</artifactId>
            <version>${protostuff.version}</version>
        </dependency>
        <!--服务发现-->
        <dependency>
            <groupId>com.alibaba.nacos</groupId>
            <artifactId>nacos-client</artifactId>
            <version>${nacos.version}</version>
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

###1.消息bean

####1.消息头RpcMessageHeader

位于com.mirrors.core.bean下的两个类，定义了**消息头**和**消息类（包括消息头和消息体）**

```java
package com.mirrors.core.bean;

import com.mirrors.core.constants.RpcConstant;
import com.mirrors.core.enums.MessageStatus;
import com.mirrors.core.enums.MessageType;
import com.mirrors.core.enums.SerializerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网络传输的信息类--消息头（自定义协议）
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 15:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RpcMessageHeader {
    /**
     * 4字节 魔数
     */
    private byte[] magicNum;

    /**
     * 1字节 版本号
     */
    private byte version;

    /**
     * 1字节 序列化算法
     */
    private byte serializerType;

    /**
     * 1字节 消息类型
     */
    private byte messageType;

    /**
     * 1字节 消息状态类型
     */
    private byte messageStatus;

    /**
     * 4字节 消息的序列号ID
     */
    private int sequenceId;

    /**
     * 4字节 数据内容长度
     */
    private int length;

    /**
     * 根据传进的序列化名字，选择不同的序列化算法进行创建 消息头RpcMessageHeader
     *
     * @param serializerName
     * @return
     */
    public RpcMessageHeader createBySerializer(String serializerName) {
        return RpcMessageHeader.builder()
                .magicNum(RpcConstant.MAGIC_NUM) // 魔数
                .version(RpcConstant.VERSION) // 版本
                .serializerType(SerializerType.getByName(serializerName).getType()) // 序列化算法
                .messageType(MessageType.REQUEST.getType()) // 消息类型
                .sequenceId(RpcConstant.getSequenceId()).build(); // 消息序列id
    }

}

```

####2.消息类RpcMessage

```java
package com.mirrors.core.bean;

/**
 * 网路传输的协议信息类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 15:32
 */
public class RpcMessage {

    /**
     * 消息头
     */
    private RpcMessageHeader rpcMessageHeader;

    /**
     * 消息体（存放消息）
     */
    private Object rpcMessageBody;
}

```

### 2.常量constants

```java
package com.mirrors.core.constants;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一些涉及rpc的常量
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 15:42
 */
public class RpcConstant {
    /**
     * 魔数，用来判断是否无效数据包
     */
    public static final byte[] MAGIC_NUM = new byte[]{(byte) 't', (byte) 'z', (byte) 'j', (byte) '6'};

    /**
     * 版本号
     */
    public static final byte VERSION = 1;

    /**
     * 特殊消息类型ping
     */
    public static final String PING = "ping";

    /**
     * 特殊消息类型pong
     */
    public static final String PONG = "pong";

    /**
     * 消息的序列id
     */
    public static final AtomicInteger sequenceId = new AtomicInteger();

    /**
     * 返回序列号
     *
     * @return
     */
    public static int getSequenceId() {
        return sequenceId.getAndIncrement();
    }
}

```

###3.枚举enums

####1.消息状态MessageStatus

```java
package com.mirrors.core.enums;

import lombok.Getter;

/**
 * 消息状态类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 16:21
 */
public enum MessageStatus {
    /**
     * 成功
     */
    SUCCESS((byte) 0),

    /**
     * 失败
     */
    FAIL((byte) 1);

    /**
     * 消息类型
     */
    @Getter
    private final byte type;

    /**
     * 构造函数
     *
     * @param type
     */
    MessageStatus(byte type) {
        this.type = type;
    }

    /**
     * 是否消息类型是否为 成功
     *
     * @param type
     * @return
     */
    public static boolean isSuccess(byte type) {
        return MessageStatus.SUCCESS.type == type;
    }
}

```

#### 2.消息类型MessageType

```java
package com.mirrors.core.enums;

import lombok.Getter;

/**
 * 消息类型
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 16:08
 */
public enum MessageType {
    /**
     * 类型 0 表示请求消息
     * () 默认调用构造器
     */
    REQUEST((byte) 0),

    /**
     * 类型 1 表示响应消息
     */
    RESPONSE((byte) 1),

    /**
     * 类型 2 表示心跳检查请求
     */
    HEARTBEAT_REQUEST((byte) 2),

    /**
     * 类型 3 表示心跳检查响应
     */
    HEARTBEAT_RESPONSE((byte) 3);

    /**
     * 消息类型，字节标识
     */
    @Getter
    private final byte type;

    /**
     * 构造器，初始化type类型
     *
     * @param type
     */
    MessageType(Byte type) {
        this.type = type;
    }

    /**
     * 根据类型返回枚举
     *
     * @param type
     * @return
     */
    public static MessageType getByType(Byte type) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.getType() == type) {
                return messageType;
            }
        }
        // 找不到符合的消息类型，抛出异常，利用String.format占位符
        throw new IllegalArgumentException(String.format("The message type %s is illegal.", type));
    }
}
```

####3.序列化算法SerializerType

```java
package com.mirrors.core.enums;

import lombok.Getter;

/**
 * 序列化类型
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 16:28
 */
public enum SerializerType {
    /**
     * JDK 序列化算法
     */
    JDK((byte) 0),

    /**
     * JSON 序列化算法
     */
    JSON((byte) 1),

    /**
     * HESSIAN 序列化算法
     */
    HESSIAN((byte) 2),

    /**
     * KRYO 序列化算法
     */
    KRYO((byte) 3),

    /**
     * PROTOSTUFF 序列化算法
     */
    PROTOSTUFF((byte) 4);

    /**
     * 序列化类型
     */
    @Getter
    private final byte type;

    /**
     * 构造函数
     *
     * @param type
     */
    SerializerType(byte type) {
        this.type = type;
    }

    /**
     * 通过序列化类型获取序列化算法枚举类
     *
     * @param type 类型
     * @return 枚举类型
     */
    public static SerializerType getByType(byte type) {
        for (SerializerType serializerType : SerializerType.values()) {
            if (serializerType.getType() == type) {
                return serializerType;
            }
        }
        // HESSIAN 作为默认序列化算法
        return HESSIAN;
    }

    /**
     * 通过序列化算法名 获取序列化算法枚举类
     *
     * @param serializerName 类型名称
     * @return 枚举类型
     */
    public static SerializerType getByName(String serializerName) {
        for (SerializerType serializerType : SerializerType.values()) {
            // 不考虑大小写
            if (serializerType.name().equalsIgnoreCase(serializerName)) {
                return serializerType;
            }
        }
        // HESSIAN 作为默认序列化算法
        return HESSIAN;
    }
}
```

### 4.消息体dto

#### 1.心跳检测信息

```Java
package com.mirrors.core.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 心跳检测机制 信息
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 21:20
 */
@Data
@Builder
public class HeartbeatMessage {

    /**
     * 消息
     */
    private String message;
}

```

#### 2.服务端提供信息

```java
package com.mirrors.core.dto;

/**
 * rpc中服务提供方的服务信息
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 21:20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 服务名称 = 服务名-版本号
     */
    private String serviceName;

    /**
     * 版本号
     */
    private String version;

    /**
     * 服务端地址
     */
    private String ip;

    /**
     * 服务端端口
     */
    private String host;
}

```

#### 3.RpcRequest

```java
package com.mirrors.core.dto;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 21:20
 */
@Data
public class RpcRequest {

    /**
     * 服务名称 = 服务名-版本号
     */
    private String serviceName;

    /**
     * 要调用的方法名
     */
    private String methodName;

    /**
     * 方法参数类型
     */
    private Class<?>[] paramTypes;

    /**
     * 方法参数值
     */
    private Object[] paramValues;

}
```

#### 4.RpcResponse

```java
package com.mirrors.core.dto;

/**
 * rpc完成后的返回信息
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 21:20
 */
@Data
public class RpcResponse {

    /**
     * 调用成功后的返回信息
     */
    private Object returnValue;

    /**
     * 调用发生异常时的信息，注意不要直接把异常赋值给exceptionValue，容易导致超出数据包长长度范围
     */
    private Exception exceptionValue;

}
```

### 5.工具类utils

ServiceUtil：**用于 ServiceInfo对象 与 Map 互相转换**

在Nacos的服务注册中需要使用

```java
package com.mirrors.core.utils;

import com.google.gson.Gson;
import com.mirrors.core.dto.ServiceInfo;

import java.util.Collections;
import java.util.Map;

/**
 * service 工具类，用于 ServiceInfo对象 与 Map 互相转换
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 16:30
 */
public class ServiceUtil {

    /**
     * 协助 ServiceInfo对象 和 Map 互转
     */
    public static final Gson gson = new Gson();

    /**
     * 根据 服务名称-版本号 生成注册服务的 key
     *
     * @param serviceName
     * @param version
     * @return
     */
    public static String getServiceKey(String serviceName, String version) {
        return String.join("-", serviceName, version);
    }

    /**
     * 将 serviceInfo 对象转换为 map
     *
     * @param serviceInfo
     * @return
     */
    public static Map toMap(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return Collections.emptyMap();
        }
        Map map = gson.fromJson(gson.toJson(serviceInfo), Map.class);
        // 由于port是Integer类型，所以要单独加入map
        map.put("port", serviceInfo.getPort().toString());
        return map;
    }

    /**
     * 将 map 对象转换为 serviceInfo
     *
     * @param map
     * @return
     */
    public static ServiceInfo toServiceInfo(Map map) {
        // 由于在toMap时，port的值转为了String类型，所以要重新put一个Integer类型
        map.put("port", Integer.parseInt(map.get("port").toString()));
        ServiceInfo serviceInfo = gson.fromJson(gson.toJson(map), ServiceInfo.class);
        return serviceInfo;
    }
}
```

### 6.工厂类factory

SingletonFactory：单例模式，获取单例对象

```java
package com.mirrors.core.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单例模式，获取单例对象
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 21:07
 */
public final class SingletonFactory {

    /**
     * 保持 每个全限定类名 对应的 单例对象
     */
    private static final Map<String, Object> SINGLETON_MAP = new ConcurrentHashMap<>();

    /**
     * 根据 类 获取单例对象
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getInstance(Class<T> clazz) {
        try {
            // 获取全限定类名
            String name = clazz.getName();
            if (SINGLETON_MAP.containsKey(name)) {
                // 单例已经存在，clazz强转返回
                return clazz.cast(SINGLETON_MAP.get(name));
            } else {
                // 单例不存在，利用反射调用构造函数创建
                T instance = clazz.getDeclaredConstructor().newInstance();
                SINGLETON_MAP.put(name, instance);
                return instance;
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }
}
```



## 3.自定义协议和编解码

1. **RPC 框架对性能有非常高的要求，所以通信协议应该越简单越好，这样可以减少编解码的性能损耗**
2. RPC 框架可以基于不同的协议实现，**大部分主流 RPC 框架会选择 TCP、HTTP 协议，出名的 gRPC 框架使用的则是 HTTP2。TCP、HTTP、HTTP2 都是稳定可靠的，但其实使用 UDP 协议也是可以的，具体看业务使用的场景**
3. RPC 框架应该能够支持多种协议

### 1.自定义协议

自定义协议一般要求有：

- **魔数**：用来在第一时间判定是否是无效数据包，**快速**识别**字节流**是否是程序能够处理的，能处理才进行后面的**耗时**业务操作，如果不能处理，尽快执行失败，断开连接等操作。
- **版本号**：可以支持协议的升级
- **序列化算法**：消息正文到底采用哪种序列化反序列化方式，可以由此扩展，例如：json、protobuf、hessian、jdk、kryo
- **消息类型**：是登录、注册、单聊、群聊... 跟业务相关
- **状态类型**：
- **消息序号**：为了双工通信，提供异步能力，通过这个请求ID将响应关联起来，也可以通过请求ID做**链路追踪**。
- **正文长度**：标注传输数据内容的长度，用于判断是否是一个完整的数据包
- 消息正文：主要传递的消息内容

```
------------------------------------------------------------------------------------------
| 魔数 (4byte) | 版本号 (1byte)  | 序列化算法 (1byte) | 消息类型 (1byte) | 状态类型 (1byte)   |
-------------------------------------------------------------------------------------------
|            消息序列号 (4byte)               |            消息长度 (4byte)                 |
-------------------------------------------------------------------------------------------
|                        消息内容 (不固定)                                                  |
-------------------------------------------------------------------------------------------
```

### 2.编解码

#### 1.粘包半包

（1）粘包

- 现象，发送 abc 和 def，接收 abcdef
- 原因
  - **应用层：接收方 ByteBuf 设置太大（Netty 默认 1024）**
  - **滑动窗口：假设发送方 256 bytes 表示一个完整报文，但由于接收方处理不及时且窗口大小足够大，这 256 bytes 字节就会缓冲在接收方的滑动窗口中，当滑动窗口中缓冲了多个报文就会粘包**
  - **Nagle 算法：会造成粘包（**预防小分组的产生 **）**

（2）半包

- 现象，发送 abcdef，接收 abc 和 def
- 原因
  - **应用层：接收方 ByteBuf 小于实际发送数据量**
  - **滑动窗口：假设接收方的窗口只剩了 128 bytes，发送方的报文大小是 256 bytes，这时放不下了，只能先发送前 128 bytes，等待 ack 后才能发送剩余部分，这就造成了半包**
  - **MSS 限制：当发送的数据超过 MSS 限制后，会将数据切分发送，就会造成半包**

（3）本质是**因为 TCP 是流式协议，消息无边界**

#### 2.解决方案

- **短连接：发一次数据包建立一次连接，这样连接建立到连接断开之间就是一次消息边界，缺点是效率低；**
- **固定长度：每一条消息采用固定长度，缺点是浪费空间；**
- **分隔符：每一条消息采用分隔符，例如 \n ，缺点是需要转义；**
- **消息长度+消息内容**：每一条消息分为 header 和 body，header 中包含 body 的长度（推荐）

#### 3.RpcFrameDecoder类

```java
package com.mirrors.core.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * LengthFieldBasedFrameDecoder解码器 自定义长度 解决TCP粘包黏包问题
 * 本质上是ChannelHandler，一个处理入站事件的ChannelHandler
 * 用定长字节表示接下来数据的长度
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 14:51
 */
public class RpcFrameDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * 无参构造
     * 数据帧的最大长度，长度域的偏移字节数，长度域所占的字节数
     */
    public RpcFrameDecoder() {
        super(1024, 12, 4);
    }

    /**
     * 构造函数
     *
     * @param maxFrameLength    数据帧的最大长度
     * @param lengthFieldOffset 长度域的偏移字节数
     * @param lengthFieldLength 长度域所占的字节数
     */
    public RpcFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }
}

// 只需要创建 new LengthFiledBasedFrameDecoder(1024, 12, 4, 0) 的帧解码器就可以解决粘包半包问题
```

####4.共享的编码解码类

```java
package com.mirrors.core.codec;

import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.bean.RpcMessageHeader;
import com.mirrors.core.constants.RpcConstant;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.enums.MessageType;
import com.mirrors.core.enums.SerializerType;
import com.mirrors.core.serializer.Serializer;
import com.mirrors.core.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.Arrays;
import java.util.List;

/**
 * 注解Sharable标志共享
 * 消息编码解码器，前提要有 {@link com.mirrors.core.codec.RpcFrameDecoder} 进行粘包半包处理
 * --------------------------------------------------------------------
 * | 魔数 (4byte) | 版本号 (1byte)  | 序列化算法 (1byte) | 消息类型 (1byte) |
 * --------------------------------------------------------------------
 * |    状态类型 (1byte)  |    消息序列号 (4byte)   |    消息长度 (4byte)   |
 * --------------------------------------------------------------------
 * |                        消息内容 (不固定长度)                         |
 * --------------------------------------------------------------------
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 15:23
 */
@ChannelHandler.Sharable
public class SharableRpcMessageCodec extends MessageToMessageCodec {

    /**
     * 编码，将RpcMessage转为ByteBuf
     *
     * @param channelHandlerContext
     * @param object
     * @param list
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object object, List list) throws Exception {
        // 将object类型转换，拿到 消息头 和 消息体
        RpcMessage message = (RpcMessage) object;
        RpcMessageHeader header = message.getRpcMessageHeader();
        Object body = message.getRpcMessageBody();

        // 拿到ByteBuf
        ByteBuf byteBuf = channelHandlerContext.alloc().buffer();

        // 根据 消息头 设置
        byteBuf.writeBytes(header.getMagicNum());
        byteBuf.writeByte(header.getVersion());
        byteBuf.writeByte(header.getSerializerType());
        byteBuf.writeByte(header.getMessageType());
        byteBuf.writeByte(header.getMessageStatus());
        byteBuf.writeInt(header.getSequenceId());

        // 计算消息长度（获取序列化算法，先序列化，再设置 消息头的长度字段）
        SerializerType serializerType = SerializerType.getByType(header.getSerializerType());
        Serializer serializer = SerializerFactory.getSerializer(serializerType);
        byte[] bytes = serializer.serialize(body);
        header.setLength(bytes.length);

        // 消息体长度 和 消息正文 依次写入ByteBuf
        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);

        // 传递下一个handler
        list.add(byteBuf);
    }

    /**
     * 解码,将ByteBuf转为RpcMessage
     *
     * @param channelHandlerContext
     * @param object
     * @param list
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, Object object, List list) throws Exception {
        // 将object类型转换ByteBuf
        ByteBuf byteBuf = (ByteBuf) object;

        // 4字节 魔数，判断是否符合协议要求
        int magicNumLength = RpcConstant.MAGIC_NUM.length;
        byte[] magicNum = new byte[magicNumLength];
        byteBuf.readBytes(magicNum, 0, magicNumLength);
        for (int i = 0; i < magicNumLength; i++) {
            if (magicNum[i] != RpcConstant.MAGIC_NUM[i]) {
                throw new IllegalArgumentException("unknown0 magic number: " + Arrays.toString(magicNum));
            }
        }
        // 1字节 版本号，判断是否符合要求
        byte version = byteBuf.readByte();
        if (version != RpcConstant.VERSION) {
            throw new IllegalArgumentException("the version number does not match: " + version);
        }
        // 1字节 序列化类型
        byte serializerType = byteBuf.readByte();
        // 1字节 消息类型
        byte messageType = byteBuf.readByte();
        // 1字节 消息状态
        byte messageStatus = byteBuf.readByte();
        // 4字节 消息序列号ID
        int sequenceId = byteBuf.readInt();
        // 4字节 消息体长度
        int bodyLength = byteBuf.readInt();
        // 最后 消息体（正文）
        byte[] bytes = new byte[bodyLength];
        byteBuf.readBytes(bytes, 0, bodyLength);

        // 构建消息头
        RpcMessageHeader rpcMessageHeader = RpcMessageHeader.builder()
                .magicNum(magicNum)
                .version(version)
                .serializerType(serializerType)
                .messageType(messageType)
                .messageStatus(messageStatus)
                .sequenceId(sequenceId)
                .length(bodyLength).build();

        // 根据 序列化类型 获取序列化对象
        SerializerType serializationType = SerializerType.getByType(serializerType);
        Serializer serializer = SerializerFactory.getSerializer(serializationType);

        // 创建信息类对象（先设置消息头），获取消息类型，根据消息类型，反序列化对应的类（再设置消息体）
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setRpcMessageHeader(rpcMessageHeader);

        MessageType msgType = MessageType.getByType(messageType);
        if (msgType == MessageType.REQUEST) {
            // 反序列化
            RpcRequest rpcRequest = serializer.deserialize(RpcRequest.class, bytes);
            rpcMessage.setRpcMessageBody(rpcRequest);
        } else if (msgType == MessageType.RESPONSE) {
            // 反序列化
            RpcResponse rpcResponse = serializer.deserialize(RpcResponse.class, bytes);
            rpcMessage.setRpcMessageBody(rpcResponse);
        } else if (msgType == MessageType.HEARTBEAT_REQUEST || msgType == MessageType.HEARTBEAT_RESPONSE) {
            // 心跳检测的request和response都是用的同一个类，且类对象只有一个String属性，反序列化为String即可
            String message = serializer.deserialize(String.class, bytes);
            rpcMessage.setRpcMessageBody(message);
        }

        // 传递下一个handler
        list.add(rpcMessage);
    }
}
```



##4.序列化算法

###1.实现接口

所有序列化算法都要实现 Serializer 这个接口

```java
package com.mirrors.core.serializer;

/**
 * 序列化算法接口，所有序列哈算法都实现接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:26
 */
// SPI 的本质是将接口实现类的全限定名配置在文件中，并由服务加载器读取配置文件，加载实现类，动态为接口替换实现类。正因此特性，我们可以很容易的通过 SPI 机制为我们的程序提供拓展功能
@SPI
public interface Serializer {

    /**
     * 序列化
     *
     * @param data
     * @param <T>
     * @return
     */
    <T> byte[] serialize(T data);

    /**
     * 反序列化
     *
     * @param clazz
     * @param bytes
     * @param <T>
     * @return
     */
    <T> T deserialize(Class<T> clazz, byte[] bytes);
}
```

根据 SerializerFactory 这个类来获取序列化对象

```java
package com.mirrors.core.serializer;

import com.mirrors.core.enums.SerializerType;
import com.mirrors.core.serializer.hessian.HessianSerializer;
import com.mirrors.core.serializer.jdk.JdkSerializer;
import com.mirrors.core.serializer.json.JsonSerializer;
import com.mirrors.core.serializer.kryo.KryoSerializer;
import com.mirrors.core.serializer.protostuff.ProtostuffSerializer;

/**
 * 根据 序列化类型 找到 序列化器
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:35
 */
public class SerializerFactory {

    /**
     * 根据 序列化类型 找到 序列化器
     *
     * @param serializerType
     * @return
     */
    public static Serializer getSerializer(SerializerType serializerType) {
        switch (serializerType) {
            case JDK:
                return new JdkSerializer();
            case JSON:
                return new JsonSerializer();
            case KRYO:
                return new KryoSerializer();
            case PROTOSTUFF:
                return new ProtostuffSerializer();
            case HESSIAN:
                return new HessianSerializer();
            default:
                // 找不到对应算法就报错
                throw new IllegalArgumentException(String.format("The serialization type %s is illegal.", serializerType.name()));
        }
    }
}
```

### 2.jdk

```java
package com.mirrors.core.serializer.jdk;

import com.mirrors.core.serializer.Serializer;

import java.io.*;

/**
 * jdk序列化
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:32
 */
public class JdkSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T data) {
        try {
            // 保存data数据
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 对象的序列化流，把对象转成字节数据的输出到文件中（或ByteArrayOutputStream）保存
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            // 将 data 写入ByteArrayOutputStream
            objectOutputStream.writeObject(data);
            // 利用 ByteArrayOutputStream 转为字节数组返回
            return byteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("jdk serialize fail", e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            // 读入bytes数组
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            // 利用 ObjectInputStream 转为 Object
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            // 类型转换，返回
            return (T) objectInputStream.readObject();
            
        } catch (Exception e) {
            throw new RuntimeException("jdk serialize fail", e);
        }
    }
}
```

### 3.hessian

```java
package com.mirrors.core.serializer.hessian;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import com.mirrors.core.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Hessian序列化
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:32
 */
public class HessianSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T data) {
        try {
            // 保存到 ByteArrayOutputStream
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 利用 HessianSerializerOutput 写入 ByteArrayOutputStream
            HessianSerializerOutput hessianSerializerOutput = new HessianSerializerOutput(byteArrayOutputStream);
            hessianSerializerOutput.writeObject(data);
            // 注意要 flush 刷新
            hessianSerializerOutput.flush();
            return byteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("hessian serialize fail", e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            // 读入 bytes
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            // 读入 byteArrayInputStream，转为Object
            HessianSerializerInput hessianSerializerInput = new HessianSerializerInput(byteArrayInputStream);
            return (T) hessianSerializerInput.readObject();

        } catch (IOException e) {
            throw new RuntimeException("hessian serialize fail", e);
        }
    }
}
```

### 4.json

```java
package com.mirrors.core.serializer.json;

import com.google.gson.*;
import com.mirrors.core.serializer.Serializer;
import lombok.SneakyThrows;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:33
 */
public class JsonSerializer implements Serializer {

    /**
     * 内部类：自定义 JavaClass 对象序列化，解决 Gson 无法序列化 Class 信息
     * 1.实现 JsonSerializer<Class<?>> 接口； Class<?>类型
     * 2.实现 JsonDeserializer<Class<?>> 接口； Class<?>类型
     */
    static class ClassCodec implements com.google.gson.JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

        @SneakyThrows
        @Override
        public Class<?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            // 根据全限定类名 json -> class
            String name = jsonElement.getAsString();
            return Class.forName(name);
        }

        @Override
        public JsonElement serialize(Class<?> aClass, Type type, JsonSerializationContext jsonSerializationContext) {
            // 根据全限定类名 class -> json
            return new JsonPrimitive(aClass.getName());
        }
    }

    @Override
    public <T> byte[] serialize(T data) {
        try {
            // 利用 registerTypeAdapter 添加 特定对Class类型的序列化类ClassCodec
            Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
            // 对象 -> json
            String json = gson.toJson(data);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("json serialize fail", e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            // 利用 registerTypeAdapter 添加 特定对Class类型的序列化类ClassCodec
            Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
            // 将 bytes 转为 String
            String json = new String(bytes, StandardCharsets.UTF_8);
            // 利用 gson 将 json（根据clazz）转为特定对象
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("json serialize fail", e);
        }
    }
}

```

### 5.kryo

```java
package com.mirrors.core.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:33
 */
public class KryoSerializer implements Serializer {

    /**
     * kryo不是线程安全
     * 利用ThreadLocal为每一个线程初始化时，都创建返回属于自己的一个kryo
     */
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 支持 RpcRequest 和 RpcResponse 两个类的序列化和反序列化
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        return kryo;
    });

    @Override
    public <T> byte[] serialize(T data) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 利用Kryo提供的 Output 来接受
            Output output = new Output(byteArrayOutputStream);
            // 从 kryoThreadLocal 中取出本线程的 kryo 对象
            Kryo kryo = kryoThreadLocal.get();
            // 将data写入output
            kryo.writeObject(output, data);
            // 使用后，remove移除本线程对象
            kryoThreadLocal.remove();
            // 使用output转为 byte数组 返回
            return output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException("kryo serialize fail", e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            // 利用Kryo提供的 Input 来接受
            Input input = new Input(byteArrayInputStream);
            Kryo kryo = kryoThreadLocal.get();
            // kryo从input中读取对象
            T object = kryo.readObject(input, clazz);
            // 先删除，再返回
            kryoThreadLocal.remove();
            return object;
        } catch (Exception e) {
            throw new RuntimeException("kryo serialize fail", e);
        }
    }
}
```

### 6.protostuff

```java
package com.mirrors.core.serializer.protostuff;

import com.mirrors.core.serializer.Serializer;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:34
 */
public class ProtostuffSerializer implements Serializer {

    /**
     * 提前分配Buffer，避免每次进行序列化都需要重新分配 buffer 内存空间
     * 默认 512B
     */
    private final LinkedBuffer LINKEDBUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

    @Override
    public <T> byte[] serialize(T data) {
        try {
            // 序列化对象的结构
            Schema schema = RuntimeSchema.getSchema(data.getClass());
            return ProtostuffIOUtil.toByteArray(data, schema, LINKEDBUFFER);

        } catch (Exception e) {
            throw new RuntimeException("protostuff serialize fail", e);
        } finally {
            // 序列化要重置 LINKEDBUFFER
            LINKEDBUFFER.clear();
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            Schema<T> schema = RuntimeSchema.getSchema(clazz);
            // schema.newMessage()底层使用的反射创建？
            T object = schema.newMessage();
            // 合并另一个消息对象中的字段到当前消息对象中，如果当前消息对象中已有相同的字段，则不会覆盖它们
            ProtostuffIOUtil.mergeFrom(bytes, object, schema);
            return object;

        } catch (Exception e) {
            throw new RuntimeException("protostuff serialize fail", e);
        }
    }
}
```

### 7.对比

客户端和服务端在通信过程中需要传输哪些数据呢？这些数据又该如何编解码呢？如果**采用 TCP 协议，你需要将调用的接口、方法、请求参数、调用属性等信息序列化成二进制字节流传递给服务提供方**，服务端接收到数据后，**再把二进制字节流反序列化得到调用信息，然后利用反射的原理调用对应方法，最后将返回结果、返回码、异常信息等返回给客户端**。

所谓**序列化和反序列化就是将对象转换成二进制流以及将二进制流再转换成对象的过程**。因为**网络通信依赖于字节流**，而且这些请求信息都是不确定的，所以一般会选用通用且高效的序列化算法。比较常用的序列化算法有 FastJson、Kryo、Hessian、Protobuf 等，这些第三方序列化算法都比 Java 原生的序列化操作都更加高效。Dubbo 支持多种序列化算法，并定义了 Serialization 接口规范，所有序列化算法扩展都必须实现该接口，其中**默认使用的是 Hessian 序列化算法**。

序列化对于远程调用的响应速度、吞吐量、网络带宽消耗等同样也起着至关重要的作用，是我们提升分布式系统性能的最关键因素之一。

**判断一个编码框架的优劣主要从以下几个方面：**

```undefined
是否支持跨语言，支持语种是否丰富
编码后的码流
编解码的性能
类库是否小巧，API使用是否方便
使用者开发的工作量和难度。
```

五种序列化算法的比较如下：

| 序列化算法     | **优点**                 | **缺点**         |
| -------------- | ------------------------ | ---------------- |
| **Kryo**       | 速度快，序列化后体积小   | 跨语言支持较复杂 |
| **Hessian**    | 默认支持跨语言           | 较慢             |
| **Protostuff** | 速度快，基于protobuf     | 需静态编译       |
| **Json**       | 使用方便                 | 性能一般         |
| **Jdk**        | 使用方便，可序列化所有类 | 速度慢，占空间   |

【参考】：[Java中四个json解析包对比 - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/344360857) 



## 5.负载均衡策略

在**分布式系统中，服务提供者和服务消费者都会有多台节点，如何保证服务提供者所有节点的负载均衡呢**？客户端在发起调用之前，**需要感知有多少服务端节点可用，然后从中选取一个进行调用**。客户端需要拿到服务端节点的状态信息，并根据不同的策略实现负载均衡算法。负载均衡策略是影响 RPC 框架吞吐量很重要的一个因素

- **Round-Robin 轮询**。Round-Robin 是最简单有效的负载均衡策略，并没有考虑服务端节点的实际负载水平，而是依次轮询服务端节点。
- Weighted Round-Robin **权重轮询**。对不同负载水平的服务端节点增加权重系数，这样可以通过权重系数降低性能较差或者配置较低的节点流量。权重系数可以根据服务端负载水平实时进行调整，使集群达到相对均衡的状态。
- **Least Connections 最少连接数**。客户端**根据服务端节点当前的连接数进行负载均衡，客户端会选择连接数最少的一台服务器进行调用**。Least Connections 策略只是服务端其中一种维度，我们可以演化出**最少请求数、CPU 利用率最低等其他维度的负载均衡方案**。
- **【重点】Consistent Hash 一致性 Hash**。目前主流推荐的负载均衡策略，Consistent Hash 是一种特殊的 Hash 算法，**在服务端节点扩容或者下线时，尽可能保证客户端请求还是固定分配到同一台服务器节点**。Consistent Hash 算法是**采用哈希环来实现的**，通过 **Hash 函数将对象和服务器节点放置在哈希环上**，一般来说服务器可以选择 IP + Port 进行 Hash，然后为对象选择对应的服务器节点，**在哈希环中顺时针查找距离对象 Hash 值最近的服务器节点。**
- **Random 随机访问**。

此外，负载均衡算法可以是多种多样的，客户端可以记录例如健康状态、连接数、内存、CPU、Load 等更加丰富的信息，根据综合因素进行更好地决策。

### 1.继承抽象类

抽象类要实现的接口

```java
package com.mirrors.core.loadbalance;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;

import java.util.List;

/**
 * 负载均衡接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:26
 */
@SPI
public interface LoadBalance {

    /**
     * 从 ServiceInfo的列表中（服务端列表）中选择一个服务方进行返回
     *
     * @param serviceInfoList
     * @param rpcRequest
     * @return
     */
    ServiceInfo select(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest);
}

```

抽象类

```java
package com.mirrors.core.loadbalance;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;

import java.util.List;

/**
 * 负载均衡抽象类，重写接口扩展，接口内调用doSelect，让子类调用doSelect
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:37
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    /**
     * 重写接口的select方法，进行判断，最后让子类实现服务选择
     *
     * @param serviceInfoList
     * @param rpcRequest
     * @return
     */
    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest) {
        // 服务列表为空，返回null
        if (serviceInfoList == null || serviceInfoList.isEmpty()) {
            return null;
        }
        // 只有一个服务，直接返回
        if (serviceInfoList.size() == 1) {
            return serviceInfoList.get(0);
        }
        // 让子类实现doSelect，进行服务选择
        return doSelect(serviceInfoList, rpcRequest);
    }

    /**
     * 由子类，真正执行负载均衡（选择哪个服务）
     *
     * @param serviceInfoList
     * @param rpcRequest
     * @return
     */
    protected abstract ServiceInfo doSelect(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest);
}
```

### 2.随机策略

```java
package com.mirrors.core.loadbalance.impl;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.loadbalance.AbstractLoadBalance;

import java.util.List;
import java.util.Random;

/**
 * 随机策略
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:44
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    @Override
    protected ServiceInfo doSelect(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest) {
        int size = serviceInfoList.size();
        // 生成[0,size-1]之间的随机数
        return serviceInfoList.get(new Random().nextInt(size));
    }

}
```

### 3.轮询策略

参考：[Java魔法类：Unsafe应用解析 - 美团技术团队 (meituan.com)](https://tech.meituan.com/2019/02/14/talk-about-java-magic-class-unsafe.html) 

```java
package com.mirrors.core.loadbalance.impl;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.loadbalance.AbstractLoadBalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询策略
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:46
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    /**
     * 利用 AtomicInteger 记录轮询的计数，初始化为0
     */
    private static final AtomicInteger atomicInteger = new AtomicInteger(0);

    /**
     * 返回当前值并加一，通过 CAS 原子更新
     * 当 atomicInteger 的值来到 Integer.MAX_VALUE 时，重新置0（解决AtomicInteger越界问题）
     *
     * @return
     */
    private final int getAndIncrement() {
        int current, next;
        do {
            current = atomicInteger.get();
            next = (current >= Integer.MAX_VALUE ? 0 : current + 1);
            // current不变，要是 CAS 成功就退出，返回current
        } while (!atomicInteger.compareAndSet(current, next));
        return current;
    }

    @Override
    protected ServiceInfo doSelect(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest) {
        int size = serviceInfoList.size();
        return serviceInfoList.get(getAndIncrement() % size);
    }
}
```

### 4.一致性哈希策略

参考：[一致性哈希算法（consistent hashing） - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/129049724) 

​	[MessageDigest的功能及用法 - 一片黑 - 博客园 (cnblogs.com)](https://www.cnblogs.com/honey01/p/6420111.html) 

​	[负载均衡 | Apache Dubbo](https://cn.dubbo.apache.org/zh-cn/docsv2.7/dev/source/loadbalance/#23-consistenthashloadbalance) 

- 首先根据 ip 或者其他的信息为缓存节点生成一个 hash，并将这个 hash 投射到 [0, 232 - 1] 的圆环上。
- 当有查询或写入请求时，则为缓存项的 key 生成一个 hash 值。然后查找第一个大于或等于该 hash 值的缓存节点，并到这个节点中查询或写入缓存项。
- 如果当前节点挂了，则在下一次查询或写入缓存时，为缓存项查找另一个大于其 hash 值的缓存节点即可。

```java
package com.mirrors.core.loadbalance.impl;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.loadbalance.AbstractLoadBalance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:46
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    
    /**
     * 服务调用时key 与 实现一致性哈希的内部类 的映射缓存
     * key：服务名 和 方法名
     */
    private final Map<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected ServiceInfo doSelect(List<ServiceInfo> serviceInfos, RpcRequest rpcRequest) {
        // 根据 服务名 和 方法名 构建key
        String key = rpcRequest.getServiceName() + "." + rpcRequest.getMethodName();
        // 获取 服务列表 的原始hash值
        int identityHashCode = System.identityHashCode(serviceInfos);
        // 根据 key 从TreeMap中找到 selector
        ConsistentHashSelector selector = selectors.get(key);
        // 如果 selector为空 或 hash值不一样，说明缓存不存在或失效，则缓存的服务列表发生变化（服务列表个数可能增加减少）
        if (selector == null || selector.identityHashCode != identityHashCode) {
            // 创建新的服务列表缓存
            selectors.put(key, new ConsistentHashSelector(serviceInfos, 160, identityHashCode));
            selector = selectors.get(key);
        }
        // 根据 方法参数值 和 key 构造一致性哈希计算的selectKey
        String selectKey = key;
        if (rpcRequest.getParamValues() != null && rpcRequest.getParamValues().length > 0) {
            selectKey += Arrays.stream(rpcRequest.getParamValues()).toString();
        }
        // 将 key 与 方法参数 进行hash运算，因此 ConsistentHashLoadBalance 的负载均衡逻辑只受参数值影响，且不关系权重
        // 具有相同参数值的请求将会被分配给同一个服务提供者
        return selector.select(selectKey);
    }

    /**
     * 实现一致性哈希的内部类
     */
    private static final class ConsistentHashSelector {

        /**
         * 使用 TreeMap 存储虚拟节点（virtualServices 需要提供高效的查询操作，因此选用 TreeMap 作为存储结构）
         * TreeMap扩展知识……
         */
        private final TreeMap<Long, ServiceInfo> virtualServices;

        /**
         * 服务列表的原始哈希码
         */
        private final int identityHashCode;

        /**
         * @param serviceInfos
         * @param virtualNum
         * @param identityHashCode
         */
        public ConsistentHashSelector(List<ServiceInfo> serviceInfos, int virtualNum, int identityHashCode) {
            this.virtualServices = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for (ServiceInfo serviceInfo : serviceInfos) {
                String ip = serviceInfo.getIp();
                // virtualNum 要除4的原因是下面要进行 4次hash运算
                for (int i = 0; i < virtualNum / 4; i++) {
                    // 对 address + i 进行 md5 运算，得到一个长度为16的字节数组
                    byte[] digest = md5(ip + i);
                    // 对 digest 部分字节进行4次 hash 运算，得到四个不同的 long 型正整数
                    for (int h = 0; h < 4; h++) {
                        // 根据摘要字节数组生成不同的 hash 值（同一个服务节点储存多个，防止数据倾斜）
                        long hash = hash(digest, h);
                        // 将 hash 与 invoker 的映射关系存储到 virtualInvokers 中，
                        virtualServices.put(hash, serviceInfo);
                    }
                }
            }
        }

        /**
         * 进行 md5 运算，返回摘要字节数组
         *
         * @param key
         * @return
         */
        private byte[] md5(String key) {
            MessageDigest messageDigest;
            try {
                // 返回实现指定摘要算法的 MessageDigest 对象
                messageDigest = MessageDigest.getInstance("MD5");
                // 获取key的字节数组
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                // 使用指定的 byte 数组更新摘要
                messageDigest.update(keyBytes);

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            // 返回摘要字节数组，在调用此方法之后，摘要被重置
            return messageDigest.digest();
        }

        /**
         * 根据摘要字节数组生成 hash 值（调用时，index只会传入0，1，2，3）
         * index = 0 时，取 digest 中下标为 0 ~ 3 的4个字节进行位运算
         * index = 1 时，取 digest 中下标为 4 ~ 7 的4个字节进行位运算
         * index = 2 时，取 digest 中下标为 8 ~ 11 的4个字节进行位运算
         * index = 3 时，取 digest 中下标为 12 ~ 15 的4个字节进行位运算
         *
         * @param digest
         * @param index
         * @return
         */
        private long hash(byte[] digest, int index) {
            return (((long) (digest[3 + index * 4] & 0xFF) << 24) | ((long) (digest[2 + index * 4] & 0xFF) << 16) | ((long) (digest[1 + index * 4] & 0xFF) << 8) | (digest[index * 4] & 0xFF)) & 0xFFFFFFFFL;
        }

        /**
         * 根据key先进行md5 后进行hash计算
         * 根据hash 找到第一个大于等于 hash 值的服务信息，若没有则返回第一个
         *
         * @param key
         * @return
         */
        public ServiceInfo select(String key) {
            // 对key进行md5计算，并取 digest 数组的前四个字节进行 hash 运算
            byte[] digest = md5(key);
            long hash = hash(digest, 0);
            // 在 TreeMap 中查找第一个节点值 >= 当前hash 的服务方
            Map.Entry<Long, ServiceInfo> serviceEntry = virtualServices.ceilingEntry(hash);
            // 如果 serviceEntry 为空，说明 hash 大于 服务方节点 在圆环上最大的位置，将 TreeMap头节点 赋给serviceEntry（手动成环）
            if (serviceEntry == null) {
                serviceEntry = virtualServices.firstEntry();
            }
            return serviceEntry.getValue();
        }
    }
}
```

## 6.服务注册和发现

在分布式系统中，不同服务之间应该如何通信

- **传统的方式可以通过 HTTP 请求调用、保存服务端的服务列表等，这样做需要开发者主动感知到服务端暴露的信息，系统之间耦合严重**

为了更好地将客户端和服务端解耦，以及实现服务优雅上线和下线，使用注册中心

- 在 RPC 框架中，主要是**使用注册中心来实现服务注册和发现的功能**。服务端节点**上线后自行向注册中心注册服务列表**，节点**下线时需要从注册中心将节点元数据信息移除**。客户端向服务端发起调用时，自己负责**从注册中心获取服务端的服务列表，然后在通过负载均衡算法选择其中一个服务节点进行调用**
- 服务在下线时需要从注册中心移除元数据，那么**注册中心怎么才能感知到服务下线呢**？我们最先想到的方法就是**节点主动通知的实现方式**，当节点需要下线时，向注册中心发送下线请求，让注册中心移除自己的元数据信息。但是**如果节点异常退出，例如断网、进程崩溃等**，那么注册中心将会一直残留异常节点的元数据，从而可能造成服务调用出现问题。
- 实现服务优雅下线比较好的方式是**采用主动通知 + 心跳检测的方案**。除了主动通知注册中心下线外，还需要增加节点与注册中心的心跳检测功能，这个过程也叫作探活。**心跳检测可以由节点或者注册中心负责，例如注册中心可以向服务节点每 60s 发送一次心跳包，如果 3 次心跳包都没有收到请求结果，可以任务该服务节点已经下线。**

由此可见，采用注册中心的好处是可**以解耦客户端和服务端之间错综复杂的关系，并且能够实现对服务的动态管理**。服务**配置可以支持动态修改，然后将更新后的配置推送到客户端和服务端，无须重启任何服务。**

### 1.服务注册

#### 1.实现接口

```java
package com.mirrors.core.registry;

import com.alibaba.nacos.api.exception.NacosException;
import com.mirrors.core.dto.ServiceInfo;

/**
 * 服务注册接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 15:49
 */
@SPI
public interface ServiceRegistry {

    /**
     * 根据服务信息，向注册中心注册
     *
     * @param serviceInfo
     */
    void register(ServiceInfo serviceInfo) throws Exception;

    /**
     * 根据服务信息，从注册中心移除
     *
     * @param serviceInfo
     */
    void unregister(ServiceInfo serviceInfo) throws Exception;

    /**
     * 关闭和注册中心的连接
     */
    void disconnect() throws Exception;
}
```

#### 2.Nacos

```Java
package com.mirrors.core.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.registry.ServiceRegistry;
import com.mirrors.core.utils.ServiceUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * nacos注册中心
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 15:56
 */
@Slf4j
public class NacosServiceRegistry implements ServiceRegistry {

    /**
     * nacos 命名服务
     */
    private NamingService namingService;

    /**
     * 构造方法，根据传进的 nacos连接地址 初始化命名服务，如localhost:8848
     *
     * @param ipAndPort
     */
    public NacosServiceRegistry(String ipAndPort) {
        try {
            namingService = NamingFactory.createNamingService(ipAndPort);
        } catch (NacosException e) {
            throw new RuntimeException("connecting nacos fail", e);
        }
    }

    @Override
    public void register(ServiceInfo serviceInfo) {
        try {
            // 创建服务实例
            Instance instance = new Instance();
            instance.setServiceName(serviceInfo.getServiceName());
            instance.setIp(serviceInfo.getIp());
            instance.setPort(serviceInfo.getPort());
            instance.setHealthy(true);
            instance.setMetadata(ServiceUtil.toMap(serviceInfo));

            // 注册
            namingService.registerInstance(instance.getServiceName(), instance);
            log.info("registry success: {}", instance.getServiceName());

        } catch (NacosException e) {
            throw new RuntimeException("registry fail", e);
        }
    }

    @Override
    public void unregister(ServiceInfo serviceInfo) {
        try {
            // 创建服务实例
            Instance instance = new Instance();
            instance.setServiceName(serviceInfo.getServiceName());
            instance.setIp(serviceInfo.getIp());
            instance.setPort(serviceInfo.getPort());
            instance.setHealthy(true);
            instance.setMetadata(ServiceUtil.toMap(serviceInfo)); 

            // 移除
            namingService.deregisterInstance(instance.getServiceName(), instance);
            log.info("unregister success: {}", instance.getServiceName());

        } catch (NacosException e) {
            throw new RuntimeException("unregister fail", e);
        }
    }

    @Override
    public void disconnect() throws NacosException {
        namingService.shutDown();
        log.info("nacos disconnect");
    }
}
```

#### 3.ZooKeeper

```java
package com.mirrors.core.registry.zookeeper;

import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.IOException;

/**
 * zookeeper注册中心
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 15:58
 */
@Slf4j
public class ZooKeeperServiceRegistry implements ServiceRegistry {

    /**
     * 会话超时
     */
    private static final int SESSION_TIMEOUT = 60 * 1000;

    /**
     * 连接超过
     */
    private static final int CONNECT_TIMEOUT = 15 * 1000;

    /**
     * 初始sleep时间
     */
    private static final int BASE_SLEEP_TIME = 3 * 1000;

    /**
     * 最大重连次数
     */
    private static final int MAX_RETRY = 10;

    /**
     * 根路径
     */
    private static final String BASE_PATH = "/rpc_mirrors";

    /**
     * zookeeper客户端
     */
    private CuratorFramework client;

    /**
     * 服务发现
     */
    private ServiceDiscovery<ServiceInfo> serviceDiscovery;

    /**
     * 构造方法，根据传进的 zookeeper连接地址 初始化命名服务，如localhost:2181
     *
     * @param ipAndPort
     */
    public ZooKeeperServiceRegistry(String ipAndPort) {
        try {
            // 创建zookeeper客户端示例，并开始通信
            client = CuratorFrameworkFactory.builder()
                    .connectString(ipAndPort)
                    .sessionTimeoutMs(SESSION_TIMEOUT)
                    .connectionTimeoutMs(CONNECT_TIMEOUT)
                    .retryPolicy(new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRY))
                    .build();
            client.start();

            // 构建 ServiceDiscovery 服务注册中心，并开启
            serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceInfo.class)
                    .client(client)
                    .serializer(new JsonInstanceSerializer<>(ServiceInfo.class))
                    .basePath(BASE_PATH)
                    .build();
            serviceDiscovery.start();

        } catch (Exception e) {
            throw new RuntimeException("connecting zookeeper fail", e);
        }
    }

    @Override
    public void register(ServiceInfo serviceInfo) {
        try {
            // 创建服务实例
            ServiceInstance<ServiceInfo> serviceInstance = ServiceInstance.<ServiceInfo>builder()
                    .name(serviceInfo.getServiceName())
                    .address(serviceInfo.getIp())
                    .port(serviceInfo.getPort())
                    .payload(serviceInfo)
                    .build();
            // 注册
            serviceDiscovery.registerService(serviceInstance);
            log.info("registry success: {}", serviceInstance.getName());

        } catch (Exception e) {
            throw new RuntimeException("registry fail", e);
        }
    }

    @Override
    public void unregister(ServiceInfo serviceInfo) {
        try {
            // 创建服务实例
            ServiceInstance<ServiceInfo> serviceInstance = ServiceInstance.<ServiceInfo>builder()
                    .name(serviceInfo.getServiceName())
                    .address(serviceInfo.getIp())
                    .port(serviceInfo.getPort())
                    .payload(serviceInfo)
                    .build();
            // 移除
            serviceDiscovery.unregisterService(serviceInstance);
            log.info("unregister success: {}", serviceInstance.getName());

        } catch (Exception e) {
            throw new RuntimeException("unregister fail", e);
        }
    }

    @Override
    public void disconnect() throws IOException {
        serviceDiscovery.close();
        client.close();
        log.info("zookeeper disconnect");
    }
}
```

### 2.服务发现

#### 1.实现接口

```java
package com.mirrors.core.discover;

import com.alibaba.nacos.api.exception.NacosException;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务发现接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 19:45
 */
@SPI
public interface ServiceDiscover {

    /**
     * 进行服务发现
     *
     * @param rpcRequest
     * @return
     */
    ServiceInfo discover(RpcRequest rpcRequest);

    /**
     * 获取服务的所有提供者
     *
     * @param serviceName
     * @return
     */
    List<ServiceInfo> getServiceInfos(String serviceName) throws Exception;

    /**
     * 断开连接
     */
    void disconnect() throws Exception;
}
```

#### 2.Nacos

**增加 Nacos 服务本地缓存并监听**：解决了每次请求都需要访问 nacos 来进行服务发现，可以添加本地服务缓存功能，然后监听 nacos 服务节点的变化来动态更新本地服务列表

```java
package com.mirrors.core.discover.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.mirrors.core.discover.ServiceDiscover;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.loadbalance.LoadBalance;
import com.mirrors.core.utils.ServiceUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * nacos 服务发现
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 19:52
 */
@Slf4j
public class NacosServiceDiscover implements ServiceDiscover {

    /**
     * nacos 命名服务
     */
    private NamingService namingService;

    /**
     * 负载均衡算法
     */
    private LoadBalance loadBalance;

    /**
     * 将服务列表缓存到本地内存，当nacos挂掉后，可以继续提供服务
     */
    private final Map<String, List<ServiceInfo>> serviceMap = new ConcurrentHashMap<>();

    /**
     * 构造方法，传入 nacos连接地址 和 指定的负载算法算法
     *
     * @param ipAndPort
     * @param loadBalance
     */
    public NacosServiceDiscover(String ipAndPort, LoadBalance loadBalance) {
        try {
            this.loadBalance = loadBalance;
            namingService = NamingFactory.createNamingService(ipAndPort);
        } catch (NacosException e) {
            log.error("nacos connect fail", e);
        }
    }

    @Override
    public ServiceInfo discover(RpcRequest rpcRequest) {
        try {
            // 获取服务列表
            List<ServiceInfo> serviceInfos = getServiceInfos(rpcRequest.getServiceName());
            // 负载均衡，从服务列表中选择一个
            return loadBalance.select(serviceInfos, rpcRequest);

        } catch (Exception e) {
            throw new RuntimeException("nacos did not find service", e);
        }
    }

    @Override
    public List<ServiceInfo> getServiceInfos(String serviceName) throws NacosException {
        // 当 缓存 中没有找到当前服务名
        if (!serviceMap.containsKey(serviceName)) {
            // 从nacos获取服务列表，将 服务端提供的服务实例 全部映射为 ServiceInfo列表
            List<ServiceInfo> serviceInfos = namingService.getAllInstances(serviceName)
                    .stream()
                    .map(instance -> ServiceUtil.toServiceInfo(instance.getMetadata()))
                    .collect(Collectors.toList());
            // 加入缓存
            serviceMap.put(serviceName, serviceInfos);

            // 创建 指定服务名称 的监听事件，实时监听更新本地缓存缓存列表
            namingService.subscribe(serviceName, event -> {
                NamingEvent namingEvent = (NamingEvent) event;
                log.info("The service [{}] map changed. The current number of service instances is {}.", serviceName, namingEvent.getInstances().size());
                // 更新本地服务列表缓存
                List<ServiceInfo> serviceInfos1 = namingEvent.getInstances()
                        .stream()
                        .map(instance -> ServiceUtil.toServiceInfo(instance.getMetadata()))
                        .collect(Collectors.toList());
                serviceMap.put(namingEvent.getServiceName(), serviceInfos1);
            });
        }
        // 返回服务列表
        return serviceMap.get(serviceName);
    }

    @Override
    public void disconnect() throws NacosException {
        namingService.shutDown();
        log.info("nacos disconnect");
    }
}
```

#### 3.ZooKeeper

**增加 ZooKeeper 服务本地缓存并监听**：解决了每次请求都需要访问 zookeeper  来进行服务发现，可以添加本地服务缓存功能，然后监听 zookeeper   服务节点的变化来动态更新本地服务列表。

```java
package com.mirrors.core.discover.zookeeper;

import com.mirrors.core.discover.ServiceDiscover;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.loadbalance.LoadBalance;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceCache;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.details.ServiceCacheListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * zookeeper 服务发现
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 19:52
 */
@Slf4j
public class ZooKeeperServiceDiscover implements ServiceDiscover {

    /**
     * 会话超时
     */
    private static final int SESSION_TIMEOUT = 60 * 1000;

    /**
     * 连接超过
     */
    private static final int CONNECT_TIMEOUT = 15 * 1000;

    /**
     * 初始sleep时间
     */
    private static final int BASE_SLEEP_TIME = 3 * 1000;

    /**
     * 最大重连次数
     */
    private static final int MAX_RETRY = 10;

    /**
     * 根路径
     */
    private static final String BASE_PATH = "/rpc_mirrors";

    /**
     * 负载均衡算法
     */
    private LoadBalance loadBalance;

    /**
     * zookeeper客户端
     */
    private CuratorFramework client;

    /**
     * 服务发现
     */
    private ServiceDiscovery<ServiceInfo> serviceDiscovery;

    /**
     * ServiceCache：将服务数据缓存至本地，并监听服务变化，实时更新缓存
     * 服务缓存对象，从zookeeper获取列表缓存，构建本地服务缓存
     */
    private final Map<String, ServiceCache<ServiceInfo>> serviceCacheMap = new ConcurrentHashMap<>();

    /**
     * 将服务列表缓存到本地内存，当zookeeper挂掉后，可以继续提供服务
     */
    private final Map<String, List<ServiceInfo>> serviceMap = new ConcurrentHashMap<>();


    public ZooKeeperServiceDiscover(String ipAndPort, LoadBalance loadBalance) {
        try {
            this.loadBalance = loadBalance;
            // 创建zookeeper客户端，并开始连接
            client = CuratorFrameworkFactory.builder()
                    .connectString(ipAndPort)
                    .sessionTimeoutMs(SESSION_TIMEOUT)
                    .connectionTimeoutMs(CONNECT_TIMEOUT)
                    .retryPolicy(new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRY))
                    .build();
            client.start();

            // 构建服务注册中心，并开启
            serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceInfo.class)
                    .client(client)
                    .serializer(new JsonInstanceSerializer<>(ServiceInfo.class))
                    .basePath(BASE_PATH)
                    .build();
            serviceDiscovery.start();
        } catch (Exception e) {
            log.error("zookeeper connect fail", e);
        }
    }


    @Override
    public ServiceInfo discover(RpcRequest rpcRequest) {
        try {
            // 获取服务列表
            List<ServiceInfo> serviceInfos = getServiceInfos(rpcRequest.getServiceName());
            // 负载均衡，从服务列表中选择一个
            return loadBalance.select(serviceInfos, rpcRequest);

        } catch (Exception e) {
            throw new RuntimeException("zookeeper did not find service", e);
        }
    }

    @Override
    public List<ServiceInfo> getServiceInfos(String serviceName) throws Exception {
        // 当 缓存 中没有找到当前服务名
        if (!serviceMap.containsKey(serviceName)) {
            // 从zookeeper获取服务列表缓存，构建本地缓存
            ServiceCache<ServiceInfo> serviceCache = serviceDiscovery.serviceCacheBuilder()
                    .name(serviceName).build();
            // 添加服务监听，服务变化时，主动更新本地缓存并通知
            serviceCache.addListener(new ServiceCacheListener() {
                // 1.服务改变时
                @Override
                public void cacheChanged() {
                    log.info("The service [{}] map changed. The current number of service instances is {}.", serviceName, serviceCache.getInstances().size());
                    // 更新本地缓存列表
                    List<ServiceInfo> serviceInfos = serviceCache.getInstances().stream()
                            .map(ServiceInstance::getPayload)
                            .collect(Collectors.toList());
                    serviceMap.put(serviceName, serviceInfos);
                }

                // 连接状态改变时
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
            serviceMap.put(serviceName, serviceInfos);
        }
        return serviceMap.get(serviceName);
    }

    @Override
    public void disconnect() throws Exception {
        // 逐个关闭服务缓存对象
        for (ServiceCache<ServiceInfo> serviceCache : serviceCacheMap.values()) {
            if (serviceCache != null) {
                serviceCache.close();
            }
        }
        // 关闭注册中心
        if (serviceDiscovery != null) {
            serviceDiscovery.close();
        }
        // 关闭客户端连接
        if (client != null) {
            client.close();
        }
    }
}
```



## 7.SPI机制

SPI 的本质是将**接口实现类的全限定名配置在文件中**，并**由服务加载器读取配置文件**，**加载实现类**。这样可以在运行时，**动态为接口替换实现类**。正因此特性，我们可以很容易的通过 SPI 机制为我们的程序提供拓展功能 

参考：[Dubbo SPI | Apache Dubbo](https://cn.dubbo.apache.org/zh-cn/docsv2.7/dev/source/dubbo-spi/) 

































