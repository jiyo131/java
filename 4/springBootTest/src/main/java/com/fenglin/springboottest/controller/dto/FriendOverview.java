package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 「我的主页 · 红线挚友」数据：好友列表 + 收到/发出的申请 + 搜索结果。
 */
@Data
public class FriendOverview {

    /** 好友列表（按结缘时间倒序） */
    private List<FriendCard> friends;

    /** 收到的待处理申请（我作为接收人） */
    private List<RequestCard> incoming;

    /** 我发出的待处理申请 */
    private List<RequestCard> outgoing;

    /** 好友总数 */
    private Integer friendCount = 0;

    private String serverTime;

    /**
     * 好友 / 搜索结果通用用户卡。
     * 搜索结果额外带 relation：none 可申请 / friend 已是好友 / outgoing 已发出申请 / incoming 对方向我发过申请。
     */
    @Data
    public static class FriendCard {

        private Long userId;

        private String username;

        private String nickname;

        /** 修炼阵营：1 修士 / 2 妖族 */
        private Integer camp;

        private String campText;

        /** 当前情绪值（商店货币） */
        private Integer emotion = 0;

        /** 累计修为（境界依据） */
        private Integer cultivation = 0;

        /** 按阵营换算的当前境界名 */
        private String realmTitle;

        /** 结缘时间（仅好友列表有） */
        private String since;

        /** 搜索结果专用关系标记，好友列表恒为 null */
        private String relation;
    }

    /**
     * 好友申请卡（收到的 incoming / 发出的 outgoing 共用）。
     */
    @Data
    public static class RequestCard {

        private Long id;

        /** 对方用户 id（incoming = 发起人；outgoing = 接收人） */
        private Long userId;

        private String username;

        private String nickname;

        private Integer camp;

        private String campText;

        private Integer cultivation = 0;

        private String realmTitle;

        /** 申请留言 */
        private String message;

        /** 发起时间 */
        private String createdAt;
    }
}
