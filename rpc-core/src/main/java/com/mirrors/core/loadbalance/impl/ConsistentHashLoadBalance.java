package com.mirrors.core.loadbalance.impl;

import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.ServiceInfo;
import com.mirrors.core.loadbalance.AbstractLoadBalance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 9:46
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    /**
     * 服务调用时key 与 实现一致性哈希的内部类 的映射缓存
     * key：服务名 和 方法名
     */
    private final Map<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();


    @Override
    protected ServiceInfo doSelect(List<ServiceInfo> serviceInfos, RpcRequest rpcRequest) {
        // 根据 服务名 和 方法名 构建key
        String key = rpcRequest.getServiceName() + "." + rpcRequest.getMethodName();
        // 获取 服务列表 的原始hash值
        int identityHashCode = System.identityHashCode(serviceInfos);
        // 根据 key 从TreeMap中找到 selector
        ConsistentHashSelector selector = selectors.get(key);
        // 如果 selector为空 或 hash值不一样，说明缓存不存在或失效，则缓存的服务列表发生变化（服务列表个数可能增加减少）
        if (selector == null || selector.identityHashCode != identityHashCode) {
            // 创建新的服务列表缓存
            selectors.put(key, new ConsistentHashSelector(serviceInfos, 160, identityHashCode));
            selector = selectors.get(key);
        }
        // 根据 方法参数值 和 key 构造一致性哈希计算的selectKey
        String selectKey = key;
        if (rpcRequest.getParamValues() != null && rpcRequest.getParamValues().length > 0) {
            selectKey += Arrays.stream(rpcRequest.getParamValues()).toString();
        }
        // 将 key 与 方法参数 进行hash运算，因此 ConsistentHashLoadBalance 的负载均衡逻辑只受参数值影响，且不关系权重
        // 具有相同参数值的请求将会被分配给同一个服务提供者
        return selector.select(selectKey);
    }

    /**
     * 实现一致性哈希的内部类；
     * 一个服务，对应多个<Long, ServiceInfo>，Long不同，ServiceInfo相同
     */
    private static final class ConsistentHashSelector {

        /**
         * 使用 TreeMap 存储虚拟节点（virtualServices 需要提供高效的查询操作，因此选用 TreeMap 作为存储结构）
         * TreeMap扩展知识……
         */
        private final TreeMap<Long, ServiceInfo> virtualServices;

        /**
         * 服务列表的原始哈希码
         */
        private final int identityHashCode;

        /**
         * 构造函数，先对节点进行一致性哈希，记录下来；
         * 一个服务共有virtualNum个节点 = virtualNum/4个不同的digest + 每个digest有4个hash
         *
         * @param serviceInfos
         * @param virtualNum
         * @param identityHashCode
         */
        public ConsistentHashSelector(List<ServiceInfo> serviceInfos, int virtualNum, int identityHashCode) {
            this.virtualServices = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for (ServiceInfo serviceInfo : serviceInfos) {
                String ip = serviceInfo.getIp();
                // virtualNum 要除4的原因是下面要进行 4次hash运算
                for (int i = 0; i < virtualNum / 4; i++) {
                    // 对 ip + i 进行 md5 运算，得到一个长度为16的字节数组
                    byte[] digest = md5(ip + i);
                    // 对 digest 部分字节进行4次 hash 运算，得到四个不同的 long 型正整数
                    for (int h = 0; h < 4; h++) {
                        // 根据摘要字节数组生成不同的 hash 值（同一个服务节点储存多个，防止数据倾斜）
                        long hash = hash(digest, h);
                        // 将 hash 与 invoker 的映射关系存储到 virtualInvokers 中，
                        virtualServices.put(hash, serviceInfo);
                    }
                }
            }
        }

        /**
         * 进行 md5 运算，返回摘要字节数组
         *
         * @param key
         * @return
         */
        private byte[] md5(String key) {
            MessageDigest messageDigest;
            try {
                // 返回实现指定摘要算法的 MessageDigest 对象
                messageDigest = MessageDigest.getInstance("MD5");
                // 获取key的字节数组
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                // 使用指定的 byte 数组更新摘要
                messageDigest.update(keyBytes);

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            // 返回摘要字节数组，在调用此方法之后，摘要被重置
            return messageDigest.digest();
        }

        /**
         * 根据摘要字节数组生成 hash 值（调用时，index只会传入0，1，2，3）
         * index = 0 时，取 digest 中下标为 0 ~ 3 的4个字节进行位运算
         * index = 1 时，取 digest 中下标为 4 ~ 7 的4个字节进行位运算
         * index = 2 时，取 digest 中下标为 8 ~ 11 的4个字节进行位运算
         * index = 3 时，取 digest 中下标为 12 ~ 15 的4个字节进行位运算
         *
         * @param digest
         * @param index
         * @return
         */
        private long hash(byte[] digest, int index) {
            // 十六进制，1个F == 二进制，4个1
            return (((long) (digest[3 + index * 4] & 0xFF) << 24) | // 25-32位
                    ((long) (digest[2 + index * 4] & 0xFF) << 16) | // 17-24位
                    ((long) (digest[1 + index * 4] & 0xFF) << 8) | // 9-16位
                    (digest[index * 4] & 0xFF)) // 1-8位
                    // 32个1
                    & 0xFFFFFFFFL;
        }

        /**
         * 根据key先进行md5 后进行hash计算
         * 根据hash 找到第一个大于等于 hash 值的服务信息，若没有则返回第一个
         *
         * @param key
         * @return
         */
        public ServiceInfo select(String key) {
            // 对key进行md5计算，并取 digest 数组的前四个字节进行 hash 运算
            byte[] digest = md5(key);
            long hash = hash(digest, 0);
            // 在 TreeMap 中查找第一个节点值 >= 当前hash 的服务方
            Map.Entry<Long, ServiceInfo> serviceEntry = virtualServices.ceilingEntry(hash);
            // 如果 serviceEntry 为空，说明 hash 大于 服务方节点 在圆环上最大的位置，将 TreeMap第一个节点 赋给serviceEntry（手动成环）
            if (serviceEntry == null) {
                serviceEntry = virtualServices.firstEntry();
            }
            return serviceEntry.getValue();
        }
    }
}
