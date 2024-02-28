package com.mirrors.core.serializer;

import com.mirrors.core.spi.SPI;

/**
 * 序列化算法接口，所有序列哈算法都实现接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:26
 */
// SPI 的本质是将接口实现类的全限定名配置在文件中，并由服务加载器读取配置文件，加载实现类，动态为接口替换实现类。
// @SPI
public interface Serializer {

    /**
     * 序列化
     *
     * @param data
     * @param <T>
     * @return
     */
    <T> byte[] serialize(T data);

    /**
     * 反序列化
     *
     * @param clazz
     * @param bytes
     * @param <T>
     * @return
     */
    <T> T deserialize(Class<T> clazz, byte[] bytes);
}
