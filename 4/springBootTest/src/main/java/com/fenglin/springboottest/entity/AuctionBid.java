package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拍卖出价流水：只增不改。出价即扣情绪值、被超越全额退款（退款不落此表）。
 */
@Data
@TableName("star_auction_bid")
public class AuctionBid {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拍品 id */
    private Long lotId;

    /** 出价人 id */
    private Long userId;

    /** 出价（情绪值） */
    private Integer amount;

    private LocalDateTime createdAt;
}
