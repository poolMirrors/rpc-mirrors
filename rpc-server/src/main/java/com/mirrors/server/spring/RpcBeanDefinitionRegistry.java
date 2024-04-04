package com.mirrors.server.spring;

import com.mirrors.server.annotation.RpcComponentScan;
import com.mirrors.server.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

/**
 * 自定义 rpc框架 BeanDefinition 注册器类，
 * 主要用于处理 @RpcComponentScan 注解 和 @RpcService注解，
 * 先找到 @RpcComponentScan注解属性上的 的包位置，再扫描包下 被 @RpcService注解 标注的类；
 * <p>
 * 所有实现了ImportBeanDefinitionRegistrar该接口的类的都会被ConfigurationClassPostProcessor处理，
 * ConfigurationClassPostProcessor实现了BeanFactoryPostProcessor接口，
 * 所以ImportBeanDefinitionRegistrar中动态注册的bean是优先于依赖其的bean初始化
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/13 15:49
 */
@Slf4j
public class RpcBeanDefinitionRegistry implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    /**
     * 接口：统一资源定位器
     */
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 此方法会在 spring 自定义扫描执行之后执行，这个时候 beanDefinitionMap 已经有扫描到的 beanDefinition 对象了
     *
     * @param annotationMetadata 包含有关当前正在处理的注解类的元数据信息，如注解的属性值、类名等。
     * @param registry           允许将新的Bean定义注册到Spring容器中。
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        // 1.获取 @RpcComponentScan注解 的属性和值
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcComponentScan.class.getName()));
        String[] basePackages = {};
        if (annotationAttributes != null) {
            basePackages = annotationAttributes.getStringArray("basePackages"); // 拿到注解的属性值
        }
        // 如果没有指定包名，设置默认包路径
        if (basePackages.length == 0) {
            StandardAnnotationMetadata metadata = (StandardAnnotationMetadata) annotationMetadata;
            basePackages = new String[]{metadata.getIntrospectedClass().getPackage().getName()};
        }
        // 2.创建 @RpcService注解 的Scanner
        RpcClassPathBeanDefinitionScanner scanRpcService = new RpcClassPathBeanDefinitionScanner(registry, RpcService.class);
        if (resourceLoader != null) {
            scanRpcService.setResourceLoader(resourceLoader);
        }

        // 3.扫描包下所有被 @RpcService 标注的bean（scan方法会调用register方法，注册扫描到的类，并生成 BeanDefinition 注册到 spring 容器）
        int count = scanRpcService.scan(basePackages);
        log.info("The number of BeanDefinition scanned and annotated by RpcService is {}.", count);
    }
}
