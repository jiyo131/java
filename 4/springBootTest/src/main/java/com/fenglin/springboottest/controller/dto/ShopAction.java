package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 兑换动作结果：刚兑到的商品 + 刷新后的商店看板。
 */
@Data
public class ShopAction {

    /** 刚兑到的商品 */
    private ShopCard item;

    /** 刷新后的看板（情绪值 / 库存 / 我的记录均为最新） */
    private ShopOverview overview;

    /** 本次消耗的情绪值 */
    private Integer spent = 0;

    /** 兑换结果文案 */
    private String message;
}
