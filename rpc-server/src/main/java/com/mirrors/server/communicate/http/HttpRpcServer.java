package com.mirrors.server.communicate.http;

import com.mirrors.server.communicate.RpcServer;
import org.apache.catalina.Context;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;

import java.net.InetAddress;

/**
 * 基于 http 通信
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 22:00
 */
public class HttpRpcServer implements RpcServer {

    /**
     * 创建一个简单的Tomcat服务器，并配置了一个简单的Web应用
     *
     * @param port
     */
    @Override
    public void start(Integer port) {
        try {
            // 创建Tomcat实例
            Tomcat tomcat = new Tomcat();
            // 获取Tomcat的服务器实例
            Server server = tomcat.getServer();
            // 获取服务器中Tomcat的服务
            Service service = server.findService("Tomcat");

            // 获取本机的IP地址作为主机名
            String localhost = InetAddress.getLocalHost().getHostAddress();
            // 创建Connector实例，处理网络连接
            Connector connector = new Connector();
            connector.setPort(port);

            // 创建Tomcat的引擎
            StandardEngine engine = new StandardEngine();
            engine.setDefaultHost(localhost);
            // 创建Tomcat的主机
            StandardHost host = new StandardHost();
            host.setName(localhost);

            // 设置Context的路径
            String contextPath = "";
            // 创建Web应用的上下文
            Context context = new StandardContext();
            context.setPath(contextPath);
            // 添加生命周期监听器
            context.addLifecycleListener(new Tomcat.FixContextListener());

            // 将Context添加到Host中
            host.addChild(context);
            // 将Host添加到Engine中
            engine.addChild(host);

            // 将Engine设置到Service中
            service.setContainer(engine);
            // 将Connector添加到Service中
            service.addConnector(connector);

            // 向Tomcat添加Servlet实例，如自定义DispatcherServlet
            tomcat.addServlet(contextPath, "dispatcher", new DispatcherServlet());
            // 将Servlet映射到路径"/*"
            context.addServletMappingDecoded("/*", "dispatcher");

            // 启动Tomcat
            tomcat.start();
            // 等待Tomcat服务器的关闭
            tomcat.getServer().await();

        } catch (Exception e) {
            throw new RuntimeException(String.format("The http server failed to start on port %d.", port), e);
        }

    }
}
