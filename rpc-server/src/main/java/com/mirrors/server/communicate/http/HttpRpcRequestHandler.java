package com.mirrors.server.communicate.http;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.factory.SingletonFactory;
import com.mirrors.server.handler.RpcRequestHandler;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 基于http连接的消息处理器
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 21:14
 */
@Slf4j
public class HttpRpcRequestHandler {

    /**
     * 反射调用 服务的本地缓存，返回结果
     */
    private final RpcRequestHandler rpcRequestHandler;

    public HttpRpcRequestHandler() {
        rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    /**
     * 处理 http请求信息
     *
     * @param request
     * @param response
     */
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        // 拿到 request和response 的输入输出流
        try (ObjectInputStream inputStream = new ObjectInputStream(request.getInputStream());
             ObjectOutputStream outputStream = new ObjectOutputStream(response.getOutputStream())) {

            // 输入流读取客户端请求，不需要编解码和自定义信息协议
            RpcRequest rpcRequest = (RpcRequest) inputStream.readObject();
            log.debug("The http server received message is {}.", rpcRequest);
            // 响应RpcResponse
            RpcResponse rpcResponse = new RpcResponse();
            try {
                // 反射调用方法
                Object result = rpcRequestHandler.invokeService(rpcRequest);
                rpcResponse.setReturnValue(result);

            } catch (Exception e) {
                log.error("The service [{}], the method [{}] invoke failed!", rpcRequest.getServiceName(), rpcRequest.getMethodName());
                // 压缩错误信息，不然堆栈信息过多，导致报错
                rpcResponse.setExceptionValue(new RuntimeException("Error in remote procedure call, " + e.getMessage()));
            }
            // 输出流
            log.debug("response: {}.", rpcResponse);
            outputStream.writeObject(rpcResponse);

        } catch (Exception e) {
            throw new RuntimeException("The http server failed to handle rpc request", e);
        }
    }
}
