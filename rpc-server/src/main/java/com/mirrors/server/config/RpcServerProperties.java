package com.mirrors.server.config;

import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 服务配置类，要提供getter和setter，注入属性；
 * 提供给 server端 使用（读取properties文件注入到类属性中）；
 * <a href="https://blog.csdn.net/gs_albb/article/details/85019466">ConfigurationProperties</a>
 *
 * <li>
 * `@ConfigurationProperties`：
 * 使用@ConfigurationProperties的时候，把配置类的属性与yml配置文件绑定起来的时候，
 * 还需要加上@Component注解才能绑定并注入IOC容器中，若不加上@Component，则会无效。
 * </li>
 * <li>
 * `@EnableConfigurationProperties`：
 * 将让使用了 @ConfigurationProperties 注解的配置类生效,将该类注入到 IOC 容器中,交由 IOC 容器进行管理，
 * 此时则不用再配置类上加上@Component。
 * </li>
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 20:57
 */
@Data
@ConfigurationProperties(prefix = "rpc.server") // 指定前缀
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
     * 应用名称，如provider-1
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
    public RpcServerProperties() throws UnknownHostException {
        this.ip = InetAddress.getLocalHost().getHostAddress();
        port = 8080;
        appName = "provider-1";
        registry = "zookeeper";
        communicate = "netty";
        registryIpAndPort = "127.0.0.1:2181";
    }
}
