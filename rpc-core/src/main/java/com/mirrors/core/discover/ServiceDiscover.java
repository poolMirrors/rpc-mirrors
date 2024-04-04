package com.mirrors.core.discover;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.spi.SPI;

import java.util.List;

/**
 * 服务发现接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 19:45
 */
@SPI
public interface ServiceDiscover {

    /**
     * 进行服务发现
     *
     * @param rpcRequest
     * @return
     */
    ServiceInfo discover(RpcRequest rpcRequest);

    /**
     * 获取服务的所有提供者
     *
     * @param serviceName
     * @return
     */
    List<ServiceInfo> getServiceInfos(String serviceName) throws Exception;

    /**
     * 断开连接
     */
    void disconnect() throws Exception;
}
