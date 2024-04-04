package com.mirrors.provider;

import com.mirrors.server.annotation.RpcComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/16 20:34
 */
@SpringBootApplication
@RpcComponentScan(basePackages = {"com.mirrors.provider"})
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class);
    }
}
