package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Entry;
import com.fenglin.springboottest.mapper.projection.CategoryCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EntryMapper extends BaseMapper<Entry> {

    /**
     * 按分类统计上架档案数：一次查询拿全，避免分类树逐节点 count。
     */
    @Select("SELECT category_id AS categoryId, COUNT(*) AS cnt FROM star_entry WHERE status = 1 GROUP BY category_id")
    List<CategoryCount> countGroupByCategory();

    /**
     * 全站累计浏览量，用于首页概览。
     */
    @Select("SELECT IFNULL(SUM(view_count), 0) FROM star_entry WHERE status = 1")
    Long sumViewCount();
}
