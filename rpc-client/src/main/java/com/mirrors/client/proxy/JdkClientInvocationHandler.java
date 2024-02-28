package com.mirrors.client.proxy;

import com.mirrors.client.communicate.RpcClient;
import com.mirrors.client.config.RpcClientProperties;
import com.mirrors.client.handler.RemoteCall;
import com.mirrors.core.discover.ServiceDiscover;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 基于 JDK 动态代理；implements InvocationHandler
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
