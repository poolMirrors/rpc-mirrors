package com.mirrors.provider.service.impl;

import com.mirrors.api.service.HelloService;
import com.mirrors.server.annotation.RpcService;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/16 20:40
 */
@RpcService(interfaceClass = HelloService.class) // 标注服务实现类
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}
