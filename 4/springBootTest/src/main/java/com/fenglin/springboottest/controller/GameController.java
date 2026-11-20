package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.GameWhackResult;
import com.fenglin.springboottest.controller.dto.GameWhackSettle;
import com.fenglin.springboottest.service.GameService;
import com.fenglin.springboottest.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 涂山小游戏接口：目前仅打地鼠（whack）。
 * 全部需要登录态（401 由 TokenStore.resolveUserId 判定）。
 */
@RestController
@RequestMapping("/api/game/whack")
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private TokenStore tokenStore;

    /** 今日局数 + 情绪值快照（进游戏页时拉取） */
    @GetMapping("/summary")
    public Result<GameWhackResult> summary(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        return Result.ok(gameService.summaryWhack(userId));
    }

    /** 结算一局：服务端校验并发放情绪值 */
    @PostMapping("/settle")
    public Result<GameWhackResult> settle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) GameWhackSettle req) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(gameService.settleWhack(userId, req));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
