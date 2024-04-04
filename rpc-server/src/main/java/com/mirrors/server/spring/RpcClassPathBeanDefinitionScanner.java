package com.mirrors.server.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

/**
 * 自定义 类路径下的包扫描器；由{@link RpcBeanDefinitionRegistry} 创建；
 * 将指定包下的类通过一定规则过滤后 将Class 信息包装成 BeanDefinition 的形式注册到IOC容器中。
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 16:28
 */
public class RpcClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    /**
     * Class类型，必须继承了Annotation的注解类型
     */
    private Class<? extends Annotation> annotationType;

    /**
     * 默认无参构造
     *
     * @param registry
     */
    public RpcClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry);
        // 过滤指定的注解类型（过滤器）
        if (annotationType != null) {
            this.addIncludeFilter(new AnnotationTypeFilter(this.annotationType));
        } else {
            this.addIncludeFilter(((metadataReader, metadataReaderFactory) -> true));
        }
    }

    /**
     * 自定义构造函数；在RpcBeanDefinitionRegistry中传入RpcService.class；
     * <a href="https://www.jianshu.com/p/d5ffdccc4f5d">ClassPathBeanDefinitionScanner</a>
     *
     * @param registry
     * @param annotationType
     */
    public RpcClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annotationType) {
        super(registry);
        this.annotationType = annotationType;
        // 过滤指定的注解类型（过滤器）
        if (annotationType != null) {
            // 添加该注解的默认过滤器
            // ClassPathBeanDefinitionScanner作用就是将指定包下的类通过一定规则过滤后 将 Class信息 包装成 BeanDefinition 的形式注册到IOC容器中。
            this.addIncludeFilter(new AnnotationTypeFilter(annotationType));
        } else {
            this.addIncludeFilter(((metadataReader, metadataReaderFactory) -> true));
        }
    }

    /**
     * 在 RpcBeanDefinitionRegistry 中被调用
     *
     * @param basePackages
     * @return
     */
    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }
}
