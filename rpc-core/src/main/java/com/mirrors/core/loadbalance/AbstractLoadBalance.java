package com.mirrors.core.loadbalance;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;

import java.util.List;

/**
 * 负载均衡抽象类，重写接口扩展，接口内调用doSelect，让子类调用doSelect
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:37
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    /**
     * 重写接口的select方法，进行判断，最后让子类实现服务选择
     *
     * @param serviceInfoList
     * @param rpcRequest
     * @return
     */
    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest) {
        // 服务列表为空，返回null
        if (serviceInfoList == null || serviceInfoList.isEmpty()) {
            return null;
        }
        // 只有一个服务，直接返回
        if (serviceInfoList.size() == 1) {
            return serviceInfoList.get(0);
        }
        // 多个服务；让子类实现doSelect，进行服务选择
        return doSelect(serviceInfoList, rpcRequest);
    }

    /**
     * 由子类，真正执行负载均衡（选择哪个服务）
     *
     * @param serviceInfoList
     * @param rpcRequest
     * @return
     */
    protected abstract ServiceInfo doSelect(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest);
}
