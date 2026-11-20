package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fenglin.springboottest.common.Categories;
import com.fenglin.springboottest.controller.dto.ShopAction;
import com.fenglin.springboottest.controller.dto.ShopCard;
import com.fenglin.springboottest.controller.dto.ShopOverview;
import com.fenglin.springboottest.entity.ShopItem;
import com.fenglin.springboottest.entity.ShopRecord;
import com.fenglin.springboottest.entity.UserEmotion;
import com.fenglin.springboottest.mapper.ShopItemMapper;
import com.fenglin.springboottest.mapper.ShopRecordMapper;
import com.fenglin.springboottest.mapper.UserEmotionMapper;
import com.fenglin.springboottest.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 兑换商店实现。
 *
 * 数据约定：
 *  - star_shop_item    上架商品（status=1 才展示），stock=-1 不限量 / >=0 限量
 *  - star_shop_record  兑换记录，只增不改
 *  - star_user_emotion 情绪值，完成委托累积而来，此处作为兑换通货扣减
 *
 * 兑换流程（synchronized + 事务，防并发超扣）：
 *  校验商品与库存 → 校验情绪值余额 → 扣情绪值 → 减库存 → 落兑换记录。
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopItemMapper, ShopItem> implements ShopService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 情绪值阶位（仅用于商店页展示当前称呼，与任务页一致） */
    private static final int[] LEVEL_FLOORS = {0, 60, 180, 360, 600};
    private static final String[] LEVEL_TITLES = {"初见红线", "心有灵犀", "红线牵引", "三世情缘", "苦情之证"};

    @Autowired
    private ShopRecordMapper recordMapper;

    @Autowired
    private UserEmotionMapper emotionMapper;

    @Override
    public ShopOverview overview(Long userId) {
        List<ShopItem> items = lambdaQuery()
                .eq(ShopItem::getStatus, 1)
                .orderByAsc(ShopItem::getSortNo)
                .orderByAsc(ShopItem::getId)
                .list();

        // 当前用户的兑换流水：按商品聚件数，全量聚总数 / 总花费
        List<ShopRecord> myFlow = userId == null
                ? Collections.emptyList()
                : recordMapper.selectList(Wrappers.lambdaQuery(ShopRecord.class)
                        .eq(ShopRecord::getUserId, userId));
        Map<Long, Integer> ownedPerItem = new HashMap<>(myFlow.size() * 2);
        int ownedTotal = 0;
        int spentTotal = 0;
        for (ShopRecord record : myFlow) {
            ownedPerItem.merge(record.getItemId(), 1, Integer::sum);
            ownedTotal++;
            spentTotal += record.getPrice() == null ? 0 : record.getPrice();
        }

        UserEmotion emotion = loadEmotion(userId);

        ShopOverview vo = new ShopOverview();
        vo.setEmotion(nvl(emotion.getEmotion()));
        vo.setCompletedCount(nvl(emotion.getCompletedCount()));
        vo.setLevelTitle(LEVEL_TITLES[levelIndexOf(nvl(emotion.getEmotion()))]);
        vo.setOwnedCount(ownedTotal);
        vo.setSpentTotal(spentTotal);

        List<ShopCard> cards = new ArrayList<>(items.size());
        for (ShopItem item : items) {
            cards.add(toCard(item, ownedPerItem.getOrDefault(item.getId(), 0)));
            vo.setTotalItems(vo.getTotalItems() + 1);
            if (ShopItem.CAT_FABAO.equals(item.getCategory())) {
                vo.setFabaoCount(vo.getFabaoCount() + 1);
            } else if (ShopItem.CAT_DANYAO.equals(item.getCategory())) {
                vo.setDanyaoCount(vo.getDanyaoCount() + 1);
            } else if (ShopItem.CAT_GONGFA.equals(item.getCategory())) {
                vo.setGongfaCount(vo.getGongfaCount() + 1);
            }
        }
        vo.setItems(cards);
        vo.setMyRecords(myRecords(userId, myFlow));
        vo.setServerTime(LocalDateTime.now().format(TIME_FMT));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized ShopAction redeem(Long userId, Long itemId) {
        ShopItem item = itemId == null ? null : getById(itemId);
        if (item == null || nvl(item.getStatus()) != 1) {
            throw new IllegalStateException("这件宝贝已下架或不存在");
        }
        int stock = nvl(item.getStock());
        if (stock == 0) {
            throw new IllegalStateException("「" + item.getName() + "」已兑完，下次赶早");
        }
        int price = nvl(item.getPrice());

        UserEmotion emotion = emotionMapper.selectById(userId);
        int balance = emotion == null ? 0 : nvl(emotion.getEmotion());
        if (balance < price) {
            throw new IllegalStateException("情绪值还差 " + (price - balance)
                    + "，先去苦情树多牵几桩姻缘吧");
        }

        LocalDateTime now = LocalDateTime.now();

        // 扣情绪值（只动当前值，累计获得 / 完成数不受兑换影响）
        emotion.setEmotion(balance - price);
        emotion.setUpdatedAt(now);
        emotionMapper.updateById(emotion);

        // 限量商品减库存
        if (stock >= 0) {
            item.setStock(stock - 1);
            updateById(item);
        }

        // 落兑换记录
        ShopRecord record = new ShopRecord();
        record.setUserId(userId);
        record.setItemId(item.getId());
        record.setPrice(price);
        record.setCreatedAt(now);
        recordMapper.insert(record);

        ShopAction action = new ShopAction();
        action.setItem(toCard(item, 1));
        action.setOverview(overview(userId));
        action.setSpent(price);
        action.setMessage("已兑入「" + item.getName() + "」，情绪值 -" + price);
        return action;
    }

    /* ================= 内部辅助 ================= */

    private List<ShopOverview.ShopMyRecord> myRecords(Long userId, List<ShopRecord> flow) {
        if (userId == null || flow.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ShopItem> items = new HashMap<>(lambdaQuery()
                .in(ShopItem::getId, flow.stream().map(ShopRecord::getItemId).distinct().toList())
                .list()
                .stream()
                .collect(java.util.stream.Collectors.toMap(ShopItem::getId, i -> i)));
        List<ShopOverview.ShopMyRecord> list = new ArrayList<>(flow.size());
        // 倒序：最近兑的排前面
        for (int i = flow.size() - 1; i >= 0; i--) {
            ShopRecord record = flow.get(i);
            ShopItem item = items.get(record.getItemId());
            ShopOverview.ShopMyRecord row = new ShopOverview.ShopMyRecord();
            row.setId(record.getId());
            row.setItemId(record.getItemId());
            row.setPrice(nvl(record.getPrice()));
            row.setCreatedAt(format(record.getCreatedAt()));
            if (item != null) {
                row.setName(item.getName());
                row.setGlyph(item.getGlyph());
                row.setIcon(item.getIcon());
                row.setColor(item.getColor());
                row.setCategoryText(Categories.textOf(item.getCategory()));
            } else {
                row.setName("已下架的宝贝");
            }
            list.add(row);
        }
        return list;
    }

    private ShopCard toCard(ShopItem item, int ownedCount) {
        ShopCard card = new ShopCard();
        card.setId(item.getId());
        card.setCode(item.getCode());
        card.setCategory(item.getCategory());
        card.setCategoryText(Categories.textOf(item.getCategory()));
        card.setName(item.getName());
        card.setSubtitle(item.getSubtitle());
        card.setDescription(item.getDescription());
        card.setGlyph(item.getGlyph() == null || item.getGlyph().isEmpty() ? "🎁" : item.getGlyph());
        card.setIcon(item.getIcon());
        card.setColor(item.getColor() == null || item.getColor().isEmpty() ? "#ff9eb5" : item.getColor());
        card.setPrice(nvl(item.getPrice()));
        card.setExpGain(nvl(item.getExpGain()));
        card.setStock(nvl(item.getStock()));
        card.setAvailable(nvl(item.getStatus()) == 1 && nvl(item.getStock()) != 0);
        card.setTags(splitTags(item.getTags()));
        card.setOwnedCount(ownedCount);
        return card;
    }

    private UserEmotion loadEmotion(Long userId) {
        UserEmotion emotion = userId == null ? null : emotionMapper.selectById(userId);
        if (emotion == null) {
            emotion = new UserEmotion();
            emotion.setUserId(userId);
            emotion.setEmotion(0);
            emotion.setTotalGain(0);
            emotion.setCompletedCount(0);
        }
        return emotion;
    }

    private static int levelIndexOf(int emotion) {
        int index = 0;
        for (int i = 0; i < LEVEL_FLOORS.length; i++) {
            if (emotion >= LEVEL_FLOORS[i]) {
                index = i;
            }
        }
        return index;
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(list::add);
        return list;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FMT);
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
