package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.controller.dto.GameWhackResult;
import com.fenglin.springboottest.controller.dto.GameWhackSettle;
import com.fenglin.springboottest.entity.GameRecord;
import com.fenglin.springboottest.entity.UserEmotion;
import com.fenglin.springboottest.mapper.GameRecordMapper;
import com.fenglin.springboottest.mapper.UserEmotionMapper;
import com.fenglin.springboottest.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 打地鼠结算实现。
 * 服务端只信任「命中次数」这类无法伪造金额的计数并重算得分：
 *   普通小狐 +2 / 金狐 +5，单局上限 90，每日上限 DAILY_LIMIT 局。
 * 情绪值发放逻辑与「完成委托」一致：emotion / total_gain 同步累加，不动 completed_count。
 */
@Service
public class GameServiceImpl implements GameService {

    /** 普通小狐命中得分 */
    private static final int SCORE_PER_HIT = 2;
    /** 金狐命中得分 */
    private static final int SCORE_PER_GOLDEN = 5;
    /** 单局情绪值上限 */
    private static final int MAX_REWARD_PER_ROUND = 90;
    /** 单局命中次数合理上限（客户端 30 秒约最多冒出 26 只） */
    private static final int MAX_HITS = 30;
    /** 一局时长合理区间（毫秒）：游戏固定 30 秒，放宽到 20~120 秒容错网络延迟 */
    private static final long MIN_DURATION_MS = 20_000L;
    private static final long MAX_DURATION_MS = 120_000L;

    @Autowired
    private GameRecordMapper gameRecordMapper;

    @Autowired
    private UserEmotionMapper emotionMapper;

    @Override
    public GameWhackResult settleWhack(Long userId, GameWhackSettle req) {
        int hits = clamp(req == null ? null : req.getHits(), 0, MAX_HITS);
        int golden = clamp(req == null ? null : req.getGoldenHits(), 0, hits);
        long durationMs = req == null || req.getDurationMs() == null ? 0 : req.getDurationMs();

        if (durationMs < MIN_DURATION_MS || durationMs > MAX_DURATION_MS) {
            throw new IllegalStateException("本局时长异常，结算无效");
        }

        LocalDateTime now = LocalDateTime.now();
        if (roundsToday(userId, now) >= DAILY_LIMIT) {
            throw new IllegalStateException("今日「打地鼠」次数已用完（每日 " + DAILY_LIMIT + " 局），明天再来吧～");
        }

        // 服务端重算得分：普通 +2 / 金狐 +5，封顶 MAX_REWARD_PER_ROUND
        int score = (hits - golden) * SCORE_PER_HIT + golden * SCORE_PER_GOLDEN;
        int reward = Math.min(score, MAX_REWARD_PER_ROUND);

        GameRecord record = new GameRecord();
        record.setUserId(userId);
        record.setGameKey("whack");
        record.setScore(score);
        record.setHits(hits);
        record.setGoldenHits(golden);
        record.setDurationMs((int) Math.min(durationMs, Integer.MAX_VALUE));
        record.setReward(reward);
        record.setCreatedAt(now);
        gameRecordMapper.insert(record);

        // 情绪值入账（与完成委托同一张钱包表）
        UserEmotion emotion = emotionMapper.selectById(userId);
        if (emotion == null) {
            emotion = new UserEmotion();
            emotion.setUserId(userId);
            emotion.setEmotion(reward);
            emotion.setTotalGain(reward);
            emotion.setCompletedCount(0);
            emotion.setUpdatedAt(now);
            emotionMapper.insert(emotion);
        } else {
            emotion.setEmotion(nvl(emotion.getEmotion()) + reward);
            emotion.setTotalGain(nvl(emotion.getTotalGain()) + reward);
            emotion.setUpdatedAt(now);
            emotionMapper.updateById(emotion);
        }

        GameWhackResult result = new GameWhackResult();
        result.setReward(reward);
        result.setEmotion(nvl(emotion.getEmotion()));
        result.setTotalGain(nvl(emotion.getTotalGain()));
        // 此时战绩已入库，直接重查当日局数即为最新值
        result.setTodayRounds(roundsToday(userId, now));
        result.setDailyLimit(DAILY_LIMIT);
        return result;
    }

    @Override
    public GameWhackResult summaryWhack(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        GameWhackResult result = new GameWhackResult();
        result.setTodayRounds(roundsToday(userId, now));
        result.setDailyLimit(DAILY_LIMIT);

        UserEmotion emotion = emotionMapper.selectById(userId);
        result.setEmotion(emotion == null ? 0 : nvl(emotion.getEmotion()));
        result.setTotalGain(emotion == null ? 0 : nvl(emotion.getTotalGain()));
        return result;
    }

    /* ================= 内部辅助 ================= */

    /** 当日已玩局数（按本地时区自然日） */
    private int roundsToday(Long userId, LocalDateTime now) {
        LocalDateTime dayStart = LocalDate.from(now).atStartOfDay();
        Long count = gameRecordMapper.selectCount(Wrappers.lambdaQuery(GameRecord.class)
                .eq(GameRecord::getUserId, userId)
                .eq(GameRecord::getGameKey, "whack")
                .ge(GameRecord::getCreatedAt, dayStart)
                .lt(GameRecord::getCreatedAt, dayStart.plusDays(1)));
        return count == null ? 0 : count.intValue();
    }

    private int clamp(Integer v, int min, int max) {
        if (v == null || v < min) {
            return min;
        }
        return Math.min(v, max);
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
