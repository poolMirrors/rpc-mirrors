package com.mirrors.client.communicate.socket;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.client.communicate.RpcClient;
import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.dto.RpcResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 基于socket的客户端；
 * 发送接受的数据类型为 RpcRequest 和 RpcResponse（从RpcMetadata中获取）
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 17:06
 */
public class SocketRpcClient implements RpcClient {
    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        // 获取服务地址和端口
        InetSocketAddress socketAddress = new InetSocketAddress(requestMetadata.getServerIp(), requestMetadata.getServerPort());
        try (Socket socket = new Socket()) {
            // 利用socket与服务端连接
            socket.connect(socketAddress);
            // socket输出流 发送RpcRequest（从requestMetadata中取）
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(requestMetadata.getRpcMessage().getRpcMessageBody());
            outputStream.flush();
            // 阻塞等待 服务端响应数据
            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            RpcResponse rpcResponse = (RpcResponse) inputStream.readObject();
            // 封装成 RpcMessage 对象返回给 调用者
            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setRpcMessageBody(rpcResponse);
            return rpcMessage;

        } catch (Exception e) {
            throw new RuntimeException("the socket client failed to read or write", e);
        }
    }
}
