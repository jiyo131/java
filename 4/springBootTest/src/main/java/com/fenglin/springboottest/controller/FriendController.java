package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.FriendOverview;
import com.fenglin.springboottest.service.FriendService;
import com.fenglin.springboottest.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 「我的主页 · 红线挚友」接口：搜索用户 / 发申请 / 处理申请 / 删好友，全部需要登录态。
 * 401 约定与 TaskController 一致（Result.fail(401, ...) 由前端 http.js 转成 e.code）。
 */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private TokenStore tokenStore;

    /** 好友面板：好友列表 + 收到/发出的待处理申请 */
    @GetMapping
    public Result<FriendOverview> overview(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        return Result.ok(friendService.overview(userId));
    }

    /** 搜索用户：?keyword=xxx，按账号或昵称模糊匹配，最多 10 条 */
    @GetMapping("/search")
    public Result<List<FriendOverview.FriendCard>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(friendService.search(userId, keyword));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 发送好友申请：POST /{targetUserId}/request，body 可带 {message} */
    @PostMapping("/{targetUserId}/request")
    public Result<FriendOverview> sendRequest(
            @PathVariable Long targetUserId,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            String message = body == null ? null : body.get("message");
            return Result.ok(friendService.sendRequest(userId, targetUserId, message));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 同意收到的申请 */
    @PostMapping("/request/{requestId}/accept")
    public Result<FriendOverview> accept(
            @PathVariable Long requestId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(friendService.accept(userId, requestId));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 拒绝收到的申请 */
    @PostMapping("/request/{requestId}/reject")
    public Result<FriendOverview> reject(
            @PathVariable Long requestId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(friendService.reject(userId, requestId));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 撤回自己发出的申请 */
    @PostMapping("/request/{requestId}/cancel")
    public Result<FriendOverview> cancel(
            @PathVariable Long requestId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(friendService.cancel(userId, requestId));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 删除好友：{friendUserId} 为对方用户 id */
    @DeleteMapping("/{friendUserId}")
    public Result<FriendOverview> removeFriend(
            @PathVariable Long friendUserId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(friendService.removeFriend(userId, friendUserId));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
