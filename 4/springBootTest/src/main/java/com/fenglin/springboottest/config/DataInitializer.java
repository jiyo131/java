package com.fenglin.springboottest.config;

import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 首次启动时自动创建演示账号 admin / 123456（BCrypt 加密）。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (userService.lambdaQuery().eq(User::getUsername, "admin").count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("123456"));
            admin.setNickname("涂山红娘");
            admin.setCreatedAt(LocalDateTime.now());
            userService.save(admin);
        }
    }
}
