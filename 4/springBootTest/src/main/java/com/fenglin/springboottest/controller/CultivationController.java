package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.CultivationAction;
import com.fenglin.springboottest.controller.dto.CultivationOverview;
import com.fenglin.springboottest.service.CultivationService;
import com.fenglin.springboottest.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 「修炼」接口：修为管境界，情绪值管钱包。
 * 全部需要登录态（401 由 TokenStore.resolveUserId 判定）。
 */
@RestController
@RequestMapping("/api/cultivation")
public class CultivationController {

    @Autowired
    private CultivationService cultivationService;

    @Autowired
    private TokenStore tokenStore;

    /** 修炼面板：累计修为 + 今日打坐状态 + 可服用背包 */
    @GetMapping
    public Result<CultivationOverview> overview(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录后再修炼");
        }
        return Result.ok(cultivationService.overview(userId));
    }

    /** 每日打坐（每自然日一次） */
    @PostMapping("/meditate")
    public Result<CultivationAction> meditate(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录后再修炼");
        }
        try {
            return Result.ok(cultivationService.meditate(userId));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 服用丹药 / 参悟功法（消耗一条未使用兑换记录，转为修为） */
    @PostMapping("/use/{recordId}")
    public Result<CultivationAction> use(
            @PathVariable("recordId") Long recordId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录后再修炼");
        }
        try {
            return Result.ok(cultivationService.use(userId, recordId));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
