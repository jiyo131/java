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

    private String message;
}
