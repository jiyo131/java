package com.fenglin.springboottest.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.PageResult;
import com.fenglin.springboottest.entity.RedLineTask;
import com.fenglin.springboottest.entity.TaskRecord;
import com.fenglin.springboottest.mapper.RedLineTaskMapper;
import com.fenglin.springboottest.mapper.TaskRecordMapper;
import com.fenglin.springboottest.service.AdminGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 后台 · 任务管理（首页「红线仙任务」告示板数据的增删改查）。
 *
 * <p>难度：1 轻缘 / 2 深缘 / 3 宿缘；status：1 可接取 / 0 下架。
 * 已有用户接取记录的任务不允许删除（避免 star_task_record 悬空），可下架替代。</p>
 */
@RestController
@RequestMapping("/api/admin/tasks")
public class AdminTaskController {

    private static final int MAX_SIZE = 50;

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private RedLineTaskMapper taskMapper;

    @Autowired
    private TaskRecordMapper taskRecordMapper;

    /** 任务列表：关键词 / 难度 / 状态筛选 + 分页（含下架数据） */
    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "-1") int difficulty,
            @RequestParam(required = false, defaultValue = "-1") int status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }

        int p = Math.max(1, page);
        int s = size <= 0 ? 10 : Math.min(size, MAX_SIZE);

        LambdaQueryWrapper<RedLineTask> qw = Wrappers.lambdaQuery(RedLineTask.class);
        if (difficulty == 1 || difficulty == 2 || difficulty == 3) {
            qw.eq(RedLineTask::getDifficulty, difficulty);
        }
        if (status == 0 || status == 1) {
            qw.eq(RedLineTask::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().replaceAll("[%_\\\\]", "");
            qw.and(w -> w.like(RedLineTask::getTitle, kw)
                    .or().like(RedLineTask::getCode, kw)
                    .or().like(RedLineTask::getSubtitle, kw)
                    .or().like(RedLineTask::getClient, kw)
                    .or().like(RedLineTask::getTags, kw));
        }

        long total = taskMapper.selectCount(qw);
        qw.orderByAsc(RedLineTask::getSortNo).orderByAsc(RedLineTask::getId)
                .last("LIMIT " + s + " OFFSET " + (long) (p - 1) * s);
        List<RedLineTask> rows = taskMapper.selectList(qw);

        List<Map<String, Object>> list = rows.stream().map(t -> {
            long accepted = taskRecordMapper.selectCount(
                    Wrappers.lambdaQuery(TaskRecord.class).eq(TaskRecord::getTaskId, t.getId()));
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("code", t.getCode());
            m.put("title", t.getTitle());
            m.put("subtitle", t.getSubtitle());
            m.put("description", t.getDescription());
            m.put("client", t.getClient());
            m.put("glyph", t.getGlyph());
            m.put("icon", t.getIcon());
            m.put("color", t.getColor());
            m.put("difficulty", t.getDifficulty());
            m.put("reward", t.getReward());
            m.put("duration", t.getDuration());
            m.put("scene", t.getScene());
            m.put("tags", t.getTags());
            m.put("sortNo", t.getSortNo());
            m.put("status", t.getStatus());
            m.put("acceptedCount", accepted);
            m.put("createdAt", t.getCreatedAt());
            return m;
        }).toList();
        return Result.ok(PageResult.of(list, total, p, s));
    }

    /** 新增任务 */
    @PostMapping
    public Result<Void> create(@RequestHeader(value = "Authorization", required = false) String auth,
                               @RequestBody RedLineTask task) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        String err = validate(task, null);
        if (err != null) {
            return Result.fail(err);
        }
        if (taskMapper.selectCount(Wrappers.lambdaQuery(RedLineTask.class)
                .eq(RedLineTask::getCode, task.getCode().trim())) > 0) {
            return Result.fail("任务编码已存在：" + task.getCode().trim());
        }
        normalize(task);
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return Result.ok();
    }

    /** 修改任务 */
    @PutMapping("/{id}")
    public Result<Void> update(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id, @RequestBody RedLineTask task) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        RedLineTask old = id == null ? null : taskMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "任务不存在，可能已被删除");
        }
        String err = validate(task, id);
        if (err != null) {
            return Result.fail(err);
        }
        normalize(task);
        task.setId(old.getId());
        task.setCreatedAt(null); // 不覆盖创建时间
        taskMapper.updateById(task);
        return Result.ok();
    }

    /** 上架（可接取）/ 下架 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable("id") Long id,
                                     @RequestBody Map<String, Object> body) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        RedLineTask old = id == null ? null : taskMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "任务不存在，可能已被删除");
        }
        int status;
        try {
            status = Integer.parseInt(String.valueOf(body.get("status")));
        } catch (Exception ex) {
            return Result.fail("状态参数不正确");
        }
        if (status != 0 && status != 1) {
            return Result.fail("状态参数不正确");
        }
        RedLineTask patch = new RedLineTask();
        patch.setId(old.getId());
        patch.setStatus(status);
        taskMapper.updateById(patch);
        return Result.ok();
    }

    /** 删除任务（已有用户接取记录时拒绝，可用下架替代） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        RedLineTask old = id == null ? null : taskMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "任务不存在，可能已被删除");
        }
        long records = taskRecordMapper.selectCount(
                Wrappers.lambdaQuery(TaskRecord.class).eq(TaskRecord::getTaskId, id));
        if (records > 0) {
            return Result.fail("该任务已有 " + records + " 条接取记录，不能删除；可改为「下架」隐藏");
        }
        taskMapper.deleteById(id);
        return Result.ok();
    }

    // ------------------------------------------------------------------

    /** 字段校验；duplicateId 用于更新时排除自身后查编码重复。返回 null 表示通过。 */
    private String validate(RedLineTask t, Long duplicateId) {
        if (t == null) {
            return "请求体不能为空";
        }
        String code = t.getCode() == null ? "" : t.getCode().trim();
        if (code.isEmpty() || code.length() > 48) {
            return "任务编码必填，且不超过 48 字";
        }
        if (!code.matches("[A-Za-z0-9_-]+")) {
            return "任务编码只能包含字母、数字、下划线或中划线";
        }
        RedLineTask same = taskMapper.selectOne(
                Wrappers.lambdaQuery(RedLineTask.class).eq(RedLineTask::getCode, code));
        if (same != null && !same.getId().equals(duplicateId)) {
            return "任务编码已存在：" + code;
        }
        String title = t.getTitle() == null ? "" : t.getTitle().trim();
        if (title.isEmpty() || title.length() > 64) {
            return "任务名称必填，且不超过 64 字";
        }
        if (t.getDifficulty() == null) {
            t.setDifficulty(1);
        }
        if (t.getDifficulty() < 1 || t.getDifficulty() > 3) {
            return "难度只能是 1 轻缘 / 2 深缘 / 3 宿缘";
        }
        if (t.getReward() == null) {
            t.setReward(10);
        }
        if (t.getReward() < 0) {
            return "情绪值奖励不能为负数";
        }
        if (t.getSubtitle() != null && t.getSubtitle().length() > 128) {
            return "副标题不超过 128 字";
        }
        if (t.getDescription() != null && t.getDescription().length() > 500) {
            return "委托详情不超过 500 字";
        }
        if (t.getClient() != null && t.getClient().length() > 48) {
            return "委托方不超过 48 字";
        }
        if (t.getGlyph() != null && t.getGlyph().length() > 16) {
            return "任务图标不超过 16 字";
        }
        if (t.getIcon() != null && t.getIcon().length() > 255) {
            return "任务图标图片路径不超过 255 字";
        }
        if (t.getColor() != null && t.getColor().length() > 16) {
            return "任务主色不超过 16 字";
        }
        if (t.getDuration() != null && t.getDuration().length() > 32) {
            return "时限描述不超过 32 字";
        }
        if (t.getScene() != null && t.getScene().length() > 48) {
            return "委托地点不超过 48 字";
        }
        if (t.getTags() != null && t.getTags().length() > 255) {
            return "关键词标签不超过 255 字（英文逗号分隔）";
        }
        if (t.getSortNo() != null && t.getSortNo() < 0) {
            return "排序值不能为负数";
        }
        if (t.getStatus() != null && t.getStatus() != 0 && t.getStatus() != 1) {
            return "状态只能是可接取或下架";
        }
        return null;
    }

    /** 入库前修剪公共字段 */
    private void normalize(RedLineTask t) {
        t.setCode(t.getCode().trim());
        t.setTitle(t.getTitle().trim());
        if (t.getStatus() == null) {
            t.setStatus(1);
        }
        if (t.getSortNo() == null) {
            t.setSortNo(0);
        }
    }
}
