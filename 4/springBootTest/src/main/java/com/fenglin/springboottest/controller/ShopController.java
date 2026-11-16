package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.ShopAction;
import com.fenglin.springboottest.controller.dto.ShopOverview;
import com.fenglin.springboottest.service.ShopService;
import com.fenglin.springboottest.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页「兑换商店」接口：法宝 / 丹药 / 功法 三柜看板 / 兑换。
 * 情绪值属于具体用户，全部接口需要登录态。
 */
@RestController
@RequestMapping("/api/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private TokenStore tokenStore;

    /** 看板：三柜商品 + 用户情绪值 + 我的兑换记录 */
    @GetMapping
    public Result<ShopOverview> overview(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录后再来兑换");
        }
        return Result.ok(shopService.overview(userId));
    }

    /** 兑换商品（扣情绪值） */
    @PostMapping("/{id}/redeem")
    public Result<ShopAction> redeem(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录后再来兑换");
        }
        try {
            return Result.ok(shopService.redeem(userId, id));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
