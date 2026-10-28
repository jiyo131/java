package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 后台管理鉴权组件。
 *
 * <p>当前约定：star_user 表中用户名为 admin 的账号即为后台管理员（演示项目从简，
 * 不单独建管理员表）。所有 /api/admin/** 接口在业务处理前先通过本组件校验，
 * 非管理员一律返回 401。</p>
 */
@Service
public class AdminGuard {

    /** 管理员账号名，与 DataInitializer 创建的演示账号保持一致 */
    public static final String ADMIN_USERNAME = "admin";

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private UserService userService;

    /**
     * 根据 Authorization 头解析出管理员用户。
     * @return 管理员用户；token 无效或不是管理员时返回 null
     */
    public User adminOf(String authorization) {
        Long uid = tokenStore.resolveUserId(authorization);
        if (uid == null) {
            return null;
        }
        User user = userService.getById(uid);
        if (user == null || !ADMIN_USERNAME.equals(user.getUsername())) {
            return null;
        }
        return user;
    }

    /** 是否管理员（便捷布尔版） */
    public boolean isAdmin(String authorization) {
        return adminOf(authorization) != null;
    }
}
