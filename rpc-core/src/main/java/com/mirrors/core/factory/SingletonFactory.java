package com.mirrors.core.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单例模式，获取单例对象
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 21:07
 */
public final class SingletonFactory {

    /**
     * 保持 每个全限定类名 对应的 单例对象
     */
    private static final Map<String, Object> SINGLETON_MAP = new ConcurrentHashMap<>();

    /**
     * 根据 类 获取单例对象
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getInstance(Class<T> clazz) {
        try {
            // 获取全限定类名
            String name = clazz.getName();
            if (SINGLETON_MAP.containsKey(name)) {
                // 单例已经存在，clazz强转返回
                return clazz.cast(SINGLETON_MAP.get(name));
            } else {
                // 单例不存在，利用反射调用构造函数创建
                T instance = clazz.getDeclaredConstructor().newInstance();
                SINGLETON_MAP.put(name, instance);
                return instance;
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }

}
