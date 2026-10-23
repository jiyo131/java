package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页返回结构。
 */
@Data
public class PageResult<T> {

    private List<T> list;

    private long total;

    private int page;

    private int size;

    /** 总页数 */
    private int pages;

    /** 是否还有下一页（前端「加载更多」用） */
    private boolean hasMore;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> r = new PageResult<>();
        r.setList(list == null ? Collections.emptyList() : list);
        r.setTotal(total);
        r.setPage(page);
        r.setSize(size);
        int pages = size <= 0 ? 0 : (int) ((total + size - 1) / size);
        r.setPages(pages);
        r.setHasMore((long) page * size < total);
        return r;
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return of(Collections.emptyList(), 0, page, size);
    }
}
