package com.mirrors.server.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

/**
 * 自定义 类路径下的包扫描器
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 16:28
 */
public class RpcClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    /**
     * 必须继承了Annotation的注解类型
     */
    private Class<? extends Annotation> annotationType;

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
     * 自定义构造函数
     *
     * @param registry
     * @param annotationType
     */
    public RpcClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annotationType) {
        super(registry);
        this.annotationType = annotationType;
        // 过滤指定的注解类型（过滤器）
        if (annotationType != null) {
            this.addIncludeFilter(new AnnotationTypeFilter(annotationType));
        } else {
            this.addIncludeFilter(((metadataReader, metadataReaderFactory) -> true));
        }
    }

    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }
}
