package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 兑换记录：只增不改，兑换即扣情绪值并落一条记录。
 */
@Data
@TableName("star_shop_record")
public class ShopRecord {

    /** 未使用（法宝恒为此状态） */
    public static final int STATUS_UNUSED = 1;
    /** 已服用（丹药/功法消耗掉，转为修为） */
    public static final int STATUS_USED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 兑换人 id */
    private Long userId;

    /** 商品 id */
    private Long itemId;

    /** 成交时消耗的情绪值 */
    private Integer price;

    /** 1 未使用 / 2 已服用（法宝无服用概念，恒为 1） */
    private Integer used;

    /** 兑换时间 */
    private LocalDateTime createdAt;
}
