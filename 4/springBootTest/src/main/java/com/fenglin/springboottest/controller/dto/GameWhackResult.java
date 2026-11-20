package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 打地鼠结算 / 查询结果。
 */
@Data
public class GameWhackResult {

    /** 本局实得情绪值（summary 查询时为 0） */
    private Integer reward = 0;

    /** 当前情绪值余额 */
    private Integer emotion = 0;

    /** 累计获得情绪值 */
    private Integer totalGain = 0;

    /** 今日已玩局数 */
    private Integer todayRounds = 0;

    /** 每日局数上限 */
    private Integer dailyLimit;
}
