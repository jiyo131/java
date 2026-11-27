package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 红线仙任务「圆满榜」：按累计完成委托数排名（同数比累计情绪值）。
 */
@Data
public class TaskLeaderboard {

    private List<RankRow> rows;

    /** 榜单覆盖人数（完成过委托的用户总数） */
    private Integer playerCount = 0;

    /** 当前用户名次（未上榜时为 null） */
    private Integer myRank;

    /** 当前用户累计完成委托数 */
    private Integer myCompletedCount = 0;

    private String serverTime;

    @Data
    public static class RankRow {

        /** 名次，从 1 开始 */
        private Integer rank;

        private Long userId;

        private String nickname;

        /** 累计完成委托数 */
        private Integer completedCount = 0;

        /** 当前情绪值 */
        private Integer emotion = 0;

        /** 情绪阶位：初见红线 / 心有灵犀 / 红线牵引 / 三世情缘 / 苦情之证 */
        private String levelTitle;

        /** 是否当前登录用户 */
        private Boolean me = false;
    }
}
