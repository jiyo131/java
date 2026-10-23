package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.entity.SearchLog;

import java.util.List;

public interface SearchLogService extends IService<SearchLog> {

    /**
     * 记录一次检索；日志属旁路能力，内部吞掉异常，绝不影响检索主流程。
     */
    void record(String keyword, Long userId, int resultCount);

    /**
     * 近 30 天热门检索词。
     */
    List<String> hotKeywords(int limit);
}
