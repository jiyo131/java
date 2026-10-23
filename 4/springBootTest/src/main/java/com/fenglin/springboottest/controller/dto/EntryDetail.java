package com.fenglin.springboottest.controller.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 档案详情：卡片字段 + 台词 / 羁绊 / 分类路径 / 相关推荐。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EntryDetail extends EntryCard {

    private String quote;

    private String bond;

    /** 分类路径，如 [人物档案, 涂山一脉] */
    private List<String> path;

    /** 同分类推荐 */
    private List<EntryCard> related;
}
