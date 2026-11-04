package com.fenglin.springboottest.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.PageResult;
import com.fenglin.springboottest.entity.TaskRecord;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.entity.UserEmotion;
import com.fenglin.springboottest.mapper.TaskRecordMapper;
import com.fenglin.springboottest.mapper.UserEmotionMapper;
import com.fenglin.springboottest.service.AdminGuard;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台 · 账号管理（star_user 增删改查）。
 *
 * <p>保护规则：内置管理员账号（admin）不可停用、不可删除、不可修改账号名；
 * 停用账号无法登录前后台，已签发的 token 立即失效（AdminGuard 拦截）。</p>
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final int MAX_SIZE = 50;

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskRecordMapper taskRecordMapper;

    @Autowired
    private UserEmotionMapper userEmotionMapper;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 账号列表：关键词（账号/昵称）/ 状态筛选 + 分页；绝不返回密码字段 */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "-1") int status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }

        int p = Math.max(1, page);
        int s = size <= 0 ? 10 : Math.min(size, MAX_SIZE);

        LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery(User.class);
        if (status == 0 || status == 1) {
            qw.eq(User::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().replaceAll("[%_\\\\]", "");
            qw.and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw));
        }

        long total = userService.count(qw);
        qw.orderByAsc(User::getId).last("LIMIT " + s + " OFFSET " + (long) (p - 1) * s);
        List<User> rows = userService.list(qw);

        List<Map<String, Object>> list = rows.stream().map(this::toMap).toList();
        return Result.ok(PageResult.of(list, total, p, s));
    }

    /** 新增账号（密码 BCrypt 加密入库） */
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "Authorization", required = false) String auth,
                               @RequestBody Map<String, Object> body) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        String nickname = str(body.get("nickname"));

        String err = validateUsername(username);
        if (err != null) {
            return Result.fail(err);
        }
        err = validatePassword(password);
        if (err != null) {
            return Result.fail(err);
        }
        if (nickname.length() > 32) {
            return Result.fail("昵称不超过 32 字");
        }
        if (userService.lambdaQuery().eq(User::getUsername, username).count() > 0) {
            return Result.fail("该账号已被注册：" + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setNickname(nickname.isEmpty() ? username : nickname);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        userService.save(user);
        return Result.ok();
    }

    /** 修改昵称（账号名不可改） */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        User admin = adminGuard.adminOf(auth);
        if (admin == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        User old = id == null ? null : userService.getById(id);
        if (old == null) {
            return Result.fail(404, "账号不存在，可能已被删除");
        }
        if (body.containsKey("username") && !str(body.get("username")).equals(old.getUsername())) {
            return Result.fail("账号名不允许修改");
        }
        String nickname = str(body.get("nickname"));
        if (nickname.isEmpty() || nickname.length() > 32) {
            return Result.fail("昵称必填，且不超过 32 字");
        }
        User patch = new User();
        patch.setId(old.getId());
        patch.setNickname(nickname);
        userService.updateById(patch);
        return Result.ok();
    }

    /** 停用 / 启用（admin 不允许停用） */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable("id") Long id,
                                     @RequestBody Map<String, Object> body) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        User old = id == null ? null : userService.getById(id);
        if (old == null) {
            return Result.fail(404, "账号不存在，可能已被删除");
        }
        int status;
        try {
            status = Integer.parseInt(String.valueOf(body.get("status")));
        } catch (Exception ex) {
            return Result.fail("状态参数不正确");
        }
        if (status != 0 && status != 1) {
            return Result.fail("状态参数不正确");
        }
        if (status == 0 && AdminGuard.ADMIN_USERNAME.equals(old.getUsername())) {
            return Result.fail("内置管理员账号不允许停用");
        }
        userService.lambdaUpdate().eq(User::getId, id).set(User::getStatus, status).update();
        return Result.ok();
    }

    /** 重置密码（新密码 BCrypt 加密入库） */
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @PathVariable("id") Long id,
                                      @RequestBody Map<String, Object> body) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        User old = id == null ? null : userService.getById(id);
        if (old == null) {
            return Result.fail(404, "账号不存在，可能已被删除");
        }
        String password = str(body.get("password"));
        String err = validatePassword(password);
        if (err != null) {
            return Result.fail(err);
        }
        User patch = new User();
        patch.setId(old.getId());
        patch.setPassword(encoder.encode(password));
        userService.updateById(patch);
        return Result.ok();
    }

    /** 删除账号（admin 不允许删除；同时清理该用户的任务记录与情绪值） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        User old = id == null ? null : userService.getById(id);
        if (old == null) {
            return Result.fail(404, "账号不存在，可能已被删除");
        }
        if (AdminGuard.ADMIN_USERNAME.equals(old.getUsername())) {
            return Result.fail("内置管理员账号不允许删除");
        }
        // 关联数据一并清理（检索日志保留，仅作分析用途）
        taskRecordMapper.delete(Wrappers.lambdaQuery(TaskRecord.class).eq(TaskRecord::getUserId, id));
        userEmotionMapper.deleteById(id);
        userService.removeById(id);
        return Result.ok();
    }

    // ------------------------------------------------------------------

    /** 脱敏后的账号信息（不含密码） */
    private Map<String, Object> toMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("status", u.getStatus());
        m.put("createdAt", u.getCreatedAt());
        m.put("admin", AdminGuard.ADMIN_USERNAME.equals(u.getUsername()));
        return m;
    }

    private String validateUsername(String username) {
        if (username.isEmpty() || username.length() < 3 || username.length() > 16) {
            return "账号需 3-16 位";
        }
        if (!username.matches("[\\u4e00-\\u9fa5A-Za-z0-9_]+")) {
            return "账号只能包含中文、字母、数字或下划线";
        }
        return null;
    }

    private String validatePassword(String password) {
        if (password.length() < 6 || password.length() > 20) {
            return "密码需 6-20 位";
        }
        return null;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
