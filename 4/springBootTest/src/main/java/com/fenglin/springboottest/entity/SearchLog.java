package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检索日志：每次有效检索写一条，用于「热门搜索」与后续搜索分析。
 */
@Data
@TableName("star_search_log")
public class SearchLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 检索词 */
    private String keyword;

    /** 检索用户 id（未登录为 null） */
    private Long userId;

    /** 命中条数 */
    private Integer resultCount;

    private LocalDateTime createdAt;
}
