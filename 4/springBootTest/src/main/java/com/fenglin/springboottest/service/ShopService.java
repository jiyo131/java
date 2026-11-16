package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.controller.dto.ShopAction;
import com.fenglin.springboottest.controller.dto.ShopOverview;
import com.fenglin.springboottest.entity.ShopItem;

/**
 * 兑换商店：法宝 / 丹药 / 功法 三柜，以情绪值为通货。
 */
public interface ShopService extends IService<ShopItem> {

    /** 看板：上架商品 + 用户情绪值 + 我的兑换记录 */
    ShopOverview overview(Long userId);

    /** 兑换商品：扣情绪值、减库存、落兑换记录 */
    ShopAction redeem(Long userId, Long itemId);
}
