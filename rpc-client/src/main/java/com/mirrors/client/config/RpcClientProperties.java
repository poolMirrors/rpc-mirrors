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
        this.loadBalance = "roundRobin";
        this.serialization = "hessian";
        this.communicate = "netty";
        this.registry = "zookeeper";
        this.registryIpAndPort = "127.0.0.1:2181";
        this.timeout = 5000;
    }
}
