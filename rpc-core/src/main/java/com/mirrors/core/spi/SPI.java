package com.mirrors.core.spi;

import java.lang.annotation.*;

/**
 * SPI注解，目前只作用于接口，被标识的接口 要加载所有扩展实现类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 9:33
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SPI {
}
