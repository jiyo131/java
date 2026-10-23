package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.controller.dto.EntryCard;
import com.fenglin.springboottest.controller.dto.EntryDetail;
import com.fenglin.springboottest.controller.dto.PageResult;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.entity.Entry;
import com.fenglin.springboottest.mapper.EntryMapper;
import com.fenglin.springboottest.service.CategoryService;
import com.fenglin.springboottest.service.EntryService;
import com.fenglin.springboottest.service.SearchLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EntryServiceImpl extends ServiceImpl<EntryMapper, Entry> implements EntryService {

    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 50;
    private static final int MAX_PAGE = 500;
    private static final int MAX_KEYWORD_LEN = 30;
    private static final String DEFAULT_COLOR = "#ff9eb5";

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SearchLogService searchLogService;

    @Override
    public PageResult<EntryCard> search(String keyword, Long categoryId, String sort,
                                        int page, int size, Long userId, boolean recordLog) {
        int p = Math.max(1, Math.min(page, MAX_PAGE));
        int s = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String kw = normalize(keyword);

        LambdaQueryWrapper<Entry> qw = Wrappers.lambdaQuery(Entry.class).eq(Entry::getStatus, 1);

        if (categoryId != null && categoryId > 0) {
            Set<Long> categoryIds = categoryService.selfAndDescendants(categoryId);
            if (categoryIds.isEmpty()) {
                if (recordLog && !kw.isEmpty()) {
                    searchLogService.record(kw, userId, 0);
                }
                return PageResult.empty(p, s);
            }
            qw.in(Entry::getCategoryId, categoryIds);
        }

        if (!kw.isEmpty()) {
            qw.and(w -> w.like(Entry::getName, kw)
                    .or().like(Entry::getAlias, kw)
                    .or().like(Entry::getIntro, kw)
                    .or().like(Entry::getTags, kw)
                    .or().like(Entry::getFaction, kw)
                    .or().like(Entry::getQuote, kw)
                    .or().like(Entry::getBond, kw));
        }

        // 先 count（不带 order by / limit），再分页取数据
        Long counted = baseMapper.selectCount(qw);
        long total = counted == null ? 0L : counted;
        if (recordLog && !kw.isEmpty()) {
            searchLogService.record(kw, userId, (int) total);
        }
        if (total == 0) {
            return PageResult.of(Collections.emptyList(), 0L, p, s);
        }

        applySort(qw, sort);
        long offset = (long) (p - 1) * s;
        qw.last("LIMIT " + s + " OFFSET " + offset);

        List<Entry> rows = baseMapper.selectList(qw);
        Map<Long, Category> categories = categoryService.allEnabledMap();
        List<EntryCard> cards = new ArrayList<>(rows.size());
        for (Entry row : rows) {
            cards.add(toCard(row, categories));
        }
        return PageResult.of(cards, total, p, s);
    }

    @Override
    public EntryDetail detail(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        Entry entry = getById(id);
        if (entry == null || entry.getStatus() == null || entry.getStatus() != 1) {
            return null;
        }
        // 浏览量 +1：单条 SQL 自增，避免读改写并发丢计数
        baseMapper.update(null, Wrappers.lambdaUpdate(Entry.class)
                .eq(Entry::getId, id)
                .setSql("view_count = view_count + 1"));
        entry.setViewCount((entry.getViewCount() == null ? 0 : entry.getViewCount()) + 1);

        Map<Long, Category> categories = categoryService.allEnabledMap();
        EntryDetail vo = new EntryDetail();
        fill(vo, entry, categories);
        vo.setQuote(entry.getQuote());
        vo.setBond(entry.getBond());
        vo.setPath(categoryService.pathNames(entry.getCategoryId()));
        vo.setRelated(related(entry.getCategoryId(), entry.getId(), 3));
        return vo;
    }

    @Override
    public List<EntryCard> related(Long categoryId, Long excludeId, int limit) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        int l = Math.max(1, Math.min(limit, 10));
        LambdaQueryWrapper<Entry> qw = Wrappers.lambdaQuery(Entry.class)
                .eq(Entry::getStatus, 1)
                .eq(Entry::getCategoryId, categoryId)
                .ne(excludeId != null, Entry::getId, excludeId)
                .orderByDesc(Entry::getHotScore)
                .orderByDesc(Entry::getViewCount)
                .last("LIMIT " + l);
        Map<Long, Category> categories = categoryService.allEnabledMap();
        List<EntryCard> list = new ArrayList<>();
        for (Entry e : baseMapper.selectList(qw)) {
            list.add(toCard(e, categories));
        }
        return list;
    }

    @Override
    public List<EntryCard> featured(int limit) {
        int l = Math.max(1, Math.min(limit, 20));
        LambdaQueryWrapper<Entry> qw = Wrappers.lambdaQuery(Entry.class)
                .eq(Entry::getStatus, 1)
                .orderByDesc(Entry::getHotScore)
                .orderByDesc(Entry::getViewCount)
                .last("LIMIT " + l);
        Map<Long, Category> categories = categoryService.allEnabledMap();
        List<EntryCard> list = new ArrayList<>();
        for (Entry e : baseMapper.selectList(qw)) {
            list.add(toCard(e, categories));
        }
        return list;
    }

    private void applySort(LambdaQueryWrapper<Entry> qw, String sort) {
        String s = sort == null ? "" : sort.trim().toLowerCase();
        switch (s) {
            case "hot" -> qw.orderByDesc(Entry::getHotScore).orderByDesc(Entry::getViewCount).orderByAsc(Entry::getId);
            case "new" -> qw.orderByDesc(Entry::getId);
            case "name" -> qw.orderByAsc(Entry::getName).orderByAsc(Entry::getId);
            default -> qw.orderByAsc(Entry::getSortNo).orderByAsc(Entry::getId);
        }
    }

    /**
     * 关键词清洗：去掉 LIKE 通配符，防止用户输入 % / _ 触发全表匹配，并限制长度。
     */
    private String normalize(String keyword) {
        if (keyword == null) {
            return "";
        }
        String kw = keyword.trim().replaceAll("[%_\\\\]", "");
        if (kw.length() > MAX_KEYWORD_LEN) {
            kw = kw.substring(0, MAX_KEYWORD_LEN);
        }
        return kw;
    }

    private EntryCard toCard(Entry e, Map<Long, Category> categories) {
        EntryCard vo = new EntryCard();
        fill(vo, e, categories);
        return vo;
    }

    private void fill(EntryCard vo, Entry e, Map<Long, Category> categories) {
        Category c = categories.get(e.getCategoryId());
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setAlias(e.getAlias());
        vo.setGlyph(e.getGlyph());
        vo.setAvatar(e.getAvatar());
        vo.setIntro(e.getIntro());
        vo.setFaction(e.getFaction());
        vo.setTags(splitTags(e.getTags()));
        vo.setHotScore(e.getHotScore());
        vo.setViewCount(e.getViewCount());
        vo.setCategoryId(e.getCategoryId());
        vo.setCategory(c == null ? "未分类" : c.getName());
        vo.setCategoryColor(c == null || c.getColor() == null || c.getColor().isBlank()
                ? DEFAULT_COLOR : c.getColor());
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (String t : tags.split("[,，、|]")) {
            String v = t.trim();
            if (!v.isEmpty() && list.size() < 6) {
                list.add(v);
            }
        }
        return list;
    }
}
