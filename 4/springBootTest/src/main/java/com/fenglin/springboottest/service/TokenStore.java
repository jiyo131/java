package com.fenglin.springboottest.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易内存令牌存储（演示用）。
 * 生产环境请替换为 Redis / JWT 等方案。
 */
@Service
public class TokenStore {

    private final Map<String, Long> store = new ConcurrentHashMap<>();

    public String createToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "")
                + Long.toHexString(System.currentTimeMillis());
        store.put(token, userId);
        return token;
    }

    public Long getUserId(String token) {
        return store.get(token);
    }

    /**
     * 从 Authorization 头解析用户 id（支持 "Bearer xxx" 与裸 token），未登录返回 null。
     * 首页检索 / 详情接口允许匿名访问，登录时仅用于检索日志归属。
     */
    public Long resolveUserId(String authorization) {
        if (authorization == null) {
            return null;
        }
        String t = authorization.trim();
        if (t.startsWith("Bearer ")) {
            t = t.substring(7).trim();
        } else if (t.startsWith("Bearer")) {
            t = t.substring(6).trim();
        }
        return t.isEmpty() ? null : store.get(t);
    }

    public void remove(String token) {
        store.remove(token);
    }
}
