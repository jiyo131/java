package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户修为：境界（修士/妖族两线）由累计修为驱动，与情绪值（商店货币）分离。
 */
@Data
@TableName("star_user_cultivation")
public class UserCultivation {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** 累计修为（境界依据，只增不减） */
    private Integer totalCultivation;

    /** 最近一次打坐日期（判每日一次） */
    private LocalDate meditateDate;

    /** 累计打坐次数 */
    private Integer meditateCount;

    private LocalDateTime updatedAt;
}
