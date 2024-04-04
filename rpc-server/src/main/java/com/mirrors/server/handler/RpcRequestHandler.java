package com.mirrors.server.handler;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.server.cache.ServiceLocalCache;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 根据 RpcRequest 中的信息，反射调用本地服务实例
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 15:16
 */
@Slf4j
public class RpcRequestHandler {

    /**
     * 处理 RpcRequest，反射调用 ServiceLocalCache 中的本地缓存，返回调用结果
     *
     * @param rpcRequest
     * @return
     */
    public Object invokeService(RpcRequest rpcRequest) throws Exception {
        // 获取服务实例
        Object service = ServiceLocalCache.getService(rpcRequest.getServiceName());
        if (service == null) {
            throw new RuntimeException(String.format("the service [%s] is not exist", rpcRequest.getServiceName()));
        }
        // 获取 RpcRequest请求 要调用的方法
        Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
        // 调用方法，返回结果
        Object result = method.invoke(service, rpcRequest.getParamValues());

        return result;
    }

}
