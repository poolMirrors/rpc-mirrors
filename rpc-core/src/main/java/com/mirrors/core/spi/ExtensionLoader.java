package com.mirrors.core.spi;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加载 被SPI注解的接口 的所有扩展类；
 * todo: 接口扩展类的加载？
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 9:24
 */
@Slf4j
public class ExtensionLoader<T> {

    /**
     * 接口扩展类的目录
     */
    private static final String SERVICES_DIRECTORY = "META-INF/spi/";

    /**
     * 扩展类的扩展器缓存（扩展类的加载器）
     * key：Class
     * value：Class对应的扩展器类
     */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /**
     * 存储接口实现类的实例
     * key：Class
     * value：实例对象
     */
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    /**
     * 拓展类加载器对应的接口类型
     */
    private final Class<?> type;

    /**
     * 缓存的实例
     * key：
     * value：Object
     */
    private final Map<String, Object> cachedInstances = new ConcurrentHashMap<>();

    /**
     * 缓存的类型，当前接口的所有 Extension 类型
     * key：
     * value：Class
     */
    private final Map<String, Class<?>> cachedClasses = new ConcurrentHashMap<>();

    /**
     * 私有构造方法
     *
     * @param type
     */
    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    /**
     * 获取指定接口类型的拓展类加载器
     *
     * @param type
     * @param <T>
     * @return
     */
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("extension type is null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException(String.format("extension type (%s) is not an interface", type));
        }
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException(String.format("it is not annotated with @%s!", SPI.class.getSimpleName()));
        }
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            // 如果加载器为空，新建该类加载器
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

}
