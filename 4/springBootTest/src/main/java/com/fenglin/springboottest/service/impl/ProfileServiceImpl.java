package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.RealmLadder;
import com.fenglin.springboottest.controller.dto.ProfileVo;
import com.fenglin.springboottest.controller.dto.TaskCard;
import com.fenglin.springboottest.controller.dto.TaskOverview;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.entity.UserCultivation;
import com.fenglin.springboottest.entity.UserEmotion;
import com.fenglin.springboottest.mapper.UserCultivationMapper;
import com.fenglin.springboottest.mapper.UserEmotionMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.ProfileService;
import com.fenglin.springboottest.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 「我的主页」实现。
 *
 * 境界体系（2026-11-16 起）：两条境界线改由「累计修为」驱动（star_user_cultivation.total_cultivation，
 * 只增不减，见 RealmLadder），与情绪值（商店货币）彻底分离，兑换消费不再掉境界：
 *  - 修为来源：每日打坐 + 服用丹药/参悟功法（CultivationService）
 *  - 修士境界：练气期 → 筑基期 → 金丹期 → 元婴期 → 化神期
 *  - 妖族境界：幼妖期 → 妖卒期 → 妖将期 → 妖王期 → 妖皇期
 * 用户可择一为主修阵营（star_user_emotion.camp），主页默认高亮该境界线。
 *
 * 接取情况：复用 TaskService.overview 的任务卡状态（accepted / completed）。
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final int CAMP_CULTIVATOR = 1;
    public static final int CAMP_DEMON = 2;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserEmotionMapper emotionMapper;

    @Autowired
    private UserCultivationMapper cultivationMapper;

    @Autowired
    private TaskService taskService;

    @Override
    public ProfileVo profile(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("账号不存在或已失效");
        }

        TaskOverview ov = taskService.overview(userId);
        UserEmotion emotion = emotionMapper.selectOne(Wrappers.lambdaQuery(UserEmotion.class)
                .eq(UserEmotion::getUserId, userId)
                .last("LIMIT 1"));
        UserCultivation cult = cultivationMapper.selectById(userId);
        int cultivation = cult == null ? 0 : nvl(cult.getTotalCultivation());
        int camp = emotion == null || emotion.getCamp() == null ? CAMP_CULTIVATOR
                : (emotion.getCamp() == CAMP_DEMON ? CAMP_DEMON : CAMP_CULTIVATOR);

        ProfileVo vo = new ProfileVo();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname() == null || user.getNickname().isEmpty()
                ? user.getUsername() : user.getNickname());
        vo.setCamp(camp);
        vo.setCampText(camp == CAMP_DEMON ? "妖族" : "修士");
        vo.setEmotion(ov.getEmotion());
        vo.setCultivation(cultivation);
        vo.setTotalGain(ov.getTotalGain());
        vo.setCompletedCount(ov.getCompletedCount());

        vo.setCultivatorRealms(RealmLadder.build(RealmLadder.CULTIVATOR_TITLES, cultivation));
        vo.setDemonRealms(RealmLadder.build(RealmLadder.DEMON_TITLES, cultivation));

        List<TaskCard> accepted = new ArrayList<>();
        List<TaskCard> completed = new ArrayList<>();
        if (ov.getTasks() != null) {
            for (TaskCard card : ov.getTasks()) {
                if ("accepted".equals(card.getState())) {
                    accepted.add(card);
                } else if ("completed".equals(card.getState())) {
                    completed.add(card);
                }
            }
        }
        vo.setAcceptedTasks(accepted);
        vo.setCompletedTasks(completed);
        vo.setAcceptedCount(accepted.size());
        vo.setServerTime(LocalDateTime.now().format(TIME_FMT));
        return vo;
    }

    @Override
    public ProfileVo setCamp(Long userId, Integer camp) {
        if (camp == null || (camp != CAMP_CULTIVATOR && camp != CAMP_DEMON)) {
            throw new IllegalStateException("阵营只能是 修士 或 妖族");
        }
        UserEmotion emotion = emotionMapper.selectById(userId);
        if (emotion == null) {
            emotion = new UserEmotion();
            emotion.setUserId(userId);
            emotion.setEmotion(0);
            emotion.setTotalGain(0);
            emotion.setCompletedCount(0);
            emotion.setCamp(camp);
            emotion.setUpdatedAt(LocalDateTime.now());
            emotionMapper.insert(emotion);
        } else {
            emotion.setCamp(camp);
            emotion.setUpdatedAt(LocalDateTime.now());
            emotionMapper.updateById(emotion);
        }
        return profile(userId);
    }

    /* ================= 内部辅助 ================= */

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
