package com.mirrors.server.communicate;

/**
 * rpc服务类， 接受客户端信息，调用客户端请求的方法，返回结果给客户端
 * 具体由子类实现
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 20:54
 */
public interface RpcServer {

    /**
     * 开启服务
     *
     * @param port
     */
    void start(Integer port);
}
