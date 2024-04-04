package com.mirrors.server.communicate.socket;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.factory.SingletonFactory;
import com.mirrors.server.handler.RpcRequestHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 基于 socket 的请求消息处理器，实现Runnable接口，可以多线程
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 20:06
 */
@Slf4j
public class SocketRpcRequestHandler implements Runnable {

    private final Socket socket;
    /**
     * 反射调用 服务的本地缓存，返回结果
     */
    private final RpcRequestHandler rpcRequestHandler;

    public SocketRpcRequestHandler(Socket socket) {
        this.socket = socket;
        rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    /**
     * 注意 客户端 发送的是 RpcRequest，所以服务端发送RpcResponse，
     * 无需消息协议
     */
    @Override
    public void run() {
        // 得到socket输入流，try自动关闭；得到socket输出流
        try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {

            // 直接强转，不需要编解码和消息协议
            RpcRequest rpcRequest = (RpcRequest) inputStream.readObject();
            log.debug("The socket server received message is {}.", rpcRequest);
            // 返回RpcResponse
            RpcResponse rpcResponse = new RpcResponse();
            try {
                // 处理请求信息，调用 服务的本地缓存，返回结果
                Object result = rpcRequestHandler.invokeService(rpcRequest);
                rpcResponse.setReturnValue(result);

            } catch (Exception e) {
                log.error("The service [{}], the method [{}] invoke failed!", rpcRequest.getServiceName(), rpcRequest.getMethodName());
                // 压缩错误信息，不然堆栈信息过多，导致报错
                rpcResponse.setExceptionValue(new RuntimeException("Error in remote procedure call, " + e.getMessage()));
            }
            log.debug("response: {}.", rpcResponse);
            outputStream.writeObject(rpcResponse);

        } catch (Exception e) {
            throw new RuntimeException("The socket server failed to handle rpc request", e);
        }
    }
}
