package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.EntryCard;
import com.fenglin.springboottest.controller.dto.EntryDetail;
import com.fenglin.springboottest.controller.dto.PageResult;
import com.fenglin.springboottest.service.EntryService;
import com.fenglin.springboottest.service.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页档案列表 / 搜索 / 详情接口。
 */
@RestController
@RequestMapping("/api/entries")
public class EntryController {

    @Autowired
    private EntryService entryService;

    @Autowired
    private TokenStore tokenStore;

    /**
     * 列表 + 检索。分类支持传一级分类（自动带出二级分类内容）。
     *
     * @param record 1 记录检索日志（默认，用于热门搜索词）/ 0 不记录（前端打字联想调用）
     */
    @GetMapping
    public Result<PageResult<EntryCard>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "default") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "12") int size,
            @RequestParam(required = false, defaultValue = "1") int record,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = tokenStore.resolveUserId(authorization);
        return Result.ok(entryService.search(keyword, categoryId, sort, page, size, userId, record != 0));
    }

    /**
     * 编辑推荐。
     */
    @GetMapping("/featured")
    public Result<List<EntryCard>> featured(@RequestParam(required = false, defaultValue = "6") int limit) {
        return Result.ok(entryService.featured(limit));
    }

    /**
     * 档案详情（浏览量 +1）。
     */
    @GetMapping("/{id}")
    public Result<EntryDetail> detail(@PathVariable("id") Long id) {
        EntryDetail detail = entryService.detail(id);
        if (detail == null) {
            return Result.fail(404, "档案不存在或已下架");
        }
        return Result.ok(detail);
    }
}
