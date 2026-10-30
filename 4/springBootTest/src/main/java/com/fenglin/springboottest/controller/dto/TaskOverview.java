package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 首页「红线仙任务」看板数据：情绪值 + 等级 + 任务列表 + 统计。
 */
@Data
public class TaskOverview {

    /** 当前情绪值 */
    private Integer emotion = 0;

    /** 累计获得情绪值 */
    private Integer totalGain = 0;

    /** 累计完成委托数 */
    private Integer completedCount = 0;

    /** 当前进行中的委托数 */
    private Integer acceptedCount = 0;

    /** 情绪阶位：初见红线 / 心有灵犀 / 红线牵引 / 三世情缘 / 苦情之证 */
    private String levelTitle;

    /** 阶位序号，从 1 开始 */
    private Integer levelIndex = 1;

    /** 当前阶位阈值 */
    private Integer levelFloor = 0;

    /** 下一阶阈值（已至最高阶时为 null） */
    private Integer nextThreshold;

    /** 下一阶名称（已至最高阶时为 null） */
    private String nextLevelTitle;

    /** 距下一阶还需多少情绪值（已至最高阶为 0） */
    private Integer remaining = 0;

    /** 当前阶位进度百分比 0-100 */
    private Integer progress = 0;

    /** 告示板任务总数 */
    private Integer totalTasks = 0;

    /** 待接取数量 */
    private Integer availableCount = 0;

    /** 全站可领情绪值合计 */
    private Integer totalReward = 0;

    private List<TaskCard> tasks;

    private String serverTime;
}
