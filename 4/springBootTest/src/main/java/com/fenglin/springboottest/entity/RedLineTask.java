package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 红线仙任务：首页「红线仙任务」告示板上的委托。
 */
@Data
@TableName("star_redline_task")
public class RedLineTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编码（唯一） */
    private String code;

    /** 任务名称 */
    private String title;

    /** 副标题 / 一句话说明 */
    private String subtitle;

    /** 委托详情 */
    private String description;

    /** 委托方 */
    private String client;

    /** 任务图标（emoji） */
    private String glyph;

    /** 任务主色（#RRGGBB），驱动前端卡片光效 */
    private String color;

    /** 难度：1 轻缘 / 2 深缘 / 3 宿缘 */
    private Integer difficulty;

    /** 完成可得情绪值 */
    private Integer reward;

    /** 时限描述 */
    private String duration;

    /** 委托地点 */
    private String scene;

    /** 关键词标签，英文逗号分隔 */
    private String tags;

    /** 排序（小值优先） */
    private Integer sortNo;

    /** 1 可接取 / 0 下架 */
    private Integer status;

    private LocalDateTime createdAt;
}
