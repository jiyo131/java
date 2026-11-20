package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 涂山小游戏战绩：目前仅打地鼠（game_key='whack'）。
 * 每局结束结算一次情绪值，同时用于「每日限次」统计。
 */
@Data
@TableName("star_game_record")
public class GameRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 游戏标识：whack 打地鼠 */
    private String gameKey;

    /** 本局得分（服务端重算，客户端上报值仅作参考） */
    private Integer score;

    /** 命中次数 */
    private Integer hits;

    /** 金狐命中次数 */
    private Integer goldenHits;

    /** 本局时长（毫秒） */
    private Integer durationMs;

    /** 实得情绪值 */
    private Integer reward;

    private LocalDateTime createdAt;
}
