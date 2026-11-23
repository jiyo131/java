package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系（双向两行：A-B 好友时同时存 (A,B) 与 (B,A)），
 * 查「我的好友」只扫 user_id，无需 OR 查询。
 */
@Data
@TableName("star_friend")
public class Friend {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 id */
    private Long userId;

    /** 好友用户 id */
    private Long friendId;

    /** 结缘时间 */
    private LocalDateTime createdAt;
}
