package com.fenglin.springboottest.controller.admin;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.LoginRequest;
import com.fenglin.springboottest.controller.dto.LoginResponse;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.AdminGuard;
import com.fenglin.springboottest.service.TokenStore;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理登录接口（仅登录，不提供注册）。
 *
 * <p>与前台 /api/auth/login 的区别：这里只允许管理员账号（admin）登录成功，
 * 普通注册用户会被明确拒绝，用于保护数据管理功能。</p>
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private AdminGuard adminGuard;

    /** 管理员登录：校验通过后签发 token */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        if (username.isEmpty() || req.getPassword() == null || req.getPassword().isEmpty()) {
            return Result.fail("请输入管理员账号和密码");
        }
        if (!AdminGuard.ADMIN_USERNAME.equals(username)) {
            return Result.fail("该账号不具备后台管理权限");
        }
        User user = userService.login(username, req.getPassword());
        if (user == null) {
            return Result.fail("管理员账号或密码不正确");
        }
        String token = tokenStore.createToken(user.getId());
        return Result.ok(new LoginResponse(token, user.getUsername(), user.getNickname()));
    }

    /** 校验当前管理员登录态 */
    @GetMapping("/me")
    public Result<LoginResponse> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        User admin = adminGuard.adminOf(auth);
        if (admin == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        String token = auth == null ? "" : auth.trim();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        return Result.ok(new LoginResponse(token, admin.getUsername(), admin.getNickname()));
    }

    /** 退出后台 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        Long uid = tokenStore.resolveUserId(auth);
        if (uid != null && adminGuard.isAdmin(auth)) {
            String token = auth.trim();
            if (token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            } else if (token.startsWith("Bearer")) {
                token = token.substring(6).trim();
            }
            tokenStore.remove(token);
        }
        return Result.ok();
    }
}
