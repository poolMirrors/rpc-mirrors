package com.mirrors.core.loadbalance;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.spi.SPI;

import java.util.List;

/**
 * 负载均衡接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:26
 */
@SPI
public interface LoadBalance {

    /**
     * 从 ServiceInfo的列表中（服务端列表）中选择一个服务方进行返回
     *
     * @param serviceInfoList
     * @param rpcRequest
     * @return
     */
    ServiceInfo select(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest);
}
