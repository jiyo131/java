package com.fenglin.springboottest.mapper.projection;

import lombok.Data;

/**
 * 分类维度聚合结果：category_id -> 档案条数。
 */
@Data
public class CategoryCount {

    private Long categoryId;

    private Long cnt;
}
