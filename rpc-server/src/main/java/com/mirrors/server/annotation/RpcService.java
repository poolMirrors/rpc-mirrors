package com.mirrors.server.annotation;

import java.lang.annotation.*;

/**
 * RpcService注解：标注该类为服务实现类
 * 提供给 Server端(provider) 使用
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 15:29
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RpcService {
    /**
     * 版本号
     *
     * @return
     */
    String version() default "1.0";

    /**
     * 暴露服务的全限定接口名
     *
     * @return
     */
    String interfaceName() default "";

    /**
     * 暴露服务的接口类型
     *
     * @return
     */
    Class<?> interfaceClass() default void.class;
}
