package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 修炼动作结果：打坐 / 服用后返回本次收获与最新累计修为。
 */
@Data
public class CultivationAction {

    /** 本次获得修为 */
    private Integer gained = 0;

    /** 最新累计修为 */
    private Integer totalCultivation = 0;

    /** 是否晋升了境界 */
    private Boolean levelUp = false;

    /** 晋升后的境界名（未晋升为 null） */
    private String realmTitle;

    /** 动作文案 */
    private String message;
}
