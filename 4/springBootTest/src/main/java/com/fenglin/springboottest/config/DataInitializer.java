package com.fenglin.springboottest.config;

import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动初始化：
 * 1) star_user 补齐 status 列（老库平滑迁移，列已存在时忽略报错）；
 * 2) 首次启动自动创建演示账号 admin / 123456（BCrypt 加密）。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // --- 老库迁移：star_user 增加 status 列（CREATE TABLE IF NOT EXISTS 不会给已存在的表加列） ---
        try {
            jdbcTemplate.execute("ALTER TABLE star_user ADD COLUMN status TINYINT NOT NULL DEFAULT 1 "
                    + "COMMENT '状态：1 正常 / 0 停用（停用后无法登录）'");
        } catch (DataAccessException ignore) {
            // 列已存在（Duplicate column name）→ 忽略
        }

        // --- 演示管理员 ---
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (userService.lambdaQuery().eq(User::getUsername, "admin").count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("123456"));
            admin.setNickname("涂山红娘");
            admin.setStatus(1);
            admin.setCreatedAt(LocalDateTime.now());
            userService.save(admin);
        }
    }
}
