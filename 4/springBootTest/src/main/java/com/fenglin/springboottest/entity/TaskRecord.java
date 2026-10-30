package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 红线仙任务接取记录：一个用户对同一任务只有一条记录。
 * status = 1 进行中 / 2 已圆满；「取消接取」会直接删除记录，因此可再次接取。
 */
@Data
@TableName("star_task_record")
public class TaskRecord {

    public static final int STATUS_ACCEPTED = 1;
    public static final int STATUS_COMPLETED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long taskId;

    private Integer status;

    private LocalDateTime acceptedAt;

    private LocalDateTime completedAt;
}
