package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 「我的主页」数据：用户信息 + 修士/妖族两条境界线 + 委托接取情况。
 */
@Data
public class ProfileVo {

    /** 账号 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 修炼阵营：1 修士 / 2 妖族 */
    private Integer camp = 1;

    /** 阵营文案：修士 / 妖族 */
    private String campText;

    /** 当前情绪值 */
    private Integer emotion = 0;

    /** 累计获得情绪值 */
    private Integer totalGain = 0;

    /** 累计完成委托数 */
    private Integer completedCount = 0;

    /** 进行中委托数 */
    private Integer acceptedCount = 0;

    /** 修士境界线（情绪值驱动，5 阶） */
    private List<RealmNode> cultivatorRealms;

    /** 妖族境界线（情绪值驱动，5 阶） */
    private List<RealmNode> demonRealms;

    /** 进行中的委托 */
    private List<TaskCard> acceptedTasks;

    /** 已圆满的委托 */
    private List<TaskCard> completedTasks;

    private String serverTime;

    /**
     * 境界阶梯节点：情绪值达到 floor 即踏入该境，nextThreshold 为下一境门槛。
     */
    @Data
    public static class RealmNode {

        /** 阶位序号，从 1 开始 */
        private Integer index;

        /** 境界名称 */
        private String title;

        /** 踏入该境所需情绪值 */
        private Integer floor;

        /** 下一境门槛（最高境为 null） */
        private Integer nextThreshold;

        /** 是否当前所处境界 */
        private Boolean current;

        /** 是否已达成 */
        private Boolean reached;

        /** 本境进度百分比 0-100（最高境恒为 100） */
        private Integer progress;
    }
}
