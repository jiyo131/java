package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.AuctionAction;
import com.fenglin.springboottest.controller.dto.AuctionOverview;
import com.fenglin.springboottest.service.AuctionService;
import com.fenglin.springboottest.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 「涂山拍卖行」接口：竞拍看板 / 出价 / 一口价 / 出价记录，全部需要登录态。
 * 401 约定与 TaskController 一致（Result.fail(401, ...) 由前端 http.js 转成 e.code）。
 */
@RestController
@RequestMapping("/api/auction")
public class AuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private TokenStore tokenStore;

    /** 拍卖行面板：竞拍中拍品 + 成交纪录 + 我的夺宝 + 统计（懒结算到期场次） */
    @GetMapping
    public Result<AuctionOverview> overview(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        return Result.ok(auctionService.overview(userId));
    }

    /** 某件拍品的出价记录（倒序，最近 30 条） */
    @GetMapping("/{lotId}/bids")
    public Result<List<AuctionOverview.BidCard>> bids(
            @PathVariable Long lotId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        return Result.ok(auctionService.bids(userId, lotId));
    }

    /** 出价：body { amount }；出价即扣情绪值，被超越全额退款 */
    @PostMapping("/{lotId}/bid")
    public Result<AuctionAction> bid(
            @PathVariable Long lotId,
            @RequestBody Map<String, Integer> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            Integer amount = body == null ? null : body.get("amount");
            return Result.ok(auctionService.bid(userId, lotId, amount));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 一口价：立即成交并自动开下一场 */
    @PostMapping("/{lotId}/buyout")
    public Result<AuctionAction> buyout(
            @PathVariable Long lotId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(auctionService.buyout(userId, lotId));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
