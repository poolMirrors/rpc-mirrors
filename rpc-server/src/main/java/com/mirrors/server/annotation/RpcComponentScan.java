package com.mirrors.server.annotation;

import com.mirrors.server.spring.RpcBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 自定义 rpc 组件扫描注解（扫描被 RpcService注解 标注的类）
 * 自定义注解式组件扫描的关键逻辑：
 * <p> 1.引入了RpcBeanDefinitionRegistry类，这个类是一个ImportBeanDefinitionRegistrar的实现类 </p>
 * <p> 2.Spring 容器在解析该类型的 Bean 时会调用该实现类中的registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)方法，</p>
 * <p> 3.RpcComponentScan 注解上的信息被提取成 AnnotationMetadata，容器注册器对象 作为此方法的参数 </p>
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 15:36
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(RpcBeanDefinitionRegistry.class) // 【重点】
public @interface RpcComponentScan {

    /**
     * 扫描包路径
     *
     * @return
     */
    String[] basePackages() default {};

}
