package com.mirrors.server.communicate.http;

import com.mirrors.core.factory.SingletonFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 用于接受http请求
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 21:05
 */
public class DispatcherServlet extends HttpServlet {

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

    /**
     * service方法，当客户端进行访问服务端时就执行
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 创建 HttpRpcRequestHandler 的单例
        HttpRpcRequestHandler httpRpcRequestHandler = SingletonFactory.getInstance(HttpRpcRequestHandler.class);
        // 接受到http请求就 使用线程池调用 消息处理器
        threadPool.submit(new Thread(() -> httpRpcRequestHandler.handle(request, response)));
    }
}
