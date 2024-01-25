package com.mirrors.api.service;

import com.mirrors.api.bean.User;

import java.util.List;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/16 20:27
 */
public interface UserService {

    User queryUser();

    List<User> getAllUsers();
}
