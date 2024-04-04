package com.mirrors.server.communicate.socket;

import com.mirrors.server.communicate.RpcServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于socket通信
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 20:21
 */
@Slf4j
public class SocketRpcServer implements RpcServer {

    /**
     * 当前可用处理器数量（逻辑数量， 8 内核？）
     */
    private final int cpuNum = Runtime.getRuntime().availableProcessors();

    /**
     * 线程池的线程数大小，看我们执行的任务是cpu密集型，还是io密集型；
     * 如果是计算，cpu密集型，线程大小应该设置为：cpuNum + 1；
     * 如果是网络传输，数据库等，io密集型，cpuNum * 2
     */
    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(cpuNum * 2, cpuNum * 2, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));

    @Override
    public void start(Integer port) {
        // 建立ServerSocket连接，try自动关闭
        try (ServerSocket serverSocket = new ServerSocket()) {
            // 获取本地ip
            String localhost = InetAddress.getLocalHost().getHostAddress();
            // ServerSocket绑定端口
            serverSocket.bind(new InetSocketAddress(localhost, port));
            // ServerSocket循环接受socket连接，socket为空时，没有连接
            Socket socket;
            while ((socket = serverSocket.accept()) != null) {
                log.debug("The client connected [{}].", socket.getInetAddress());
                // 线程池异步执行 信息处理
                threadPool.execute(new SocketRpcRequestHandler(socket));
            }
            // 服务端口连接断开，关闭线程池
            threadPool.shutdown();

        } catch (Exception e) {
            throw new RuntimeException(String.format("The socket server failed to start on port %d.", port), e);
        }
    }
}
