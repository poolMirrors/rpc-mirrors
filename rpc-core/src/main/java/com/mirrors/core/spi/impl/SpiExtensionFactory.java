package com.mirrors.core.spi.impl;

import com.mirrors.core.spi.ExtensionFactory;
import com.mirrors.core.spi.ExtensionLoader;
import com.mirrors.core.spi.SPI;

/**
 * 获取 被SPI标注的接口 的所有扩展实现类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 9:25
 */
public class SpiExtensionFactory implements ExtensionFactory {

    @Override
    public <T> T getExtension(Class<?> type, String name) {
        // 如果是接口 且 接口注解有SPI
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) {
            // todo: 完善spi？
            ExtensionLoader<?> extensionLoader = ExtensionLoader.getExtensionLoader(type);
        }
        return null;
    }
}
