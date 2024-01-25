package com.mirrors.client.communicate.http;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.client.communicate.RpcClient;
import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.dto.RpcResponse;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

/**
 * 基于http的客户端；
 * 发送接受的数据类型为 RpcRequest 和 RpcResponse（从RpcMetadata中获取）
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 17:06
 */
public class HttpRpcClient implements RpcClient {
    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        try {
            // 构建 http 请求
            URL url = new URL("http", requestMetadata.getServerIp(), requestMetadata.getServerPort(), "/");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);

            // 发送请求
            OutputStream connectionOutputStream = httpURLConnection.getOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(connectionOutputStream);
            outputStream.writeObject(requestMetadata.getRpcMessage().getRpcMessageBody());
            outputStream.flush();
            outputStream.close();

            // 构建接受响应数据的输入流
            RpcMessage rpcMessage = new RpcMessage();
            InputStream connectionInputStream = httpURLConnection.getInputStream();
            ObjectInputStream inputStream = new ObjectInputStream(connectionInputStream);

            // 阻塞读取
            RpcResponse rpcResponse = (RpcResponse) inputStream.readObject();
            rpcMessage.setRpcMessageBody(rpcResponse);

            return rpcMessage;

        } catch (Exception e) {
            throw new RuntimeException("the http client failed to read or write", e);
        }
    }
}
