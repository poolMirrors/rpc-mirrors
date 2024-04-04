package com.mirrors.consumer.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/17 10:14
 */
@Configuration
@ComponentScan("com.mirrors")
@PropertySource(value = "classpath:application.yml", factory = YamlConfig.YamlPropertySourceFactory.class)
public class YamlConfig {

    /**
     * 读取 yaml 配置文件的属性工厂类
     */
    static class YamlPropertySourceFactory implements PropertySourceFactory {

        /**
         * @param name
         * @param resource
         * @return
         * @throws IOException
         */
        @Override
        public org.springframework.core.env.PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
            Properties propertiesFromYaml = loadYamlIntoProperties(resource);
            String sourceName = name != null ? name : resource.getResource().getFilename();
            return new PropertiesPropertySource(Objects.requireNonNull(sourceName), propertiesFromYaml);
        }

        /**
         * 将yaml文件转为properties返回
         *
         * @param resource
         * @return
         * @throws FileNotFoundException
         */
        private Properties loadYamlIntoProperties(EncodedResource resource) throws FileNotFoundException {
            try {
                YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
                factory.setResources(resource.getResource());
                factory.afterPropertiesSet();
                return factory.getObject();
            } catch (IllegalStateException e) {
                Throwable cause = e.getCause();
                if (cause instanceof FileNotFoundException)
                    throw (FileNotFoundException) e.getCause();
                throw e;
            }
        }
    }
}
