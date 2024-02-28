package com.mirrors.client.handler;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.client.communicate.RpcClient;
import com.mirrors.client.config.RpcClientProperties;
import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.bean.RpcMessageHeader;
import com.mirrors.core.discover.ServiceDiscover;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.dto.ServiceInfo;

import java.lang.reflect.Method;

/**
 * 远程调用的工具类，由客户端属性的动态代理进行调用
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 15:07
 */
public class RemoteCall {

    /**
     * 发起远程调用
     *
     * @param serviceDiscover 服务发现
     * @param rpcClient       客户端
     * @param serviceName     服务名称
     * @param properties      配置属性
     * @param method          调用方法名
     * @param args            方法参数
     * @return 返回方法调用结果
     */
    public static Object remoteCall(ServiceDiscover serviceDiscover, RpcClient rpcClient, String serviceName, RpcClientProperties properties, Method method, Object[] args) {
        // 构建请求头
        RpcMessageHeader header = RpcMessageHeader.createBySerializer(properties.getSerialization());
        // 构建请求体（RpcRequest作为请求体）
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceName(serviceName);
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setParamTypes(method.getParameterTypes());
        rpcRequest.setParamValues(args);

        // 服务发现，从服务列表负载均衡选择一个返回
        ServiceInfo serviceInfo = serviceDiscover.discover(rpcRequest);
        if (serviceInfo == null) {
            throw new RuntimeException(String.format("The service [%s] was not found in the remote registry center", serviceName));
        }

        // 构建Rpc发送的信息
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setRpcMessageHeader(header);
        rpcMessage.setRpcMessageBody(rpcRequest);

        // 构建请求元数据
        RequestMetadata metadata = RequestMetadata.builder()
                .rpcMessage(rpcMessage)
                .serverIp(serviceInfo.getIp())
                .serverPort(serviceInfo.getPort())
                .timeout(properties.getTimeout())
                .build();

        // 发送网络请求，返回结果
        RpcMessage responseRpcMessage = rpcClient.sendRpcRequest(metadata);
        if (responseRpcMessage == null) {
            throw new RuntimeException("Remote procedure call timeout");
        }

        // 拿到 消息正文
        RpcResponse response = (RpcResponse) responseRpcMessage.getRpcMessageBody();
        if (response.getExceptionValue() != null) {
            // 远程调用 发生错误
            throw new RuntimeException(response.getExceptionValue());
        }

        // 返回 远程调用响应结果
        return response.getReturnValue();
    }
}
