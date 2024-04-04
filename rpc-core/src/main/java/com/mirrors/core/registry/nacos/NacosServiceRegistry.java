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
     * Nacos 命名服务，连接Nacos进行操作
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

    /**
     * 注册
     *
     * @param serviceInfo
     */
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

    /**
     * 移除
     *
     * @param serviceInfo
     */
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
