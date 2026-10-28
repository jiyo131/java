package com.fenglin.springboottest.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.PageResult;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.entity.Entry;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.service.AdminGuard;
import com.fenglin.springboottest.service.CategoryService;
import com.fenglin.springboottest.service.EntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台 · 档案数据管理（首页「涂山人物志」内容的增删改查）。
 */
@RestController
@RequestMapping("/api/admin/entries")
public class AdminEntryController {

    private static final int MAX_SIZE = 50;

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private EntryService entryService;

    @Autowired
    private CategoryService categoryService;

    /** 档案列表：关键词 / 分类 / 状态筛选 + 分页（含下架数据） */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "-1") int status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }

        int p = Math.max(1, page);
        int s = size <= 0 ? 10 : Math.min(size, MAX_SIZE);

        LambdaQueryWrapper<Entry> qw = Wrappers.lambdaQuery(Entry.class);
        if (status == 0 || status == 1) {
            qw.eq(Entry::getStatus, status);
        }
        if (categoryId != null && categoryId > 0) {
            // 与前台一致：选中一级分类时带出全部子孙分类的内容
            Set<Long> ids = categoryService.selfAndDescendants(categoryId);
            qw.in(!ids.isEmpty(), Entry::getCategoryId, ids);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().replaceAll("[%_\\\\]", "");
            qw.and(w -> w.like(Entry::getName, kw)
                    .or().like(Entry::getAlias, kw)
                    .or().like(Entry::getFaction, kw)
                    .or().like(Entry::getTags, kw));
        }

        long total = entryService.count(qw);
        qw.orderByAsc(Entry::getSortNo).orderByDesc(Entry::getId)
                .last("LIMIT " + s + " OFFSET " + (long) (p - 1) * s);
        List<Entry> rows = entryService.list(qw);

        Map<Long, Category> categories = categoryService.allEnabledMap();
        List<Map<String, Object>> list = rows.stream().map(e -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("categoryId", e.getCategoryId());
            m.put("categoryName", categoryName(categories, e.getCategoryId()));
            m.put("name", e.getName());
            m.put("alias", e.getAlias());
            m.put("glyph", e.getGlyph());
            m.put("avatar", e.getAvatar());
            m.put("intro", e.getIntro());
            m.put("quote", e.getQuote());
            m.put("bond", e.getBond());
            m.put("faction", e.getFaction());
            m.put("tags", e.getTags());
            m.put("hotScore", e.getHotScore());
            m.put("viewCount", e.getViewCount());
            m.put("sortNo", e.getSortNo());
            m.put("status", e.getStatus());
            m.put("createdAt", e.getCreatedAt());
            m.put("updatedAt", e.getUpdatedAt());
            return m;
        }).toList();
        return Result.ok(PageResult.of(list, total, p, s));
    }

    /** 新增档案 */
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "Authorization", required = false) String auth,
                               @RequestBody Entry entry) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        String err = validate(entry, null);
        if (err != null) {
            return Result.fail(err);
        }
        if (entryService.lambdaQuery().eq(Entry::getName, entry.getName().trim()).count() > 0) {
            return Result.fail("已存在同名档案：" + entry.getName().trim());
        }
        Entry e = entry;
        e.setName(e.getName().trim());
        if (e.getStatus() == null) {
            e.setStatus(1);
        }
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(LocalDateTime.now());
        }
        e.setUpdatedAt(LocalDateTime.now());
        entryService.save(e);
        return Result.ok();
    }

    /** 修改档案 */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id, @RequestBody Entry entry) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        Entry old = id == null ? null : entryService.getById(id);
        if (old == null) {
            return Result.fail(404, "档案不存在，可能已被删除");
        }
        String err = validate(entry, id);
        if (err != null) {
            return Result.fail(err);
        }
        entry.setId(old.getId());
        entry.setName(entry.getName().trim());
        entry.setUpdatedAt(LocalDateTime.now());
        entryService.updateById(entry);
        return Result.ok();
    }

    /** 上架 / 下架 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable("id") Long id,
                                     @RequestBody Map<String, Object> body) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        Entry old = id == null ? null : entryService.getById(id);
        if (old == null) {
            return Result.fail(404, "档案不存在，可能已被删除");
        }
        int status;
        try {
            status = Integer.parseInt(String.valueOf(body.get("status")));
        } catch (Exception ex) {
            return Result.fail("状态参数不正确");
        }
        if (status != 0 && status != 1) {
            return Result.fail("状态参数不正确");
        }
        entryService.lambdaUpdate()
                .eq(Entry::getId, id)
                .set(Entry::getStatus, status)
                .set(Entry::getUpdatedAt, LocalDateTime.now())
                .update();
        return Result.ok();
    }

    /** 删除档案（物理删除，演示项目从简） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        if (id == null || entryService.getById(id) == null) {
            return Result.fail(404, "档案不存在，可能已被删除");
        }
        entryService.removeById(id);
        return Result.ok();
    }

    // ------------------------------------------------------------------

    /** 字段校验；duplicateId 用于更新时排除自身后查重。返回 null 表示通过。 */
    private String validate(Entry e, Long duplicateId) {
        if (e == null) {
            return "请求体不能为空";
        }
        String name = e.getName() == null ? "" : e.getName().trim();
        if (name.isEmpty() || name.length() > 64) {
            return "档案名称必填，且不超过 64 字";
        }
        Entry same = entryService.lambdaQuery().eq(Entry::getName, name).one();
        if (same != null && !same.getId().equals(duplicateId)) {
            return "已存在同名档案：" + name;
        }
        if (e.getCategoryId() == null || e.getCategoryId() <= 0) {
            return "请选择所属分类";
        }
        if (categoryService.getById(e.getCategoryId()) == null) {
            return "所选分类不存在";
        }
        if (e.getAlias() != null && e.getAlias().length() > 128) {
            return "称号 / 别名不超过 128 字";
        }
        if (e.getGlyph() != null && e.getGlyph().length() > 16) {
            return "卡片图标不超过 16 字";
        }
        if (e.getAvatar() != null && e.getAvatar().length() > 255) {
            return "形象图路径不超过 255 字";
        }
        if (e.getIntro() != null && e.getIntro().length() > 5000) {
            return "简介不超过 5000 字";
        }
        if (e.getQuote() != null && e.getQuote().length() > 255) {
            return "经典台词不超过 255 字";
        }
        if (e.getBond() != null && e.getBond().length() > 255) {
            return "红线羁绊不超过 255 字";
        }
        if (e.getFaction() != null && e.getFaction().length() > 64) {
            return "所属势力不超过 64 字";
        }
        if (e.getTags() != null && e.getTags().length() > 255) {
            return "关键词标签不超过 255 字（英文逗号分隔）";
        }
        if (e.getHotScore() != null && e.getHotScore() < 0) {
            return "热度权重不能为负数";
        }
        if (e.getSortNo() != null && e.getSortNo() < 0) {
            return "排序值不能为负数";
        }
        if (e.getStatus() != null && e.getStatus() != 0 && e.getStatus() != 1) {
            return "状态只能是上架或下架";
        }
        return null;
    }

    private String categoryName(Map<Long, Category> categories, Long id) {
        Category c = categories.get(id);
        return c == null ? "未分类" : c.getName();
    }
}
