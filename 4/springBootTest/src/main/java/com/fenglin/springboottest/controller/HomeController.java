package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.CategoryNode;
import com.fenglin.springboottest.controller.dto.EntryCard;
import com.fenglin.springboottest.controller.dto.HomeOverview;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.mapper.EntryMapper;
import com.fenglin.springboottest.service.CategoryService;
import com.fenglin.springboottest.service.EntryService;
import com.fenglin.springboottest.service.SearchLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页展示聚合接口。
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int HOT_LIMIT = 8;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private EntryService entryService;

    @Autowired
    private SearchLogService searchLogService;

    @Autowired
    private EntryMapper entryMapper;

    /**
     * 首屏数据：分类树 + 统计 + 热门搜索词 + 编辑推荐。
     */
    @GetMapping("/overview")
    public Result<HomeOverview> overview() {
        List<CategoryNode> tree = categoryService.tree();
        List<EntryCard> featured = entryService.featured(6);

        long entryCount = 0L;
        for (CategoryNode root : tree) {
            entryCount += root.getCount() == null ? 0L : root.getCount();
        }
        long categoryCount = categoryService.count(
                Wrappers.lambdaQuery(Category.class).eq(Category::getStatus, 1));
        Long views = entryMapper.sumViewCount();

        List<String> hotWords = new ArrayList<>(searchLogService.hotKeywords(HOT_LIMIT));
        for (EntryCard card : featured) {
            if (hotWords.size() >= HOT_LIMIT) {
                break;
            }
            if (!hotWords.contains(card.getName())) {
                hotWords.add(card.getName());
            }
        }

        HomeOverview vo = new HomeOverview();
        vo.setEntryCount(entryCount);
        vo.setCategoryCount(categoryCount);
        vo.setViewCount(views == null ? 0L : views);
        vo.setHotWords(hotWords);
        vo.setCategories(tree);
        vo.setFeatured(featured);
        vo.setServerTime(LocalDateTime.now().format(TIME_FMT));
        return Result.ok(vo);
    }

    /**
     * 热门搜索词（单独暴露，前端可用于局部刷新）。
     */
    @GetMapping("/hot-words")
    public Result<List<String>> hotWords(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(searchLogService.hotKeywords(limit));
    }

    /**
     * 编辑推荐档案。
     */
    @GetMapping("/featured")
    public Result<List<EntryCard>> featured(@RequestParam(defaultValue = "6") int limit) {
        return Result.ok(entryService.featured(limit));
    }
}
