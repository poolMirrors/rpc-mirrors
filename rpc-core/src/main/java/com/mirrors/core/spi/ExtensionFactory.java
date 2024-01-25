package com.mirrors.core.spi;

/**
 * 扩展类工厂接口
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 14:03
 */
public interface ExtensionFactory {

    /**
     * 得到扩展对象的实例
     *
     * @param type
     * @param name
     * @param <T>
     * @return
     */
    <T> T getExtension(Class<?> type, String name);

}
