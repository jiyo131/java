package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 打地鼠结算请求体（客户端上报，服务端全部重算校验）。
 */
@Data
public class GameWhackSettle {

    /** 命中次数 */
    private Integer hits;

    /** 金狐命中次数（每只金狐得分更高） */
    private Integer goldenHits;

    /** 失手次数（仅记录，不参与结算） */
    private Integer misses;

    /** 本局时长（毫秒），服务端用于合理性校验 */
    private Long durationMs;
}
