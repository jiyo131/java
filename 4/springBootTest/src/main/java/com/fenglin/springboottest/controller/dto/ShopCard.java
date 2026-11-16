package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 兑换商店商品卡片：对应 star_shop_item 一行 + 当前用户的兑换状态。
 */
@Data
public class ShopCard {

    private Long id;

    /** 商品编码 */
    private String code;

    /** 分类：fabao / danyao / gongfa */
    private String category;

    /** 分类中文名：法宝 / 丹药 / 功法 */
    private String categoryText;

    private String name;

    private String subtitle;

    private String description;

    private String glyph;

    private String color;

    /** 兑换所需情绪值 */
    private Integer price;

    /** 服用（丹药）/参悟（功法）可得修为，0 表示不可服用（如法宝） */
    private Integer expGain;

    /** 剩余库存，-1 不限量 */
    private Integer stock;

    /** 是否可兑换（上架且库存不为 0） */
    private Boolean available;

    /** 关键词标签 */
    private List<String> tags;

    /** 当前用户累计兑换件数（不限同一商品） */
    private Integer ownedCount;
}
