package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.controller.dto.TaskAction;
import com.fenglin.springboottest.controller.dto.TaskOverview;
import com.fenglin.springboottest.entity.RedLineTask;

/**
 * 红线仙任务：告示板查询 + 接取 / 取消 / 完成（完成结算情绪值）。
 */
public interface TaskService extends IService<RedLineTask> {

    /** 看板数据：任务列表（带当前用户状态）+ 情绪值 + 统计 */
    TaskOverview overview(Long userId);

    /** 接取委托 */
    TaskAction accept(Long userId, Long taskId);

    /** 取消接取（已圆满的不可取消） */
    TaskAction cancel(Long userId, Long taskId);

    /** 完成委托，结算情绪值 */
    TaskAction complete(Long userId, Long taskId);
}
