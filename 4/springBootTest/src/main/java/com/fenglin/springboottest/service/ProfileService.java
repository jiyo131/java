package com.fenglin.springboottest.service;

import com.fenglin.springboottest.controller.dto.ProfileVo;

/**
 * 「我的主页」：修士/妖族境界 + 委托接取情况 + 修炼阵营。
 */
public interface ProfileService {

    /** 拉取主页聚合数据 */
    ProfileVo profile(Long userId);

    /** 选择修炼阵营（1 修士 / 2 妖族），返回最新主页数据 */
    ProfileVo setCamp(Long userId, Integer camp);
}
