package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 兑换商店看板：三柜商品 + 用户情绪值 + 我的兑换记录。
 */
@Data
public class ShopOverview {

    /** 当前情绪值（可当钱花） */
    private Integer emotion = 0;

    /** 累计完成委托数 */
    private Integer completedCount = 0;

    /** 情绪阶位名称（与任务页一致） */
    private String levelTitle;

    /** 上架商品总数 */
    private Integer totalItems = 0;

    /** 法宝 / 丹药 / 功法 各柜件数 */
    private Integer fabaoCount = 0;
    private Integer danyaoCount = 0;
    private Integer gongfaCount = 0;

    /** 当前用户累计兑换件数 */
    private Integer ownedCount = 0;

    /** 当前用户累计花费情绪值 */
    private Integer spentTotal = 0;

    private List<ShopCard> items;

    /** 我的兑换记录（按时间倒序，含商品名） */
    private List<ShopMyRecord> myRecords;

    private String serverTime;

    /** 我的兑换记录行 */
    @Data
    public static class ShopMyRecord {
        private Long id;
        private Long itemId;
        private String name;
        private String glyph;
        /** 商品图标图片路径，icon 为空时前端回退 glyph */
        private String icon;
        private String color;
        private String categoryText;
        private Integer price;
        private String createdAt;
    }
}
