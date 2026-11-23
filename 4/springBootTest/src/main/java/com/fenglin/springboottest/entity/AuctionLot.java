package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拍卖行拍品：行本身是模板（底价 / 加价幅度 / 一口价 / 场次时长），
 * current_price / leader_user_id / end_time 三个字段是当前场次的实时状态。
 *
 * 轮转机制（AuctionService 懒结算）：结拍后立刻重置为下一场（current_price=底价、
 * leader 清空、end_time=now+duration_min），成交/流拍快照落 star_auction_deal。
 */
@Data
@TableName("star_auction_lot")
public class AuctionLot {

    public static final int ST_ONLINE = 1;
    public static final int ST_OFFLINE = 0;

    /** 结拍前多少秒内出价自动顺延（防狙击） */
    public static final int ANTI_SNIPE_SECONDS = 60;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拍品编码（唯一） */
    private String code;

    /** 拍品名称 */
    private String name;

    /** 副标题 */
    private String subtitle;

    /** 拍品详情 */
    private String description;

    /** 拍品图标（emoji） */
    private String glyph;

    /** 图标图片路径 */
    private String icon;

    /** 主色（#RRGGBB） */
    private String color;

    /** 底价（首拍最低出价） */
    private Integer basePrice;

    /** 加价幅度 */
    private Integer step;

    /** 一口价（0 = 不设一口价） */
    private Integer buyoutPrice;

    /** 每场时长（分钟） */
    private Integer durationMin;

    /** 关键词标签，英文逗号分隔 */
    private String tags;

    /** 排序（小值优先） */
    private Integer sortNo;

    /** 1 上架 / 0 下架 */
    private Integer status;

    /** 当前场次最高价 */
    private Integer currentPrice;

    /** 当前领先者用户 id */
    private Long leaderUserId;

    /** 当前场次结拍时间 */
    private LocalDateTime endTime;

    private LocalDateTime createdAt;
}
