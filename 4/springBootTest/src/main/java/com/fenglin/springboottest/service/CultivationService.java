package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fenglin.springboottest.controller.dto.CultivationAction;
import com.fenglin.springboottest.controller.dto.CultivationOverview;
import com.fenglin.springboottest.entity.UserCultivation;

/**
 * 修炼：修为管境界，情绪值管钱包。
 *
 * 修为来源（境界按累计修为计算，只增不减）：
 *  - 每日打坐：我的主页一键，每天一次，得少量修为；
 *  - 服用丹药 / 参悟功法：兑换商店兑入后，在主页消耗掉，一次性得大量修为。
 */
public interface CultivationService extends IService<UserCultivation> {

    /** 修炼面板：累计修为 + 今日打坐状态 + 可服用背包 */
    CultivationOverview overview(Long userId);

    /** 每日打坐（每自然日一次，随机 5-12 修为） */
    CultivationAction meditate(Long userId);

    /** 服用丹药 / 参悟功法：消耗一条未使用兑换记录，转为修为 */
    CultivationAction use(Long userId, Long recordId);
}
