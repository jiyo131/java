package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 首页卡片 / 列表项视图。
 */
@Data
public class EntryCard {

    private Long id;

    private String name;

    private String alias;

    private String glyph;

    /** 人物形象图（无图时前端回退 glyph 图标） */
    private String avatar;

    private String intro;

    private List<String> tags;

    private String faction;

    private Long categoryId;

    /** 分类名称 */
    private String category;

    /** 分类主色，前端用于角标与卡片光效 */
    private String categoryColor;

    private Integer hotScore;

    private Integer viewCount;
}
