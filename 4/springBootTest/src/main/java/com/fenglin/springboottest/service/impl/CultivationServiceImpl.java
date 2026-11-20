package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.common.Categories;
import com.fenglin.springboottest.common.RealmLadder;
import com.fenglin.springboottest.controller.dto.CultivationAction;
import com.fenglin.springboottest.controller.dto.CultivationOverview;
import com.fenglin.springboottest.entity.ShopItem;
import com.fenglin.springboottest.entity.ShopRecord;
import com.fenglin.springboottest.entity.UserCultivation;
import com.fenglin.springboottest.mapper.ShopItemMapper;
import com.fenglin.springboottest.mapper.ShopRecordMapper;
import com.fenglin.springboottest.mapper.UserCultivationMapper;
import com.fenglin.springboottest.service.CultivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 修炼实现。
 *
 * 数据约定：
 *  - star_user_cultivation  累计修为（只增不减）+ 打坐状态（meditate_date 判每日一次）
 *  - star_shop_record       兑换记录，丹药/功法 used=1 未使用 / 2 已服用
 *  - star_shop_item         exp_gain 为服用/参悟可得修为（法宝为 0，不可服用）
 *
 * 境界按累计修为计算（RealmLadder），因此商店消费（扣情绪值）不影响境界。
 */
@Service
public class CultivationServiceImpl extends ServiceImpl<UserCultivationMapper, UserCultivation>
        implements CultivationService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 打坐修为：随机 5-12 */
    private static final int MEDITATE_MIN = 5;
    private static final int MEDITATE_SPAN = 8;

    @Autowired
    private ShopRecordMapper shopRecordMapper;

    @Autowired
    private ShopItemMapper shopItemMapper;

    @Override
    public CultivationOverview overview(Long userId) {
        UserCultivation cult = loadCultivation(userId);
        LocalDate today = LocalDate.now();

        CultivationOverview vo = new CultivationOverview();
        vo.setTotalCultivation(nvl(cult.getTotalCultivation()));
        vo.setMeditateCount(nvl(cult.getMeditateCount()));
        vo.setMeditatedToday(today.equals(cult.getMeditateDate()));
        vo.setUsableItems(usableItems(userId));
        vo.setServerTime(LocalDateTime.now().format(TIME_FMT));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized CultivationAction meditate(Long userId) {
        LocalDate today = LocalDate.now();
        UserCultivation cult = loadCultivation(userId);
        if (today.equals(cult.getMeditateDate())) {
            throw new IllegalStateException("今日已打坐过，心浮气躁反伤道行，明早再来");
        }

        int beforeIdx = RealmLadder.indexOf(nvl(cult.getTotalCultivation()));
        int gained = MEDITATE_MIN + ThreadLocalRandom.current().nextInt(MEDITATE_SPAN + 1);
        int total = nvl(cult.getTotalCultivation()) + gained;

        cult.setTotalCultivation(total);
        cult.setMeditateDate(today);
        cult.setMeditateCount(nvl(cult.getMeditateCount()) + 1);
        if (cult.getUpdatedAt() == null) {
            cultMapperInsert(cult);
        } else {
            updateById(cult);
        }

        CultivationAction action = new CultivationAction();
        action.setGained(gained);
        action.setTotalCultivation(total);
        applyLevelUp(action, beforeIdx, total);
        action.setMessage("打坐吐纳一个时辰，修为 +" + gained);
        return action;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized CultivationAction use(Long userId, Long recordId) {
        ShopRecord record = recordId == null ? null : shopRecordMapper.selectById(recordId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new IllegalStateException("没有这件宝贝的服用记录");
        }
        if (ShopRecord.STATUS_USED == nvl(record.getUsed())) {
            throw new IllegalStateException("这份宝贝已经服用过了");
        }

        ShopItem item = shopItemMapper.selectById(record.getItemId());
        if (item == null) {
            throw new IllegalStateException("宝贝已下架，无法服用");
        }
        if (ShopItem.CAT_FABAO.equals(item.getCategory())) {
            throw new IllegalStateException("法宝是随身收藏，不需要服用");
        }
        int exp = nvl(item.getExpGain());
        if (exp <= 0) {
            throw new IllegalStateException("这件宝贝不能服用");
        }

        int beforeIdx = RealmLadder.indexOf(nvl(loadCultivation(userId).getTotalCultivation()));

        record.setUsed(ShopRecord.STATUS_USED);
        shopRecordMapper.updateById(record);

        UserCultivation cult = loadCultivation(userId);
        int total = nvl(cult.getTotalCultivation()) + exp;
        cult.setTotalCultivation(total);
        if (cult.getUpdatedAt() == null) {
            cultMapperInsert(cult);
        } else {
            updateById(cult);
        }

        CultivationAction action = new CultivationAction();
        action.setGained(exp);
        action.setTotalCultivation(total);
        applyLevelUp(action, beforeIdx, total);
        action.setMessage(("danyao".equals(item.getCategory()) ? "已服用「" : "已参悟「")
                + item.getName() + "」，修为 +" + exp);
        return action;
    }

    /* ================= 内部辅助 ================= */

    /** 可服用背包：未使用的丹药/功法兑换记录（最近兑入的排前） */
    private List<CultivationOverview.UsableItem> usableItems(Long userId) {
        List<ShopRecord> records = shopRecordMapper.selectList(Wrappers.lambdaQuery(ShopRecord.class)
                .eq(ShopRecord::getUserId, userId)
                .eq(ShopRecord::getUsed, ShopRecord.STATUS_UNUSED)
                .orderByDesc(ShopRecord::getId));
        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, ShopItem> items = new HashMap<>(records.size() * 2);
        for (ShopItem item : shopItemMapper.selectBatchIds(
                records.stream().map(ShopRecord::getItemId).distinct().toList())) {
            items.put(item.getId(), item);
        }

        List<CultivationOverview.UsableItem> list = new ArrayList<>(records.size());
        for (ShopRecord record : records) {
            ShopItem item = items.get(record.getItemId());
            if (item == null || nvl(item.getExpGain()) <= 0
                    || ShopItem.CAT_FABAO.equals(item.getCategory())) {
                continue;
            }
            CultivationOverview.UsableItem row = new CultivationOverview.UsableItem();
            row.setRecordId(record.getId());
            row.setItemId(item.getId());
            row.setName(item.getName());
            row.setGlyph(item.getGlyph() == null || item.getGlyph().isEmpty() ? "🎁" : item.getGlyph());
            row.setIcon(item.getIcon());
            row.setColor(item.getColor() == null || item.getColor().isEmpty() ? "#ff9eb5" : item.getColor());
            row.setCategoryText(Categories.textOf(item.getCategory()));
            row.setExpGain(nvl(item.getExpGain()));
            row.setObtainedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().format(TIME_FMT));
            list.add(row);
        }
        return list;
    }

    private void applyLevelUp(CultivationAction action, int beforeIdx, int total) {
        int afterIdx = RealmLadder.indexOf(total);
        action.setLevelUp(afterIdx > beforeIdx);
        action.setRealmTitle(afterIdx > beforeIdx ? RealmLadder.CULTIVATOR_TITLES[afterIdx] : null);
    }

    private UserCultivation loadCultivation(Long userId) {
        UserCultivation cult = userId == null ? null : getById(userId);
        if (cult == null) {
            cult = new UserCultivation();
            cult.setUserId(userId);
            cult.setTotalCultivation(0);
            cult.setMeditateCount(0);
        }
        return cult;
    }

    private void cultMapperInsert(UserCultivation cult) {
        cult.setUpdatedAt(LocalDateTime.now());
        baseMapper.insert(cult);
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
