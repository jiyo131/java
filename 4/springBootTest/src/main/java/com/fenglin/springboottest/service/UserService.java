package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.entity.User;

public interface UserService extends IService<User> {

    /**
     * 注册新用户
     * @return 0 成功，-1 账号已存在
     */
    int register(String username, String password);

    /**
     * 校验登录
     * @return 匹配的用户，否则 null
     */
    User login(String username, String password);
}
