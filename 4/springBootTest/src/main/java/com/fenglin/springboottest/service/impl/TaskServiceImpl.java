package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.controller.dto.TaskAction;
import com.fenglin.springboottest.controller.dto.TaskCard;
import com.fenglin.springboottest.controller.dto.TaskOverview;
import com.fenglin.springboottest.entity.RedLineTask;
import com.fenglin.springboottest.entity.TaskRecord;
import com.fenglin.springboottest.entity.UserEmotion;
import com.fenglin.springboottest.mapper.RedLineTaskMapper;
import com.fenglin.springboottest.mapper.TaskRecordMapper;
import com.fenglin.springboottest.mapper.UserEmotionMapper;
import com.fenglin.springboottest.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 红线仙任务实现。
 *
 * 数据约定：
 *  - star_redline_task   告示板任务（status=1 才展示）
 *  - star_task_record    接取记录，唯一键 (user_id, task_id)：1 进行中 / 2 已圆满
 *  - star_user_emotion   情绪值，只在「完成委托」时累加
 * 「取消接取」删除记录，因此同一委托可以反复接取 / 取消，直到完成为止。
 */
@Service
public class TaskServiceImpl extends ServiceImpl<RedLineTaskMapper, RedLineTask> implements TaskService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 情绪值阶位阈值（升序），与前端展示保持一致 */
    private static final int[] LEVEL_FLOORS = {0, 60, 180, 360, 600};
    private static final String[] LEVEL_TITLES = {"初见红线", "心有灵犀", "红线牵引", "三世情缘", "苦情之证"};
    private static final String[] DIFFICULTY_TEXT = {"轻缘", "深缘", "宿缘"};

    @Autowired
    private TaskRecordMapper recordMapper;

    @Autowired
    private UserEmotionMapper emotionMapper;

    @Override
    public TaskOverview overview(Long userId) {
        List<RedLineTask> tasks = lambdaQuery()
                .eq(RedLineTask::getStatus, 1)
                .orderByAsc(RedLineTask::getSortNo)
                .orderByAsc(RedLineTask::getId)
                .list();

        Map<Long, TaskRecord> records = recordsOf(userId);
        UserEmotion emotion = loadEmotion(userId);

        List<TaskCard> cards = new ArrayList<>(tasks.size());
        int available = 0;
        int accepted = 0;
        int totalReward = 0;
        for (RedLineTask task : tasks) {
            TaskRecord record = records.get(task.getId());
            TaskCard card = toCard(task, record);
            if ("available".equals(card.getState())) {
                available++;
            } else if ("accepted".equals(card.getState())) {
                accepted++;
            }
            totalReward += task.getReward() == null ? 0 : task.getReward();
            cards.add(card);
        }

        TaskOverview vo = new TaskOverview();
        vo.setEmotion(nvl(emotion.getEmotion()));
        vo.setTotalGain(nvl(emotion.getTotalGain()));
        vo.setCompletedCount(nvl(emotion.getCompletedCount()));
        vo.setAcceptedCount(accepted);
        vo.setAvailableCount(available);
        vo.setTotalTasks(cards.size());
        vo.setTotalReward(totalReward);
        vo.setTasks(cards);
        vo.setServerTime(LocalDateTime.now().format(TIME_FMT));
        applyLevel(vo, vo.getEmotion());
        return vo;
    }

    @Override
    public synchronized TaskAction accept(Long userId, Long taskId) {
        RedLineTask task = requireTask(taskId);
        TaskRecord record = findRecord(userId, taskId);
        if (record != null) {
            if (TaskRecord.STATUS_COMPLETED == nvl(record.getStatus())) {
                throw new IllegalStateException("这份委托已圆满，不必再接");
            }
            throw new IllegalStateException("已接下这份委托，先去完成它吧");
        }

        TaskRecord created = new TaskRecord();
        created.setUserId(userId);
        created.setTaskId(task.getId());
        created.setStatus(TaskRecord.STATUS_ACCEPTED);
        created.setAcceptedAt(LocalDateTime.now());
        try {
            recordMapper.insert(created);
        } catch (DuplicateKeyException e) {
            // 并发下唯一键兜底：说明另一请求已经接下
            throw new IllegalStateException("已接下这份委托，先去完成它吧");
        }

        return buildAction(userId, task, 0, "已接下「" + task.getTitle() + "」，别误了时辰");
    }

    @Override
    public synchronized TaskAction cancel(Long userId, Long taskId) {
        RedLineTask task = requireTask(taskId);
        TaskRecord record = findRecord(userId, taskId);
        if (record == null) {
            throw new IllegalStateException("尚未接取这份委托，无需取消");
        }
        if (TaskRecord.STATUS_COMPLETED == nvl(record.getStatus())) {
            throw new IllegalStateException("已圆满的委托不可取消");
        }
        recordMapper.deleteById(record.getId());
        return buildAction(userId, task, 0, "已取消接取，委托重新挂回苦情树");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized TaskAction complete(Long userId, Long taskId) {
        RedLineTask task = requireTask(taskId);
        TaskRecord record = findRecord(userId, taskId);
        if (record == null) {
            throw new IllegalStateException("尚未接取这份委托，先接下再来完成");
        }
        if (TaskRecord.STATUS_COMPLETED == nvl(record.getStatus())) {
            throw new IllegalStateException("这份委托已经圆满了");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setStatus(TaskRecord.STATUS_COMPLETED);
        record.setCompletedAt(now);
        recordMapper.updateById(record);

        int reward = task.getReward() == null ? 0 : task.getReward();
        UserEmotion emotion = emotionMapper.selectById(userId);
        if (emotion == null) {
            emotion = new UserEmotion();
            emotion.setUserId(userId);
            emotion.setEmotion(reward);
            emotion.setTotalGain(reward);
            emotion.setCompletedCount(1);
            emotion.setUpdatedAt(now);
            emotionMapper.insert(emotion);
        } else {
            emotion.setEmotion(nvl(emotion.getEmotion()) + reward);
            emotion.setTotalGain(nvl(emotion.getTotalGain()) + reward);
            emotion.setCompletedCount(nvl(emotion.getCompletedCount()) + 1);
            emotion.setUpdatedAt(now);
            emotionMapper.updateById(emotion);
        }

        return buildAction(userId, task, reward,
                "「" + task.getTitle() + "」已圆满，情绪值 +" + reward);
    }

    /* ================= 内部辅助 ================= */

    private RedLineTask requireTask(Long taskId) {
        RedLineTask task = taskId == null ? null : getById(taskId);
        if (task == null || nvl(task.getStatus()) != 1) {
            throw new IllegalStateException("委托已下架或不存在");
        }
        return task;
    }

    private TaskRecord findRecord(Long userId, Long taskId) {
        return recordMapper.selectOne(Wrappers.lambdaQuery(TaskRecord.class)
                .eq(TaskRecord::getUserId, userId)
                .eq(TaskRecord::getTaskId, taskId)
                .last("LIMIT 1"));
    }

    private Map<Long, TaskRecord> recordsOf(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        List<TaskRecord> list = recordMapper.selectList(Wrappers.lambdaQuery(TaskRecord.class)
                .eq(TaskRecord::getUserId, userId));
        Map<Long, TaskRecord> map = new HashMap<>(list.size() * 2);
        for (TaskRecord record : list) {
            map.put(record.getTaskId(), record);
        }
        return map;
    }

    private UserEmotion loadEmotion(Long userId) {
        UserEmotion emotion = userId == null ? null : emotionMapper.selectById(userId);
        if (emotion == null) {
            emotion = new UserEmotion();
            emotion.setUserId(userId);
            emotion.setEmotion(0);
            emotion.setTotalGain(0);
            emotion.setCompletedCount(0);
        }
        return emotion;
    }

    private TaskAction buildAction(Long userId, RedLineTask task, int gained, String message) {
        TaskAction action = new TaskAction();
        action.setTask(toCard(task, findRecord(userId, task.getId())));
        action.setOverview(overview(userId));
        action.setGained(gained);
        action.setMessage(message);
        return action;
    }

    private TaskCard toCard(RedLineTask task, TaskRecord record) {
        TaskCard card = new TaskCard();
        card.setId(task.getId());
        card.setCode(task.getCode());
        card.setTitle(task.getTitle());
        card.setSubtitle(task.getSubtitle());
        card.setDescription(task.getDescription());
        card.setClient(task.getClient());
        card.setGlyph(task.getGlyph() == null || task.getGlyph().isEmpty() ? "🧵" : task.getGlyph());
        card.setColor(task.getColor() == null || task.getColor().isEmpty() ? "#ff9eb5" : task.getColor());
        card.setDifficulty(nvl(task.getDifficulty()));
        card.setDifficultyText(difficultyText(nvl(task.getDifficulty())));
        card.setReward(nvl(task.getReward()));
        card.setDuration(task.getDuration());
        card.setScene(task.getScene());
        card.setTags(splitTags(task.getTags()));

        if (record == null) {
            card.setState("available");
        } else if (TaskRecord.STATUS_COMPLETED == nvl(record.getStatus())) {
            card.setState("completed");
        } else {
            card.setState("accepted");
        }
        card.setAcceptedAt(format(record == null ? null : record.getAcceptedAt()));
        card.setCompletedAt(format(record == null ? null : record.getCompletedAt()));
        return card;
    }

    private void applyLevel(TaskOverview vo, int emotion) {
        int index = 0;
        for (int i = 0; i < LEVEL_FLOORS.length; i++) {
            if (emotion >= LEVEL_FLOORS[i]) {
                index = i;
            }
        }
        int floor = LEVEL_FLOORS[index];
        Integer next = index + 1 < LEVEL_FLOORS.length ? LEVEL_FLOORS[index + 1] : null;

        vo.setLevelIndex(index + 1);
        vo.setLevelTitle(LEVEL_TITLES[index]);
        vo.setLevelFloor(floor);
        if (next == null) {
            vo.setNextThreshold(null);
            vo.setNextLevelTitle(null);
            vo.setRemaining(0);
            vo.setProgress(100);
        } else {
            vo.setNextThreshold(next);
            vo.setNextLevelTitle(LEVEL_TITLES[index + 1]);
            vo.setRemaining(Math.max(0, next - emotion));
            int span = next - floor;
            int pct = span <= 0 ? 100 : (int) Math.round((emotion - floor) * 100.0 / span);
            vo.setProgress(Math.max(0, Math.min(100, pct)));
        }
    }

    private static String difficultyText(int difficulty) {
        int idx = Math.max(1, Math.min(3, difficulty)) - 1;
        return DIFFICULTY_TEXT[idx];
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(list::add);
        return list;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FMT);
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
