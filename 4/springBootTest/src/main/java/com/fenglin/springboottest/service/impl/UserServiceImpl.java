package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** 默认权限：注册用户一律为默认权限（1），管理员权限为 0 */
    public static final int PERMISSION_DEFAULT = 1;
    public static final int PERMISSION_ADMIN = 0;

    @Override
    public int register(String username, String password) {
        Long count = lambdaQuery().eq(User::getUsername, username).count();
        if (count != null && count > 0) {
            return -1;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // 明文存储
        user.setNickname(username);
        user.setStatus(1);
        user.setPermission(PERMISSION_DEFAULT);
        user.setCreatedAt(LocalDateTime.now());
        save(user);
        return 0;
    }

    @Override
    public User login(String username, String password) {
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null) {
            return null;
        }
        // 明文比对
        if (!Objects.equals(password, user.getPassword())) {
            return null;
        }
        return user;
    }
}
