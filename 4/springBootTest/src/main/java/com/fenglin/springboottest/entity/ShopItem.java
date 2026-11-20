package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 兑换商店商品：法宝 / 丹药 / 功法 三柜，以情绪值为通货。
 */
@Data
@TableName("star_shop_item")
public class ShopItem {

    /** 分类编码：法宝 */
    public static final String CAT_FABAO = "fabao";
    /** 分类编码：丹药 */
    public static final String CAT_DANYAO = "danyao";
    /** 分类编码：功法 */
    public static final String CAT_GONGFA = "gongfa";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商品编码（唯一，便于脚本引用） */
    private String code;

    /** 分类：fabao 法宝 / danyao 丹药 / gongfa 功法 */
    private String category;

    /** 商品名称 */
    private String name;

    /** 副标题 / 一句话说明 */
    private String subtitle;

    /** 商品详情 */
    private String description;

    /** 商品图标（emoji） */
    private String glyph;

    /** 商品图标图片路径（如 /icons/shop-fb-01.png），icon 为空时前端回退 glyph */
    private String icon;

    /** 商品主色（#RRGGBB） */
    private String color;

    /** 兑换所需情绪值 */
    private Integer price;

    /** 服用（丹药）/参悟（功法）可得修为，0 表示不可服用（如法宝） */
    private Integer expGain;

    /** 剩余库存，-1 不限量 */
    private Integer stock;

    /** 关键词标签，英文逗号分隔 */
    private String tags;

    /** 排序（小值优先） */
    private Integer sortNo;

    /** 状态：1 上架 / 0 下架 */
    private Integer status;

    private LocalDateTime createdAt;
}
