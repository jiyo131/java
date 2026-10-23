package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.SearchLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索相关接口。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchLogService searchLogService;

    /**
     * 近 30 天热门搜索词。
     */
    @GetMapping("/hot")
    public Result<List<String>> hot(@RequestParam(required = false, defaultValue = "8") int limit) {
        return Result.ok(searchLogService.hotKeywords(limit));
    }
}
