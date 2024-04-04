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
