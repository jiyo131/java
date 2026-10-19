package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.LoginRequest;
import com.fenglin.springboottest.controller.dto.LoginResponse;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.TokenStore;
import com.fenglin.springboottest.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenStore tokenStore;

    /** 注册：账号 / 密码 / 确定密码（前端已校验一致） */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody LoginRequest req) {
        String username = req.getUsername();
        String password = req.getPassword();

        if (username == null || username.trim().length() < 3 || username.length() > 16) {
            return Result.fail("账号需 3-16 位（字母、数字或中文）");
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            return Result.fail("密码需 6-20 位");
        }
        int code = userService.register(username.trim(), password);
        if (code == -1) {
            return Result.fail("该涂山账号已被注册");
        }
        return Result.ok();
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        if (user == null) {
            return Result.fail("账号或姻缘密钥不正确");
        }
        String token = tokenStore.createToken(user.getId());
        return Result.ok(new LoginResponse(token, user.getUsername(), user.getNickname()));
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public Result<LoginResponse> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = parseToken(auth);
        if (token == null) {
            return Result.fail(401, "未登录");
        }
        Long uid = tokenStore.getUserId(token);
        if (uid == null) {
            return Result.fail(401, "登录已失效");
        }
        User user = userService.getById(uid);
        if (user == null) {
            return Result.fail(401, "用户不存在");
        }
        return Result.ok(new LoginResponse(token, user.getUsername(), user.getNickname()));
    }

    /** 注销 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = parseToken(auth);
        if (token != null) {
            tokenStore.remove(token);
        }
        return Result.ok();
    }

    private String parseToken(String auth) {
        if (auth == null) {
            return null;
        }
        String t = auth.trim();
        if (t.startsWith("Bearer ")) {
            return t.substring(7).trim();
        }
        if (t.startsWith("Bearer")) {
            return t.substring(6).trim();
        }
        return t;
    }
}
