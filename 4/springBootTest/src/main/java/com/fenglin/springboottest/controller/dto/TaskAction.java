package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 任务操作结果：更新后的卡片 + 最新看板数据 + 本次获得情绪值。
 */
@Data
public class TaskAction {

    private TaskCard task;

    private TaskOverview overview;

    /** 本次获得情绪值（仅完成委托时有值） */
    private Integer gained = 0;

    /** 本次结算是否让情绪阶位提升（前端据此播放升阶动效） */
    private Boolean levelUp = false;

    /** 结算后的阶位名称，便于前端直接提示 */
    private String levelTitle;

    private String message;
}
