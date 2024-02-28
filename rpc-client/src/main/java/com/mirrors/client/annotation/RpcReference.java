package com.mirrors.client.annotation;

import java.lang.annotation.*;

/**
 * 服务注入注解，被标注的属性将自动注入服务的实现类（基于动态代理实现）
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 9:13
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RpcReference {
    /**
     * 接口类型
     *
     * @return
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 全限定接口名
     *
     * @return
     */
    String interfaceName() default "";

    /**
     * 版本号，默认1.0
     *
     * @return
     */
    String version() default "1.0";

    /**
     * 负载均衡策略
     *
     * @return
     */
    String loadBalance() default "";

    /**
     * 服务调用超时时间
     *
     * @return
     */
    int timeout() default 0;
}
