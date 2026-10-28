package com.fenglin.springboottest.controller.admin;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.CategoryNode;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 后台 · 分类管理（首页导航栏分类筛选的数据来源）。
 */
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private EntryService entryService;

    /** 分类树（含停用分类，供管理端管理；前台展示只看启用的） */
    @GetMapping("/tree")
    public Result<java.util.List<CategoryNode>> tree(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        return Result.ok(categoryService.treeAll());
    }

    /** 新增分类 */
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "Authorization", required = false) String auth,
                               @RequestBody Category category) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        String err = validate(category, null);
        if (err != null) {
            return Result.fail(err);
        }
        Category c = category;
        c.setName(c.getName().trim());
        c.setCode(c.getCode().trim().toLowerCase(Locale.ROOT));
        c.setCreatedAt(LocalDateTime.now());
        if (c.getStatus() == null) {
            c.setStatus(1);
        }
        categoryService.save(c);
        return Result.ok();
    }

    /** 修改分类 */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id, @RequestBody Category category) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        Category old = id == null ? null : categoryService.getById(id);
        if (old == null) {
            return Result.fail(404, "分类不存在，可能已被删除");
        }
        String err = validate(category, id);
        if (err != null) {
            return Result.fail(err);
        }
        if (category.getParentId() != null && category.getParentId().equals(id)) {
            return Result.fail("父分类不能选择自己");
        }
        category.setId(old.getId());
        category.setName(category.getName().trim());
        category.setCode(category.getCode().trim().toLowerCase(Locale.ROOT));
        categoryService.updateById(category);
        return Result.ok();
    }

    /** 删除分类（有子分类或仍有档案时拒绝，防止首页导航悬空） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        if (id == null || categoryService.getById(id) == null) {
            return Result.fail(404, "分类不存在，可能已被删除");
        }
        long children = categoryService.lambdaQuery().eq(Category::getParentId, id).count();
        if (children > 0) {
            return Result.fail("该分类下还有子分类，请先删除或转移子分类");
        }
        long entries = entryService.lambdaQuery().eq(Entry::getCategoryId, id).count();
        if (entries > 0) {
            return Result.fail("该分类下还有 " + entries + " 条档案，请先转移或删除档案");
        }
        categoryService.removeById(id);
        return Result.ok();
    }

    // ------------------------------------------------------------------

    private String validate(Category c, Long excludeId) {
        if (c == null) {
            return "请求体不能为空";
        }
        String name = c.getName() == null ? "" : c.getName().trim();
        if (name.isEmpty() || name.length() > 48) {
            return "分类名称必填，且不超过 48 字";
        }
        String code = c.getCode() == null ? "" : c.getCode().trim().toLowerCase(Locale.ROOT);
        if (code.isEmpty() || !code.matches("[a-z0-9_-]{2,48}")) {
            return "分类编码必填，2-48 位小写字母 / 数字 / 中划线 / 下划线";
        }
        Category same = categoryService.lambdaQuery().eq(Category::getCode, code).one();
        if (same != null && !same.getId().equals(excludeId)) {
            return "分类编码已被使用：" + code;
        }
        Long parentId = c.getParentId() == null ? 0L : c.getParentId();
        int level;
        if (parentId == 0L) {
            level = 1;
        } else {
            Category parent = categoryService.getById(parentId);
            if (parent == null) {
                return "所选父分类不存在";
            }
            if (excludeId != null && excludeId.equals(parent.getId())) {
                return "父分类不能选择自己";
            }
            level = (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
            if (level > 3) {
                return "最多支持三级分类";
            }
        }
        c.setParentId(parentId);
        c.setLevel(level);
        if (c.getColor() != null && !c.getColor().isBlank()
                && !c.getColor().trim().matches("^#[0-9a-fA-F]{6}$")) {
            return "分类主色需为 #RRGGBB 格式";
        }
        if (c.getIcon() != null && c.getIcon().length() > 16) {
            return "分类图标不超过 16 字";
        }
        if (c.getDescription() != null && c.getDescription().length() > 255) {
            return "分类说明不超过 255 字";
        }
        if (c.getSortNo() != null && c.getSortNo() < 0) {
            return "排序值不能为负数";
        }
        if (c.getStatus() != null && c.getStatus() != 0 && c.getStatus() != 1) {
            return "状态只能是启用或停用";
        }
        return null;
    }
}
