package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户情绪值：完成红线仙委托累积而来。
 */
@Data
@TableName("star_user_emotion")
public class UserEmotion {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** 当前情绪值 */
    private Integer emotion;

    /** 累计获得 */
    private Integer totalGain;

    /** 累计完成委托数 */
    private Integer completedCount;

    private LocalDateTime updatedAt;
}
