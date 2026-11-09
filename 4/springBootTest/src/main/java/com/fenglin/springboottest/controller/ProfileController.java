package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.ProfileVo;
import com.fenglin.springboottest.service.ProfileService;
import com.fenglin.springboottest.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 「我的主页」接口：修士/妖族境界 + 委托接取情况 + 修炼阵营，全部需要登录态。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private TokenStore tokenStore;

    /** 主页聚合数据：境界线 + 接取情况 + 情绪值统计 */
    @GetMapping("/me")
    public Result<ProfileVo> me(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(profileService.profile(userId));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 选择修炼阵营：body { camp: 1 修士 / 2 妖族 } */
    @PostMapping("/camp")
    public Result<ProfileVo> setCamp(
            @RequestBody Map<String, Integer> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(profileService.setCamp(userId, body == null ? null : body.get("camp")));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
