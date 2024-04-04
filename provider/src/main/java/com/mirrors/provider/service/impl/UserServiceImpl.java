package com.mirrors.provider.service.impl;

import com.mirrors.api.bean.User;
import com.mirrors.api.service.UserService;
import com.mirrors.server.annotation.RpcService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/16 20:37
 */
@RpcService(interfaceClass = UserService.class) // 标注服务实现类
public class UserServiceImpl implements UserService {

    @Override
    public User queryUser() {
        return new User("mirrors", "20231216", 23);
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(
                Arrays.asList(
                        new User("mirrors1", "20231216", 23),
                        new User("mirrors2", "20231216", 23),
                        new User("mirrors3", "20231216", 23)
                )
        );
    }
}
