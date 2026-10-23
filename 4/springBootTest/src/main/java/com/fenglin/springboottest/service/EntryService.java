package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.controller.dto.EntryCard;
import com.fenglin.springboottest.controller.dto.EntryDetail;
import com.fenglin.springboottest.controller.dto.PageResult;
import com.fenglin.springboottest.entity.Entry;

import java.util.List;

public interface EntryService extends IService<Entry> {

    /**
     * 首页 / 搜索：关键词 + 分类（含子孙分类）+ 排序 + 手工分页。
     *
     * @param keyword    检索词（姓名 / 称号 / 简介 / 势力 / 标签 / 台词 / 羁绊）
     * @param categoryId 分类 id，为空表示全部
     * @param sort       default 默认 / hot 最热 / new 最新 / name 按名称
     * @param recordLog  是否写入检索日志（打字联想不记录，显式检索才记录热门词）
     * @param userId     当前登录用户，可为空
     */
    PageResult<EntryCard> search(String keyword, Long categoryId, String sort,
                                 int page, int size, Long userId, boolean recordLog);

    /**
     * 档案详情；每次调用累加浏览量，找不到或已下架返回 null。
     */
    EntryDetail detail(Long id);

    /**
     * 同分类推荐（排除自身）。
     */
    List<EntryCard> related(Long categoryId, Long excludeId, int limit);

    /**
     * 编辑推荐：按热度、浏览量排序。
     */
    List<EntryCard> featured(int limit);
}
