package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.TaskAction;
import com.fenglin.springboottest.controller.dto.TaskOverview;
import com.fenglin.springboottest.service.TaskService;
import com.fenglin.springboottest.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页「红线仙任务」接口：告示板 / 接取 / 取消 / 完成（结算情绪值）。
 * 任务与情绪值都属于具体用户，全部需要登录态。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TokenStore tokenStore;

    /** 告示板：任务列表（含当前用户的接取状态）+ 情绪值 + 统计 */
    @GetMapping
    public Result<TaskOverview> overview(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录后再接委托");
        }
        return Result.ok(taskService.overview(userId));
    }

    /** 接取委托 */
    @PostMapping("/{id}/accept")
    public Result<TaskAction> accept(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录后再接委托");
        }
        try {
            return Result.ok(taskService.accept(userId, id));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 取消接取 */
    @PostMapping("/{id}/cancel")
    public Result<TaskAction> cancel(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(taskService.cancel(userId, id));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 完成委托，结算情绪值 */
    @PostMapping("/{id}/complete")
    public Result<TaskAction> complete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        if (userId == null) {
            return Result.fail(401, "登录已失效，请重新登录");
        }
        try {
            return Result.ok(taskService.complete(userId, id));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }
}
