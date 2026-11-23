package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.controller.dto.AuctionAction;
import com.fenglin.springboottest.controller.dto.AuctionOverview;
import com.fenglin.springboottest.entity.AuctionBid;
import com.fenglin.springboottest.entity.AuctionDeal;
import com.fenglin.springboottest.entity.AuctionLot;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.entity.UserEmotion;
import com.fenglin.springboottest.mapper.AuctionBidMapper;
import com.fenglin.springboottest.mapper.AuctionDealMapper;
import com.fenglin.springboottest.mapper.AuctionLotMapper;
import com.fenglin.springboottest.mapper.UserEmotionMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 涂山拍卖行实现（2026-11-23，独立页面 /auction）。
 *
 * 数据约定：
 *  - star_auction_lot    拍品模板 + 当前场次状态（current_price / leader / end_time）
 *  - star_auction_bid    出价流水，只增不改
 *  - star_auction_deal   成交 / 流拍留档，只增不改（1 竞价成交 / 2 一口价 / 3 流拍）
 *
 * 计价与结算规则：
 *  - 出价即扣情绪值，被超越时全额退款（领先者始终「押着」自己的最高出价）；
 *  - 首价 ≥ 底价；此后每次 ≥ 当前价 + 加价幅度；
 *  - 结拍前 60 秒内出价，结拍时间自动顺延 60 秒（防狙击，雅雅的规矩）；
 *  - 结拍懒结算：有领先者 → 竞价成交；无人出价 → 流拍；随后自动开下一场（轮转，永不断档）；
 *  - 一口价：立即成交（领先者先退款）→ 落 deal → 立刻开下一场。
 */
@Service
public class AuctionServiceImpl implements AuctionService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 结拍时间若早于服务启动时钟（脏数据 / 长期停机），顺延多久再开拍 */
    private static final int RELIST_MIN_MINUTES = 30;

    /** 成交/流拍纪录展示条数 */
    private static final int DEAL_LIMIT = 20;

    /** 单件拍品出价记录展示条数 */
    private static final int BID_LIMIT = 30;

    @Autowired
    private AuctionLotMapper lotMapper;

    @Autowired
    private AuctionBidMapper bidMapper;

    @Autowired
    private AuctionDealMapper dealMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserEmotionMapper emotionMapper;

    /* ================= 查询 ================= */

    @Override
    public AuctionOverview overview(Long userId) {
        synchronized (this) {
            settleExpiredLots(LocalDateTime.now());
        }
        return buildOverview(userId);
    }

    @Override
    public List<AuctionOverview.BidCard> bids(Long userId, Long lotId) {
        if (lotId == null) {
            return Collections.emptyList();
        }
        List<AuctionBid> rows = bidMapper.selectList(Wrappers.lambdaQuery(AuctionBid.class)
                .eq(AuctionBid::getLotId, lotId)
                .orderByDesc(AuctionBid::getId)
                .last("LIMIT " + BID_LIMIT));
        Map<Long, User> users = loadUsers(
                rows.stream().map(AuctionBid::getUserId).distinct().toList());
        List<AuctionOverview.BidCard> list = new ArrayList<>(rows.size());
        for (AuctionBid row : rows) {
            AuctionOverview.BidCard card = new AuctionOverview.BidCard();
            card.setId(row.getId());
            card.setAmount(row.getAmount());
            card.setMine(userId != null && userId.equals(row.getUserId()));
            User u = users.get(row.getUserId());
            card.setUsername(u == null ? "已注销用户"
                    : (u.getNickname() == null || u.getNickname().isEmpty() ? u.getUsername() : u.getNickname()));
            card.setCreatedAt(row.getCreatedAt() == null ? null : row.getCreatedAt().format(TIME_FMT));
            list.add(card);
        }
        return list;
    }

    /* ================= 动作 ================= */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized AuctionAction bid(Long userId, Long lotId, Integer amount) {
        LocalDateTime now = LocalDateTime.now();
        settleExpiredLots(now);

        AuctionLot lot = lotId == null ? null : lotMapper.selectById(lotId);
        if (lot == null || lot.getStatus() == null || lot.getStatus() != AuctionLot.ST_ONLINE) {
            throw new IllegalStateException("这件拍品不存在或已下架");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalStateException("出价得是个正数情绪值");
        }
        if (lot.getEndTime() == null || !lot.getEndTime().isAfter(now)) {
            // 已到点但还没被懒结算 → 先结算再按新场次处理
            settleExpiredLots(now);
            lot = lotMapper.selectById(lotId);
            if (lot == null || lot.getEndTime() == null || !lot.getEndTime().isAfter(now)) {
                throw new IllegalStateException("这一场刚刚敲槌了，稍候片刻拍下一场");
            }
        }

        int base = nvl(lot.getBasePrice());
        int step = Math.max(1, nvl(lot.getStep()));
        int current = nvl(lot.getCurrentPrice());
        Long leaderId = lot.getLeaderUserId();
        int minBid = leaderId == null ? base : current + step;
        if (amount < minBid) {
            throw new IllegalStateException("至少出 " + minBid + " 才能压过当前价（加价幅度 " + step + "）");
        }

        // 我已是领先者 → 本次只需补差价（原价会在扣款前留在场上）
        int required = userId.equals(leaderId) ? amount - current : amount;
        UserEmotion myEmotion = loadEmotion(userId);
        if (nvl(myEmotion.getEmotion()) < required) {
            throw new IllegalStateException("情绪值不够：还差 " + (required - nvl(myEmotion.getEmotion()))
                    + "，先去苦情树多牵几桩姻缘吧");
        }

        // 扣我的钱 / 退旧领先者的钱
        myEmotion.setEmotion(nvl(myEmotion.getEmotion()) - required);
        myEmotion.setUpdatedAt(now);
        emotionMapper.updateById(myEmotion);
        if (leaderId != null && !leaderId.equals(userId)) {
            refund(leaderId, current);
        }

        // 防狙击：结拍前 60 秒内出价，顺延 60 秒
        boolean extended = false;
        long remainSec = Duration.between(now, lot.getEndTime()).getSeconds();
        LocalDateTime newEnd = lot.getEndTime();
        if (remainSec < AuctionLot.ANTI_SNIPE_SECONDS) {
            newEnd = now.plusSeconds(AuctionLot.ANTI_SNIPE_SECONDS);
            extended = true;
        }

        lot.setCurrentPrice(amount);
        lot.setLeaderUserId(userId);
        lot.setEndTime(newEnd);
        lotMapper.updateById(lot);

        AuctionBid bidRow = new AuctionBid();
        bidRow.setLotId(lot.getId());
        bidRow.setUserId(userId);
        bidRow.setAmount(amount);
        bidRow.setCreatedAt(now);
        bidMapper.insert(bidRow);

        AuctionAction action = new AuctionAction();
        String name = lot.getName();
        if (leaderId != null && leaderId.equals(userId) && !extended) {
            action.setMessage("你继续领先「" + name + "」，当前 " + amount + " 情绪值");
        } else if (leaderId != null && leaderId.equals(userId)) {
            action.setMessage("加价成功，你仍领先「" + name + "」，当前 " + amount + " 情绪值");
        } else {
            action.setMessage("出价成功，你暂时领先「" + name + "」！" + (extended ? "（最后关头出价，结拍钟顺延 60 秒）" : ""));
        }
        action.setLot(toLotCard(lot, userId));
        action.setOverview(buildOverview(userId));
        return action;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized AuctionAction buyout(Long userId, Long lotId) {
        LocalDateTime now = LocalDateTime.now();
        settleExpiredLots(now);

        AuctionLot lot = lotId == null ? null : lotMapper.selectById(lotId);
        if (lot == null || lot.getStatus() == null || lot.getStatus() != AuctionLot.ST_ONLINE) {
            throw new IllegalStateException("这件拍品不存在或已下架");
        }
        int buyout = nvl(lot.getBuyoutPrice());
        if (buyout <= 0) {
            throw new IllegalStateException("「" + lot.getName() + "」没有设一口价，只能慢慢抬价");
        }
        if (lot.getEndTime() == null || !lot.getEndTime().isAfter(now)) {
            settleExpiredLots(now);
            lot = lotMapper.selectById(lotId);
            if (lot == null || lot.getEndTime() == null || !lot.getEndTime().isAfter(now)) {
                throw new IllegalStateException("这一场刚刚敲槌了，稍候片刻拍下一场");
            }
        }

        Long leaderId = lot.getLeaderUserId();
        int current = nvl(lot.getCurrentPrice());
        int required = userId.equals(leaderId) ? buyout - current : buyout;
        UserEmotion myEmotion = loadEmotion(userId);
        if (nvl(myEmotion.getEmotion()) < required) {
            throw new IllegalStateException("一口价要 " + buyout + " 情绪值，你还差 "
                    + (required - nvl(myEmotion.getEmotion())));
        }

        myEmotion.setEmotion(nvl(myEmotion.getEmotion()) - required);
        myEmotion.setUpdatedAt(now);
        emotionMapper.updateById(myEmotion);
        if (leaderId != null && !leaderId.equals(userId)) {
            refund(leaderId, current);
        }

        // 成交留档 + 立即开下一场
        recordDeal(lot, buyout, AuctionDeal.TYPE_BUYOUT, userId, now);
        relist(lot, now);

        AuctionAction action = new AuctionAction();
        action.setMessage("一口价拿下「" + lot.getName() + "」！花费 " + buyout + " 情绪值，快去「我的夺宝」看看");
        action.setLot(toLotCard(lotMapper.selectById(lotId), userId));
        action.setOverview(buildOverview(userId));
        return action;
    }

    /* ================= 结算与轮转 ================= */

    /** 结拍到点的场次：有领先者 → 竞价成交；无人出价 → 流拍；随后自动开下一场 */
    private void settleExpiredLots(LocalDateTime now) {
        List<AuctionLot> expired = lotMapper.selectList(Wrappers.lambdaQuery(AuctionLot.class)
                .eq(AuctionLot::getStatus, AuctionLot.ST_ONLINE)
                .isNotNull(AuctionLot::getEndTime)
                .le(AuctionLot::getEndTime, now));
        for (AuctionLot lot : expired) {
            if (lot.getLeaderUserId() != null) {
                recordDeal(lot, nvl(lot.getCurrentPrice()), AuctionDeal.TYPE_BID, lot.getLeaderUserId(), now);
            } else {
                recordDeal(lot, 0, AuctionDeal.TYPE_PASSED, null, now);
            }
            relist(lot, now);
        }
    }

    /** 落成交/流拍留档（快照拍品名/图标/主色，轮转后 lot 行会被复用） */
    private void recordDeal(AuctionLot lot, int price, int type, Long winnerId, LocalDateTime now) {
        AuctionDeal deal = new AuctionDeal();
        deal.setLotId(lot.getId());
        deal.setName(lot.getName());
        deal.setGlyph(lot.getGlyph());
        deal.setColor(lot.getColor());
        deal.setPrice(price);
        deal.setDealType(type);
        deal.setWinnerUserId(winnerId);
        deal.setCreatedAt(now);
        dealMapper.insert(deal);
    }

    /** 开下一场：当前价重置为底价、清空领先者、按场次时长顺延结拍时间 */
    private void relist(AuctionLot lot, LocalDateTime now) {
        int duration = Math.max(RELIST_MIN_MINUTES, nvl(lot.getDurationMin()));
        // 注意：updateById 会跳过 null 字段，清空领先者必须用 UpdateWrapper 显式 set null
        lotMapper.update(null, Wrappers.lambdaUpdate(AuctionLot.class)
                .eq(AuctionLot::getId, lot.getId())
                .set(AuctionLot::getCurrentPrice, nvl(lot.getBasePrice()))
                .set(AuctionLot::getLeaderUserId, null)
                .set(AuctionLot::getEndTime, now.plusMinutes(duration)));
    }

    /* ================= 面板组装 ================= */

    private AuctionOverview buildOverview(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        AuctionOverview vo = new AuctionOverview();

        List<AuctionLot> lots = lotMapper.selectList(Wrappers.lambdaQuery(AuctionLot.class)
                .eq(AuctionLot::getStatus, AuctionLot.ST_ONLINE)
                .orderByAsc(AuctionLot::getSortNo)
                .orderByAsc(AuctionLot::getId));

        // 出价笔数（按拍品聚合）+ 我领先的场次
        Map<Long, Integer> bidCountPerLot = new HashMap<>();
        int leadingCount = 0;
        int myBidCount = 0;
        List<AuctionBid> allBids = bidMapper.selectList(null);
        for (AuctionBid bid : allBids) {
            bidCountPerLot.merge(bid.getLotId(), 1, Integer::sum);
            if (userId != null && userId.equals(bid.getUserId())) {
                myBidCount++;
            }
        }

        Map<Long, User> userCache = new HashMap<>();
        List<AuctionOverview.LotCard> lotCards = new ArrayList<>(lots.size());
        for (AuctionLot lot : lots) {
            AuctionOverview.LotCard card = toLotCard(lot, userId);
            card.setBidCount(bidCountPerLot.getOrDefault(lot.getId(), 0));
            long remain = lot.getEndTime() == null ? 0
                    : Math.max(0, Duration.between(now, lot.getEndTime()).getSeconds());
            card.setRemainingSeconds(remain);
            lotCards.add(card);
            if (userId != null && userId.equals(lot.getLeaderUserId())) {
                leadingCount++;
            }
            if (lot.getLeaderUserId() != null && !userCache.containsKey(lot.getLeaderUserId())) {
                userCache.put(lot.getLeaderUserId(), userMapper.selectById(lot.getLeaderUserId()));
            }
        }
        // 补领先者昵称（缓存内查）
        for (AuctionOverview.LotCard card : lotCards) {
            if (card.getLeaderId() != null) {
                User u = userCache.get(card.getLeaderId());
                card.setLeaderName(u == null ? "神秘来客"
                        : (u.getNickname() == null || u.getNickname().isEmpty() ? u.getUsername() : u.getNickname()));
            }
        }
        vo.setLots(lotCards);

        // 成交/流拍纪录（倒序，最近 20 条）
        List<AuctionDeal> deals = dealMapper.selectList(Wrappers.lambdaQuery(AuctionDeal.class)
                .orderByDesc(AuctionDeal::getId)
                .last("LIMIT " + DEAL_LIMIT));
        Map<Long, User> dealUsers = loadUsers(deals.stream()
                .map(AuctionDeal::getWinnerUserId).filter(java.util.Objects::nonNull).distinct().toList());
        List<AuctionOverview.DealCard> dealCards = new ArrayList<>(deals.size());
        int wonCount = 0;
        List<AuctionOverview.DealCard> myWon = new ArrayList<>();
        for (AuctionDeal deal : deals) {
            AuctionOverview.DealCard card = new AuctionOverview.DealCard();
            card.setId(deal.getId());
            card.setLotId(deal.getLotId());
            card.setName(deal.getName());
            card.setGlyph(deal.getGlyph());
            card.setColor(deal.getColor());
            card.setPrice(deal.getPrice());
            card.setDealType(deal.getDealType());
            card.setDealText(dealText(deal.getDealType()));
            card.setMine(userId != null && userId.equals(deal.getWinnerUserId()));
            if (deal.getWinnerUserId() != null) {
                User u = dealUsers.get(deal.getWinnerUserId());
                card.setWinnerName(u == null ? "神秘来客"
                        : (u.getNickname() == null || u.getNickname().isEmpty() ? u.getUsername() : u.getNickname()));
            }
            card.setCreatedAt(deal.getCreatedAt() == null ? null : deal.getCreatedAt().format(TIME_FMT));
            dealCards.add(card);
            if (Boolean.TRUE.equals(card.getMine())) {
                wonCount++;
                myWon.add(card);
            }
        }
        vo.setDeals(dealCards);
        vo.setMyWon(myWon);

        // 统计
        UserEmotion emotion = loadEmotion(userId);
        vo.setEmotion(nvl(emotion.getEmotion()));
        vo.setLeadingCount(leadingCount);
        vo.setWonCount(wonCount);
        vo.setBidCount(myBidCount);
        vo.setServerTime(now.format(TIME_FMT));
        return vo;
    }

    private AuctionOverview.LotCard toLotCard(AuctionLot lot, Long userId) {
        AuctionOverview.LotCard card = new AuctionOverview.LotCard();
        card.setId(lot.getId());
        card.setCode(lot.getCode());
        card.setName(lot.getName());
        card.setSubtitle(lot.getSubtitle());
        card.setDescription(lot.getDescription());
        card.setGlyph(lot.getGlyph() == null || lot.getGlyph().isEmpty() ? "🔨" : lot.getGlyph());
        card.setIcon(lot.getIcon());
        card.setColor(lot.getColor() == null || lot.getColor().isEmpty() ? "#ff9eb5" : lot.getColor());
        card.setBasePrice(nvl(lot.getBasePrice()));
        card.setStep(Math.max(1, nvl(lot.getStep())));
        card.setBuyoutPrice(Math.max(0, nvl(lot.getBuyoutPrice())));
        int current = nvl(lot.getCurrentPrice());
        card.setCurrentPrice(current);
        card.setLeaderId(lot.getLeaderUserId());
        card.setMine(userId != null && userId.equals(lot.getLeaderUserId()));
        card.setMinBid(card.getLeaderId() == null ? card.getBasePrice() : current + card.getStep());
        card.setEndAt(lot.getEndTime() == null ? null : lot.getEndTime().format(TIME_FMT));
        return card;
    }

    /* ================= 工具 ================= */

    private void refund(Long userId, int amount) {
        if (amount <= 0) {
            return;
        }
        UserEmotion emotion = emotionMapper.selectById(userId);
        if (emotion == null) {
            return; // 用户被删等极端情况：退款无从落笔，不阻断主流程
        }
        emotion.setEmotion(nvl(emotion.getEmotion()) + amount);
        emotion.setUpdatedAt(LocalDateTime.now());
        emotionMapper.updateById(emotion);
    }

    private UserEmotion loadEmotion(Long userId) {
        UserEmotion emotion = emotionMapper.selectById(userId);
        if (emotion == null) {
            emotion = new UserEmotion();
            emotion.setUserId(userId);
            emotion.setEmotion(0);
            emotion.setTotalGain(0);
            emotion.setCompletedCount(0);
        }
        return emotion;
    }

    private Map<Long, User> loadUsers(List<Long> ids) {
        Map<Long, User> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        for (User u : userMapper.selectList(Wrappers.lambdaQuery(User.class).in(User::getId, ids))) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private static String dealText(Integer type) {
        if (type == null) {
            return "拍卖纪录";
        }
        return switch (type) {
            case AuctionDeal.TYPE_BID -> "竞价成交";
            case AuctionDeal.TYPE_BUYOUT -> "一口价";
            default -> "流拍";
        };
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
