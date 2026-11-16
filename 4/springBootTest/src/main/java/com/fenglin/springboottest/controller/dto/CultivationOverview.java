package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 修炼面板：累计修为 + 今日打坐状态 + 可服用（丹药/功法）背包。
 */
@Data
public class CultivationOverview {

    /** 累计修为（境界依据） */
    private Integer totalCultivation = 0;

    /** 累计打坐次数 */
    private Integer meditateCount = 0;

    /** 今天是否已打坐 */
    private Boolean meditatedToday = false;

    /** 可服用的丹药 / 功法（未使用的兑换记录） */
    private List<UsableItem> usableItems;

    private String serverTime;

    /** 可服用道具：一条未使用的兑换记录 + 商品信息 */
    @Data
    public static class UsableItem {

        /** 兑换记录 id（服用接口用） */
        private Long recordId;

        private Long itemId;

        private String name;

        private String glyph;

        private String color;

        /** 分类中文名：丹药 / 功法 */
        private String categoryText;

        /** 服用可得修为 */
        private Integer expGain;

        /** 兑入时间 */
        private String obtainedAt;
    }
}
