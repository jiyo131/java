package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页档案条目：首页卡片、搜索、详情都基于这张表。
 */
@Data
@TableName("star_entry")
public class Entry {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分类 id */
    private Long categoryId;

    /** 名称（唯一） */
    private String name;

    /** 称号 / 别名 */
    private String alias;

    /** 卡片图标（emoji），avatar 为空时回退使用 */
    private String glyph;

    /** 人物形象图（前端 /avatars/ 下的图片路径） */
    private String avatar;

    /** 简介 */
    private String intro;

    /** 经典台词 */
    private String quote;

    /** 红线羁绊 */
    private String bond;

    /** 所属势力 */
    private String faction;

    /** 关键词标签，英文逗号分隔 */
    private String tags;

    /** 热度权重（编辑推荐） */
    private Integer hotScore;

    /** 浏览量 */
    private Integer viewCount;

    /** 默认排序，小值优先 */
    private Integer sortNo;

    /** 1 上架 / 0 下架 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
