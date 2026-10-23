package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SearchLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SearchLogMapper extends BaseMapper<SearchLog> {

    /**
     * 近 30 天热门检索词：按检索次数排序，次数相同则取最近一次检索时间。
     */
    @Select("SELECT keyword FROM star_search_log "
            + "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) "
            + "GROUP BY keyword ORDER BY COUNT(*) DESC, MAX(created_at) DESC LIMIT #{limit}")
    List<String> topKeywords(@Param("limit") int limit);
}
