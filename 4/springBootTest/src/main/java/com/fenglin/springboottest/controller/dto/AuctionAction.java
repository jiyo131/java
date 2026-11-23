package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 拍卖行动作结果：一句提示 + 更新后的拍品卡 + 最新面板。
 */
@Data
public class AuctionAction {

    private String message;

    /** 动作涉及拍品的最新状态（出价/一口价） */
    private AuctionOverview.LotCard lot;

    private AuctionOverview overview;
}
