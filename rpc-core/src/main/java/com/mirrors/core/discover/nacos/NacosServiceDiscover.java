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
     * 将服务列表缓存到本地内存，当 nacos 挂掉后，可以继续提供服务
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
            namingService = NamingFactory.createNamingService(ipAndPort);
            this.loadBalance = loadBalance;
        } catch (NacosException e) {
            log.error("nacos connect fail", e);
        }
    }

    /**
     * 进行服务发现
     *
     * @param rpcRequest
     * @return
     */
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

    /**
     * 获取服务的所有提供者
     *
     * @param serviceName
     * @return
     * @throws NacosException
     */
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

    /**
     * 断开连接
     *
     * @throws NacosException
     */
    @Override
    public void disconnect() throws NacosException {
        namingService.shutDown();
        log.info("nacos disconnect");
    }
}
