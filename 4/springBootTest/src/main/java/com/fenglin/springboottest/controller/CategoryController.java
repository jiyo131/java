package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.CategoryNode;
import com.fenglin.springboottest.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 多级分类接口。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 分类树（一级 -> 二级 -> …），count 含全部子孙分类的档案数。
     */
    @GetMapping("/tree")
    public Result<List<CategoryNode>> tree() {
        return Result.ok(categoryService.tree());
    }
}
