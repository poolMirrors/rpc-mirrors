package com.mirrors.consumer.controller;

import com.mirrors.api.service.HelloService;
import com.mirrors.client.annotation.RpcReference;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/16 20:54
 */
@RestController
@RequestMapping
public class HelloController {

    @RpcReference // 远程调用服务
    private HelloService helloService;

    @RequestMapping("/hello/{name}")
    public String hello(@PathVariable("name") String name) {
        String s = helloService.sayHello(name);
        // todo: 解除注释，添加断点方便debug
        //System.out.println("last");
        return s;
    }

    @RequestMapping("/hello/test/{count}")
    public Map<String, Long> performTest(@PathVariable("count") Long count) {
        Map<String, Long> result = new HashMap<>();
        result.put("调用次数", count);

        long start = System.currentTimeMillis();
        for (long i = 0; i < count; i++) {
            helloService.sayHello(Long.toString(i));
        }
        result.put("耗时", System.currentTimeMillis() - start);

        return result;
    }
}
