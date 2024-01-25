package com.mirrors.consumer.controller;

import com.mirrors.api.bean.User;
import com.mirrors.api.service.UserService;
import com.mirrors.client.annotation.RpcReference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/16 20:50
 */
@RestController
public class UserController {

    @RpcReference // 远程调用服务
    private UserService userService;

    @RequestMapping("/user/getUser")
    public User getUser() {
        return userService.queryUser();
    }

    @RequestMapping("/user/getAllUser")
    public List<User> getAllUser() {
        return userService.getAllUsers();
    }
}
