package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.entity.SearchLog;
import com.fenglin.springboottest.mapper.SearchLogMapper;
import com.fenglin.springboottest.service.SearchLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class SearchLogServiceImpl extends ServiceImpl<SearchLogMapper, SearchLog> implements SearchLogService {

    private static final int MAX_KEYWORD_LEN = 30;

    @Override
    public void record(String keyword, Long userId, int resultCount) {
        if (keyword == null) {
            return;
        }
        String kw = keyword.trim();
        if (kw.isEmpty() || kw.length() > MAX_KEYWORD_LEN) {
            return;
        }
        try {
            SearchLog log = new SearchLog();
            log.setKeyword(kw);
            log.setUserId(userId);
            log.setResultCount(Math.max(0, resultCount));
            log.setCreatedAt(LocalDateTime.now());
            save(log);
        } catch (Exception ignore) {
            // 检索日志属于旁路统计，写库失败不影响用户检索
        }
    }

    @Override
    public List<String> hotKeywords(int limit) {
        int l = Math.max(1, Math.min(limit, 20));
        try {
            List<String> list = baseMapper.topKeywords(l);
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
