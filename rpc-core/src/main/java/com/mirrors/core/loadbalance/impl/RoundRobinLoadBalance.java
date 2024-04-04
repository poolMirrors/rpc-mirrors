package com.mirrors.core.loadbalance.impl;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.loadbalance.AbstractLoadBalance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询策略
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:46
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    /**
     * 利用 AtomicInteger 记录轮询的计数，初始化为0
     */
    private static final AtomicInteger atomicInteger = new AtomicInteger(0);

    /**
     * 返回当前值并加一，通过 CAS 原子更新
     * 当 atomicInteger 的值来到 Integer.MAX_VALUE 时，重新置0（解决AtomicInteger越界问题）
     *
     * @return
     */
    private final int getAndIncrement() {
        int current, next;
        do {
            current = atomicInteger.get();
            next = (current >= Integer.MAX_VALUE ? 0 : current + 1);
            // current不变，要是 CAS 成功就退出，返回current
        } while (!atomicInteger.compareAndSet(current, next));
        return current;
    }

    @Override
    protected ServiceInfo doSelect(List<ServiceInfo> serviceInfoList, RpcRequest rpcRequest) {
        int size = serviceInfoList.size();
        return serviceInfoList.get(getAndIncrement() % size);
    }

}
