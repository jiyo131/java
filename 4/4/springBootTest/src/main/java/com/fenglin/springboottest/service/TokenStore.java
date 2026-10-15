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

    public void remove(String token) {
        store.remove(token);
    }
}
