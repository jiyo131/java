package com.fenglin.springboottest.config;

import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 启动初始化：
 * 1) star_user 补齐 status / permission 列（老库平滑迁移，列已存在时忽略报错）；
 * 2) 密码明文化迁移：历史 BCrypt 密码（$2 开头）无法反推明文，统一重置为演示口令
 *    —— admin 重置为 123456，其余老账号重置为 123456（演示项目从简，用户可在后台改密）；
 * 3) admin 账号权限固定为管理员权限（permission=0）；
 * 4) 首次启动自动创建演示账号 admin / 123456（明文，permission=0）。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    /** 明文重置口令：历史 BCrypt 密码无法反推，统一重置 */
    private static final String LEGACY_RESET_PASSWORD = "123456";

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

        // --- 老库迁移：star_user 增加 permission 列（1 默认权限 / 0 管理员权限） ---
        try {
            jdbcTemplate.execute("ALTER TABLE star_user ADD COLUMN permission TINYINT NOT NULL DEFAULT 1 "
                    + "COMMENT '权限：1 默认权限 / 0 管理员权限'");
        } catch (DataAccessException ignore) {
            // 列已存在 → 忽略
        }

        // --- 老库迁移：star_user_emotion 增加 camp 列（我的主页·修炼阵营 1 修士 / 2 妖族） ---
        try {
            jdbcTemplate.execute("ALTER TABLE star_user_emotion ADD COLUMN camp TINYINT NOT NULL DEFAULT 1 "
                    + "COMMENT '修炼阵营：1 修士 / 2 妖族'");
        } catch (DataAccessException ignore) {
            // 列已存在 → 忽略
        }

        // --- 密码明文化迁移：历史 BCrypt 密码（$2 开头）重置为明文演示口令 ---
        try {
            List<User> legacy = userService.lambdaQuery()
                    .likeRight(User::getPassword, "$2")
                    .list();
            for (User u : legacy) {
                User patch = new User();
                patch.setId(u.getId());
                patch.setPassword(LEGACY_RESET_PASSWORD);
                userService.updateById(patch);
            }
            if (!legacy.isEmpty()) {
                System.out.println("[DataInitializer] 已将 " + legacy.size()
                        + " 个历史 BCrypt 密码账号重置为明文口令 " + LEGACY_RESET_PASSWORD);
            }
        } catch (DataAccessException ignore) {
            // 迁移失败不阻断启动
        }

        // --- 演示管理员（明文密码 + 管理员权限） ---
        User admin = userService.lambdaQuery().eq(User::getUsername, "admin").one();
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setPassword("123456");
            admin.setNickname("涂山红娘");
            admin.setStatus(1);
            admin.setPermission(0);
            admin.setCreatedAt(LocalDateTime.now());
            userService.save(admin);
        } else if (admin.getPermission() == null || admin.getPermission() != 0) {
            // 存量 admin：权限兜底为管理员权限（密码已由上方明文化迁移处理，不覆盖用户改过的密码）
            User patch = new User();
            patch.setId(admin.getId());
            patch.setPermission(0);
            userService.updateById(patch);
        }
    }
}
