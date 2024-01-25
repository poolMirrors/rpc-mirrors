package com.mirrors.core.loadbalance.impl;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.loadbalance.AbstractLoadBalance;

import java.util.List;
import java.util.Random;

/**
 * 随机策略
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:44
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    @Override
    protected ServiceInfo doSelect(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest) {
        int size = serviceInfoList.size();
        // 生成[0,size-1]之间的随机数
        return serviceInfoList.get(new Random().nextInt(size));
    }

}
