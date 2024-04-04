package com.mirrors.server.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务本地缓存，将服务实体类bean缓存在本地
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 22:42
 */
@Slf4j
public class ServiceLocalCache {
    /**
     * 服务本地缓存<服务名, 对应的bean>
     */
    private static final Map<String, Object> serviceLocalCacheMap = new ConcurrentHashMap<>();

    /**
     * 添加服务到本地缓存
     *
     * @param serviceKey
     * @param object
     */
    public static void addService(String serviceKey, Object object) {
        serviceLocalCacheMap.put(serviceKey, object);
        log.info("[{}] was successfully added to the local cache.", serviceKey);
    }

    /**
     * 获取服务的本地缓存实体类
     *
     * @param serviceKey
     * @return
     */
    public static Object getService(String serviceKey) {
        return serviceLocalCacheMap.get(serviceKey);
    }

    /**
     * 删除本地缓存的实体类
     *
     * @param serviceKey
     */
    public static void removeService(String serviceKey) {
        serviceLocalCacheMap.remove(serviceKey);
        log.info("[{}] was removed from local cache", serviceKey);
    }
}
