package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请：同一方向只有一条记录（uk_req from_user_id + to_user_id）。
 * status：1 待处理 / 2 已同意 / 3 已拒绝；撤回 = 删除记录；被拒后可再次发起（复用原记录改回待处理）。
 */
@Data
@TableName("star_friend_request")
public class FriendRequest {

    public static final int ST_PENDING = 1;
    public static final int ST_ACCEPTED = 2;
    public static final int ST_REJECTED = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起人 id */
    private Long fromUserId;

    /** 接收人 id */
    private Long toUserId;

    /** 申请留言 */
    private String message;

    /** 1 待处理 / 2 已同意 / 3 已拒绝 */
    private Integer status;

    /** 发起时间 */
    private LocalDateTime createdAt;

    /** 处理时间（同意/拒绝） */
    private LocalDateTime handledAt;
}
