package com.fenglin.springboottest.service;

import com.fenglin.springboottest.controller.dto.GameWhackResult;
import com.fenglin.springboottest.controller.dto.GameWhackSettle;

/**
 * 涂山小游戏：打地鼠。修为管境界，情绪值管钱包，游戏是情绪值的额外来源。
 */
public interface GameService {

    /** 每日局数上限 */
    int DAILY_LIMIT = 3;

    /** 结算一局打地鼠：服务端重算得分并发放情绪值 */
    GameWhackResult settleWhack(Long userId, GameWhackSettle req);

    /** 今日局数 + 情绪值快照 */
    GameWhackResult summaryWhack(Long userId);
}
