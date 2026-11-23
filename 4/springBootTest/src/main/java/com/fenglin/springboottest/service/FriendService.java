package com.fenglin.springboottest.service;

import com.fenglin.springboottest.controller.dto.FriendOverview;

import java.util.List;

/**
 * 「我的主页 · 红线挚友」服务：搜索用户 / 发申请 / 处理申请 / 删好友。
 */
public interface FriendService {

    /** 好友面板：好友列表 + 收到/发出的待处理申请 */
    FriendOverview overview(Long userId);

    /** 按账号或昵称搜索用户（排除自己，最多 10 条，带关系标记） */
    List<FriendOverview.FriendCard> search(Long userId, String keyword);

    /** 发送好友申请（对已是好友 / 已有待处理申请的用户拒绝） */
    FriendOverview sendRequest(Long userId, Long targetUserId, String message);

    /** 同意收到的申请（落双向好友两行） */
    FriendOverview accept(Long userId, Long requestId);

    /** 拒绝收到的申请（status 置 3，可再次发起） */
    FriendOverview reject(Long userId, Long requestId);

    /** 撤回自己发出的待处理申请（删除记录） */
    FriendOverview cancel(Long userId, Long requestId);

    /** 删除好友（双向两行一起删） */
    FriendOverview removeFriend(Long userId, Long friendUserId);
}
