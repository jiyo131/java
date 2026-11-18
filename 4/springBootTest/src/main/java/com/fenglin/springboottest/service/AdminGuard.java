package com.fenglin.springboottest.service;

import com.fenglin.springboottest.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 后台管理鉴权组件。
 *
 * <p>当前约定：star_user 表 permission=0 的账号即为管理员权限（1 为默认权限），
 * 只有管理员权限才能访问后台。所有 /api/admin/** 接口在业务处理前先通过本组件校验，
 * 非管理员一律返回 401。</p>
 */
@Service
public class AdminGuard {

    /** 内置演示管理员账号名（受不可停用/不可删除等保护规则约束） */
    public static final String ADMIN_USERNAME = "admin";

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private UserService userService;

    /**
     * 根据 Authorization 头解析出管理员用户。
     * @return 管理员用户；token 无效或权限不是管理员权限（permission != 0）时返回 null
     */
    public User adminOf(String authorization) {
        Long uid = tokenStore.resolveUserId(authorization);
        if (uid == null) {
            return null;
        }
        User user = userService.getById(uid);
        if (user == null || user.getPermission() == null || user.getPermission() != 0) {
            return null;
        }
        // 账号被停用后，已签发的 token 立即失效
        if (user.getStatus() != null && user.getStatus() == 0) {
            return null;
        }
        return user;
    }

    /** 是否管理员（便捷布尔版） */
    public boolean isAdmin(String authorization) {
        return adminOf(authorization) != null;
    }
}
