package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 首页聚合数据：一次请求拿到分类树、统计、热门搜索与推荐档案，减少首屏请求数。
 */
@Data
public class HomeOverview {

    /** 档案总数 */
    private long entryCount;

    /** 启用的分类数（含各级子分类） */
    private long categoryCount;

    /** 累计浏览量 */
    private long viewCount;

    /** 热门检索词（近 30 天检索日志 + 推荐档案名兜底） */
    private List<String> hotWords;

    /** 多级分类树 */
    private List<CategoryNode> categories;

    /** 编辑推荐档案 */
    private List<EntryCard> featured;

    /** 服务端时间，便于前端展示与排查 */
    private String serverTime;
}
