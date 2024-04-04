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
@Deprecated
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
     * sleep基础时间
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
     * 服务发现（org.apache.curator.x.discovery自带）
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

    /**
     * 注册
     *
     * @param serviceInfo
     */
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

    /**
     * 移除
     *
     * @param serviceInfo
     */
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

    /**
     * 断开连接
     *
     * @throws IOException
     */
    @Override
    public void disconnect() throws IOException {
        serviceDiscovery.close();
        client.close();
        log.info("zookeeper disconnect");
    }
}
