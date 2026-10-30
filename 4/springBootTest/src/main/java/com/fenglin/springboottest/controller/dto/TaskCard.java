package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 红线仙任务卡片视图（含当前用户的接取状态）。
 */
@Data
public class TaskCard {

    private Long id;

    private String code;

    private String title;

    private String subtitle;

    private String description;

    /** 委托方 */
    private String client;

    private String glyph;

    /** 任务主色，前端用于卡片光效 */
    private String color;

    /** 难度 1-3 */
    private Integer difficulty;

    /** 难度文案：轻缘 / 深缘 / 宿缘 */
    private String difficultyText;

    /** 完成可得情绪值 */
    private Integer reward;

    private String duration;

    private String scene;

    private List<String> tags;

    /** available 待接取 / accepted 进行中 / completed 已圆满 */
    private String state;

    /** 接取时间（yyyy-MM-dd HH:mm） */
    private String acceptedAt;

    /** 完成时间（yyyy-MM-dd HH:mm） */
    private String completedAt;
}
