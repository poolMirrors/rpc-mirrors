package com.mirrors.client.spring;

import com.mirrors.client.annotation.RpcReference;
import com.mirrors.client.proxy.ClientProxyFactory;
import com.mirrors.core.discover.ServiceDiscover;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

/**
 * 客户端 Bean 后置处理器，扫描创建的 bean 中有被 @RpcReference注解 标注的属性，获取对应的代理对象并进行替换
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 16:09
 */
@Slf4j
public class RpcClientBeanPostProcessor implements BeanPostProcessor {

    /**
     * 代理工厂
     */
    private final ClientProxyFactory clientProxyFactory;

    public RpcClientBeanPostProcessor(ClientProxyFactory clientProxyFactory) {
        this.clientProxyFactory = clientProxyFactory;
    }

    /**
     * 在 bean 实例化完后，扫描 bean 中需要进行 rpc 注入的属性；
     * 将对应的属性使用 代理对象 进行替换
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取 bean 的所有属性（不分修饰符）
        Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历所有属性
        for (Field field : fields) {
            // 判断当前属性是否被 @RpcReference 标注
            if (field.isAnnotationPresent(RpcReference.class)) {
                // 获取注解
                RpcReference rpcReferenceAnnotation = field.getAnnotation(RpcReference.class);
                // 获取 被@RpcReference标注 的属性当前类型
                Class<?> clazz = field.getType();
                try {
                    if (!"".equals(rpcReferenceAnnotation.interfaceName())) {
                        clazz = Class.forName(rpcReferenceAnnotation.interfaceName());
                    }
                    if (rpcReferenceAnnotation.interfaceClass() != void.class) {
                        clazz = rpcReferenceAnnotation.interfaceClass();
                    }
                    // 获取指定类型的代理对象 =》
                    Object proxy = clientProxyFactory.getProxy(clazz, rpcReferenceAnnotation.version());
                    // 关闭安全检测
                    field.setAccessible(true);
                    // 设置 bean 的代理对象！
                    field.set(bean, proxy);

                } catch (Exception e) {
                    throw new RuntimeException(String.format("the type of field [%s] is [%s] and the proxy type is [%s]", field.getName(), field.getClass(), clazz), e);
                }
            }
        }
        // 返回bean
        return bean;
    }
}
