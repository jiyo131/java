package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拍卖成交 / 流拍留档：只增不改。deal_type：1 竞价成交 / 2 一口价 / 3 流拍。
 * 「我的夺宝」= winner_user_id = 我的 1/2 记录；流拍仅作行史，winner 为 NULL。
 */
@Data
@TableName("star_auction_deal")
public class AuctionDeal {

    public static final int TYPE_BID = 1;
    public static final int TYPE_BUYOUT = 2;
    public static final int TYPE_PASSED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拍品 id */
    private Long lotId;

    /** 拍品名称快照 */
    private String name;

    /** 拍品图标快照 */
    private String glyph;

    /** 拍品主色快照 */
    private String color;

    /** 成交价（流拍为 0） */
    private Integer price;

    /** 1 竞价成交 / 2 一口价 / 3 流拍 */
    private Integer dealType;

    /** 得主用户 id（流拍为 NULL） */
    private Long winnerUserId;

    private LocalDateTime createdAt;
}
