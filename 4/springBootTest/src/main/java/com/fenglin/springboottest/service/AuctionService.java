package com.fenglin.springboottest.service;

import com.fenglin.springboottest.controller.dto.AuctionAction;
import com.fenglin.springboottest.controller.dto.AuctionOverview;

import java.util.List;

/**
 * 「涂山拍卖行」服务：竞拍看板 / 出价 / 一口价 / 出价记录。
 * 结拍采用懒结算：任何入口先处理到期场次（成交/流拍 → 自动开下一场）。
 */
public interface AuctionService {

    /** 拍卖行面板：竞拍中拍品 + 成交纪录 + 我的夺宝 + 统计 */
    AuctionOverview overview(Long userId);

    /** 某件拍品的出价记录（倒序，最近 30 条） */
    List<AuctionOverview.BidCard> bids(Long userId, Long lotId);

    /** 出价：出价即扣情绪值，被超越全额退款；结拍前 60 秒内出价自动顺延 60 秒 */
    AuctionAction bid(Long userId, Long lotId, Integer amount);

    /** 一口价：立即成交并开下一场 */
    AuctionAction buyout(Long userId, Long lotId);
}
