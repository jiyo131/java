package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("star_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号（唯一） */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 状态：1 正常 / 0 停用（停用后无法登录前后台） */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
