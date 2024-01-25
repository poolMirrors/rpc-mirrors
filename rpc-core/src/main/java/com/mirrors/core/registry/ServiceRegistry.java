package com.mirrors.core.registry;

import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.spi.SPI;

/**
 * 服务注册接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 15:49
 */
@SPI
public interface ServiceRegistry {

    /**
     * 根据服务信息，向注册中心注册
     *
     * @param serviceInfo
     */
    void register(ServiceInfo serviceInfo) throws Exception;

    /**
     * 根据服务信息，从注册中心移除
     *
     * @param serviceInfo
     */
    void unregister(ServiceInfo serviceInfo) throws Exception;

    /**
     * 关闭和注册中心的连接
     */
    void disconnect() throws Exception;
}