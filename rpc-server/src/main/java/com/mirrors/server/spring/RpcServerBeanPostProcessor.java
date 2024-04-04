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
            // 5.调用Nacos或Zk，进行服务注册
            serviceRegistry.register(serviceInfo);
            // 6.TODO 服务端创建 服务 的本地缓存？【注意：key是服务名称，value是Bean实例】
            ServiceLocalCache.addService(serviceKey, bean);
        }
        return bean;
    }
}
