package com.mirrors.client.spring;

import com.mirrors.core.discover.ServiceDiscover;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

/**
 * 实现DisposableBean接口，在客户端退出后，释放资源
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 16:07
 */
@Slf4j
public class RpcClientExitDisposableBean implements DisposableBean {

    /**
     * 服务发现
     */
    private final ServiceDiscover serviceDiscover;

    public RpcClientExitDisposableBean(ServiceDiscover serviceDiscover) {
        this.serviceDiscover = serviceDiscover;
    }

    /**
     * 客户端退出后的操作
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        try {
            if (serviceDiscover != null) {
                serviceDiscover.disconnect();
            }
            log.info("Rpc client resource released and exited successfully.");
        } catch (Exception e) {
            log.error("An exception occurred while destroying: {}.", e.getMessage());
        }
    }
}
