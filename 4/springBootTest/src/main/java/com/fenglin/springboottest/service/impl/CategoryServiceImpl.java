package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.controller.dto.CategoryNode;
import com.fenglin.springboottest.entity.Category;
import com.fenglin.springboottest.mapper.CategoryMapper;
import com.fenglin.springboottest.mapper.EntryMapper;
import com.fenglin.springboottest.mapper.projection.CategoryCount;
import com.fenglin.springboottest.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    /** 防御性上限，避免脏数据造成父子死循环 */
    private static final int MAX_DEPTH = 8;

    @Autowired
    private EntryMapper entryMapper;

    @Override
    public List<CategoryNode> tree() {
        return buildTree(true);
    }

    @Override
    public List<CategoryNode> treeAll() {
        return buildTree(false);
    }

    /**
     * @param onlyEnabled true 只含启用分类（前台）；false 含停用分类（管理端）
     */
    private List<CategoryNode> buildTree(boolean onlyEnabled) {
        List<Category> all = lambdaQuery()
                .eq(onlyEnabled, Category::getStatus, 1)
                .orderByAsc(Category::getLevel)
                .orderByAsc(Category::getSortNo)
                .orderByAsc(Category::getId)
                .list();

        Map<Long, Long> counts = new HashMap<>();
        List<CategoryCount> rows = entryMapper.countGroupByCategory();
        if (rows != null) {
            for (CategoryCount row : rows) {
                if (row.getCategoryId() != null) {
                    counts.put(row.getCategoryId(), row.getCnt() == null ? 0L : row.getCnt());
                }
            }
        }

        Map<Long, CategoryNode> nodes = new LinkedHashMap<>();
        for (Category c : all) {
            nodes.put(c.getId(), toNode(c, counts.getOrDefault(c.getId(), 0L)));
        }

        List<CategoryNode> roots = new ArrayList<>();
        for (CategoryNode node : nodes.values()) {
            CategoryNode parent = node.getParentId() == null ? null : nodes.get(node.getParentId());
            if (parent != null && !parent.getId().equals(node.getId())) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        }
        // 自底向上汇总：一级分类的 count 覆盖所有子分类
        for (CategoryNode root : roots) {
            sumUp(root, 1);
        }
        return roots;
    }

    private long sumUp(CategoryNode node, int depth) {
        long total = node.getCount() == null ? 0L : node.getCount();
        if (depth < MAX_DEPTH) {
            for (CategoryNode child : node.getChildren()) {
                total += sumUp(child, depth + 1);
            }
        }
        node.setCount(total);
        return total;
    }

    @Override
    public Set<Long> selfAndDescendants(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return Collections.emptySet();
        }
        List<Category> all = lambdaQuery().eq(Category::getStatus, 1).list();
        Map<Long, List<Long>> childrenOf = new HashMap<>();
        boolean exists = false;
        for (Category c : all) {
            childrenOf.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
            if (categoryId.equals(c.getId())) {
                exists = true;
            }
        }
        if (!exists) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        int guard = 0;
        while (!queue.isEmpty() && guard++ < 1000) {
            Long id = queue.poll();
            if (!result.add(id)) {
                continue;
            }
            List<Long> children = childrenOf.get(id);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return result;
    }

    @Override
    public List<String> pathNames(Long categoryId) {
        Map<Long, Category> map = allEnabledMap();
        List<String> path = new ArrayList<>();
        Long cursor = categoryId;
        int depth = 0;
        while (cursor != null && cursor > 0 && depth++ < MAX_DEPTH) {
            Category c = map.get(cursor);
            if (c == null) {
                break;
            }
            path.add(0, c.getName());
            cursor = c.getParentId();
        }
        return path;
    }

    @Override
    public Map<Long, Category> allEnabledMap() {
        List<Category> all = lambdaQuery().eq(Category::getStatus, 1).list();
        Map<Long, Category> map = new LinkedHashMap<>();
        for (Category c : all) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private CategoryNode toNode(Category c, long count) {
        CategoryNode node = new CategoryNode();
        node.setId(c.getId());
        node.setParentId(c.getParentId());
        node.setCode(c.getCode());
        node.setName(c.getName());
        node.setLevel(c.getLevel());
        node.setIcon(c.getIcon());
        node.setColor(c.getColor());
        node.setDescription(c.getDescription());
        node.setSortNo(c.getSortNo());
        node.setStatus(c.getStatus());
        node.setCount(count);
        return node;
    }
}
