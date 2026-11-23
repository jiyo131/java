package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 「涂山拍卖行」数据：竞拍中的拍品 + 成交/流拍纪录 + 我的夺宝 + 个人统计。
 */
@Data
public class AuctionOverview {

    /** 竞拍中的拍品（按结拍时间升序，先拍先结） */
    private List<LotCard> lots;

    /** 成交 / 流拍纪录（倒序，最近 20 条） */
    private List<DealCard> deals;

    /** 我的夺宝（我得主的成交记录，倒序） */
    private List<DealCard> myWon;

    /** 当前情绪值（竞价货币余额） */
    private Integer emotion = 0;

    /** 我当前领先的场次数 */
    private Integer leadingCount = 0;

    /** 我历史夺宝总数（含一口价） */
    private Integer wonCount = 0;

    /** 我累计出价次数 */
    private Integer bidCount = 0;

    /** 服务端时间（前端据此做倒计时对表） */
    private String serverTime;

    /**
     * 竞拍中的拍品卡。
     */
    @Data
    public static class LotCard {

        private Long id;

        private String code;

        private String name;

        private String subtitle;

        private String description;

        private String glyph;

        private String icon;

        private String color;

        private Integer basePrice;

        private Integer step;

        /** 一口价（0 = 不设一口价） */
        private Integer buyoutPrice;

        /** 下一口最低出价（无领先者 = 底价；有领先者 = 当前价 + 加价幅度） */
        private Integer minBid;

        /** 当前最高价（无出价时等于底价展示） */
        private Integer currentPrice;

        /** 领先者用户 id（我领先时前端亮「由我领先」） */
        private Long leaderId;

        /** 领先者昵称 / 账号（无人出价为 null） */
        private String leaderName;

        /** 结拍时间（yyyy-MM-dd HH:mm:ss） */
        private String endAt;

        /** 剩余秒数（按 serverTime 对表后计算） */
        private Long remainingSeconds;

        /** 本场已出价笔数 */
        private Integer bidCount;

        /** 是否由我领先 */
        private Boolean mine;
    }

    /**
     * 成交 / 流拍纪录卡（全场纪录与「我的夺宝」共用）。
     */
    @Data
    public static class DealCard {

        private Long id;

        private Long lotId;

        private String name;

        private String glyph;

        private String color;

        private Integer price;

        /** 1 竞价成交 / 2 一口价 / 3 流拍 */
        private Integer dealType;

        /** 文案：竞价成交 / 一口价 / 流拍 */
        private String dealText;

        /** 得主昵称 / 账号（流拍为 null） */
        private String winnerName;

        /** 是否我得主的 */
        private Boolean mine;

        private String createdAt;
    }

    /**
     * 某件拍品的出价记录（倒序，最近 30 条）。
     */
    @Data
    public static class BidCard {

        private Long id;

        /** 出价人昵称 / 账号 */
        private String username;

        private Integer amount;

        /** 是否我出的价 */
        private Boolean mine;

        private String createdAt;
    }
}
