package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点。children 为下一级分类，count 为该分类及其所有子分类下的档案总数。
 */
@Data
public class CategoryNode {

    private Long id;

    private Long parentId;

    private String code;

    private String name;

    private Integer level;

    private String icon;

    private String color;

    private String description;

    private Integer sortNo;

    /** 本分类 + 所有子分类的档案数 */
    private Long count;

    private List<CategoryNode> children = new ArrayList<>();
}
