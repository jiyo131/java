package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.controller.dto.ProfileVo;
import com.fenglin.springboottest.controller.dto.TaskCard;
import com.fenglin.springboottest.controller.dto.TaskOverview;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.entity.UserEmotion;
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
 * 境界体系：情绪值（完成红线仙委托累积）同时驱动两条境界线，
 * 修士与妖族各有 5 阶，阈值与红线仙阶位一致（0/60/180/360/600）：
 *  - 修士境界：练气期 → 筑基期 → 金丹期 → 元婴期 → 化神期
 *  - 妖族境界：幼妖期 → 妖卒期 → 妖将期 → 妖王期 → 妖皇期
 * 用户可择一为主修阵营（star_user_emotion.camp），主页默认高亮该境界线。
 *
 * 接取情况：复用 TaskService.overview 的任务卡状态（accepted / completed）。
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 境界阈值（升序），与红线仙阶位共用同一套情绪值 */
    private static final int[] REALM_FLOORS = {0, 60, 180, 360, 600};
    private static final String[] CULTIVATOR_TITLES = {"练气期", "筑基期", "金丹期", "元婴期", "化神期"};
    private static final String[] DEMON_TITLES = {"幼妖期", "妖卒期", "妖将期", "妖王期", "妖皇期"};

    public static final int CAMP_CULTIVATOR = 1;
    public static final int CAMP_DEMON = 2;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserEmotionMapper emotionMapper;

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
        int camp = emotion == null || emotion.getCamp() == null ? CAMP_CULTIVATOR
                : (emotion.getCamp() == CAMP_DEMON ? CAMP_DEMON : CAMP_CULTIVATOR);

        ProfileVo vo = new ProfileVo();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname() == null || user.getNickname().isEmpty()
                ? user.getUsername() : user.getNickname());
        vo.setCamp(camp);
        vo.setCampText(camp == CAMP_DEMON ? "妖族" : "修士");
        vo.setEmotion(ov.getEmotion());
        vo.setTotalGain(ov.getTotalGain());
        vo.setCompletedCount(ov.getCompletedCount());

        vo.setCultivatorRealms(buildLadder(CULTIVATOR_TITLES, ov.getEmotion()));
        vo.setDemonRealms(buildLadder(DEMON_TITLES, ov.getEmotion()));

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

    /** 按情绪值生成一条境界线：标记当前境 / 已达成 / 本境进度 */
    private static List<ProfileVo.RealmNode> buildLadder(String[] titles, int emotion) {
        int currentIdx = 0;
        for (int i = 0; i < REALM_FLOORS.length; i++) {
            if (emotion >= REALM_FLOORS[i]) {
                currentIdx = i;
            }
        }

        List<ProfileVo.RealmNode> list = new ArrayList<>(titles.length);
        for (int i = 0; i < titles.length; i++) {
            ProfileVo.RealmNode node = new ProfileVo.RealmNode();
            node.setIndex(i + 1);
            node.setTitle(titles[i]);
            node.setFloor(REALM_FLOORS[i]);
            boolean reached = emotion >= REALM_FLOORS[i];
            node.setReached(reached);
            node.setCurrent(i == currentIdx);

            Integer next = i + 1 < REALM_FLOORS.length ? REALM_FLOORS[i + 1] : null;
            node.setNextThreshold(next);
            if (next == null) {
                node.setProgress(100);
            } else {
                int span = next - REALM_FLOORS[i];
                int pct = span <= 0 ? 100 : (int) Math.round((emotion - REALM_FLOORS[i]) * 100.0 / span);
                node.setProgress(Math.max(0, Math.min(100, pct)));
            }
            list.add(node);
        }
        return list;
    }
}
