package com.fenglin.springboottest.controller.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.entity.Entry;
import com.fenglin.springboottest.entity.SearchLog;
import com.fenglin.springboottest.service.AdminGuard;
import com.fenglin.springboottest.service.CategoryService;
import com.fenglin.springboottest.service.EntryService;
import com.fenglin.springboottest.service.SearchLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台 · 数据概览（首页展示数据的总体统计）。
 */
@RestController
@RequestMapping("/api/admin/overview")
public class AdminOverviewController {

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private EntryService entryService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SearchLogService searchLogService;

    @GetMapping
    public Result<Map<String, Object>> overview(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("entryTotal", entryService.count());
        data.put("entryOnline", entryService.lambdaQuery().eq(Entry::getStatus, 1).count());
        data.put("entryOffline", entryService.lambdaQuery().eq(Entry::getStatus, 0).count());
        data.put("categoryTotal", categoryService.count());
        data.put("categoryOnline", categoryService.lambdaQuery().eq(Category::getStatus, 1).count());

        // 浏览量合计（单条聚合 SQL）
        List<Object> viewSum = entryService.listObjs(
                Wrappers.<Entry>query().select("IFNULL(SUM(view_count), 0)"));
        data.put("viewTotal", viewSum.isEmpty() || viewSum.get(0) == null ? 0L : viewSum.get(0));

        // 近 30 天检索次数 + 热门检索词（前台「热门搜索」的数据源）
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        data.put("searchCount30d", searchLogService.lambdaQuery()
                .ge(SearchLog::getCreatedAt, since).count());
        data.put("hotKeywords", searchLogService.hotKeywords(10));
        return Result.ok(data);
    }
}
