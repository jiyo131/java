package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.controller.dto.CategoryNode;
import com.fenglin.springboottest.entity.Category;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CategoryService extends IService<Category> {

    /**
     * 多级分类树，count 为「本分类 + 全部子分类」的档案数。
     */
    List<CategoryNode> tree();

    /**
     * 当前分类及其全部子孙分类的 id 集合；用于「点一级分类也能带出二级分类内容」。
     */
    Set<Long> selfAndDescendants(Long categoryId);

    /**
     * 分类路径名称，如 [人物档案, 涂山一脉]。
     */
    List<String> pathNames(Long categoryId);

    /**
     * 启用中的分类 id -> 实体，用于列表转换时补充分类名与主色。
     */
    Map<Long, Category> allEnabledMap();
}
