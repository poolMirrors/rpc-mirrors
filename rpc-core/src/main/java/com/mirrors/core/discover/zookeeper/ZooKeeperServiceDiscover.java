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
@Deprecated
public class ZooKeeperServiceDiscover implements ServiceDiscover {

    /**
     * 会话超时
     */
    private static final int SESSION_TIMEOUT = 60 * 1000;

    /**
     * 连接超时
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
     * 负载均衡算法
     */
    private LoadBalance loadBalance;

    /**
     * zookeeper客户端
     */
    private CuratorFramework client;

    /**
     * 服务发现（org.apache.curator.x.discovery自带）
     */
    private ServiceDiscovery<ServiceInfo> serviceDiscovery;

    /**
     * ServiceCache：将 【服务】 数据缓存至本地，并监听服务变化，实时更新缓存；
     * 服务缓存对象，从zookeeper获取列表缓存，构建本地服务缓存
     */
    private final Map<String, ServiceCache<ServiceInfo>> serviceCacheMap = new ConcurrentHashMap<>();

    /**
     * 将 【服务列表】 缓存到本地内存，【当zookeeper挂掉】后，可以继续提供服务
     */
    private final Map<String, List<ServiceInfo>> serviceListMap = new ConcurrentHashMap<>();


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

    /**
     * 服务发现
     *
     * @param rpcRequest
     * @return
     */
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

    /**
     * 解决了每次请求都需要访问 zk 来进行服务发现，
     * 可以添加本地服务缓存功能，然后【监听 zk 服务节点】的变化来动态更新本地服务列表。
     *
     * @param serviceName
     * @return
     * @throws Exception
     */
    @Override
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

    /**
     * 断开
     *
     * @throws Exception
     */
    @Override
    public void disconnect() throws Exception {
        // 逐个关闭本地服务缓存对象（关闭监听更新）
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
