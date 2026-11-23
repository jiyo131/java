package com.fenglin.springboottest.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.PageResult;
import com.fenglin.springboottest.entity.AuctionBid;
import com.fenglin.springboottest.entity.AuctionDeal;
import com.fenglin.springboottest.entity.AuctionLot;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.AuctionBidMapper;
import com.fenglin.springboottest.mapper.AuctionDealMapper;
import com.fenglin.springboottest.mapper.AuctionLotMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AdminGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台 · 拍卖管理（「涂山拍卖行」拍品数据的增删改查 + 成交/流拍纪录）。
 *
 * <p>拍品状态：1 上架（参与轮转拍卖）/ 0 下架（从拍卖行撤下，懒结算不再处理该拍品）。
 * 已有出价/成交纪录的拍品不允许删除（避免 star_auction_bid / star_auction_deal 悬空），可下架替代。</p>
 */
@RestController
@RequestMapping("/api/admin/auction")
public class AdminAuctionController {

    private static final int MAX_SIZE = 50;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private AuctionLotMapper lotMapper;

    @Autowired
    private AuctionBidMapper bidMapper;

    @Autowired
    private AuctionDealMapper dealMapper;

    @Autowired
    private UserMapper userMapper;

    /** 拍品列表：关键词 / 状态筛选 + 分页（含下架数据），行带领先者昵称与出价笔数 */
    @GetMapping("/lots")
    public Result<PageResult<Map<String, Object>>> lots(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "-1") int status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }

        int p = Math.max(1, page);
        int s = size <= 0 ? 10 : Math.min(size, MAX_SIZE);

        LambdaQueryWrapper<AuctionLot> qw = Wrappers.lambdaQuery(AuctionLot.class);
        if (status == 0 || status == 1) {
            qw.eq(AuctionLot::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().replaceAll("[%_\\\\]", "");
            qw.and(w -> w.like(AuctionLot::getName, kw)
                    .or().like(AuctionLot::getCode, kw)
                    .or().like(AuctionLot::getSubtitle, kw)
                    .or().like(AuctionLot::getTags, kw));
        }

        long total = lotMapper.selectCount(qw);
        qw.orderByAsc(AuctionLot::getSortNo).orderByAsc(AuctionLot::getId)
                .last("LIMIT " + s + " OFFSET " + (long) (p - 1) * s);
        List<AuctionLot> rows = lotMapper.selectList(qw);

        // 领先者昵称缓存 + 出价笔数
        Map<Long, String> leaderNames = new HashMap<>();
        for (AuctionLot lot : rows) {
            Long leaderId = lot.getLeaderUserId();
            if (leaderId != null && !leaderNames.containsKey(leaderId)) {
                User u = userMapper.selectById(leaderId);
                leaderNames.put(leaderId, u == null ? "已注销用户"
                        : (u.getNickname() == null || u.getNickname().isEmpty() ? u.getUsername() : u.getNickname()));
            }
        }

        List<Map<String, Object>> list = rows.stream().map(l -> {
            long bids = bidMapper.selectCount(
                    Wrappers.lambdaQuery(AuctionBid.class).eq(AuctionBid::getLotId, l.getId()));
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("code", l.getCode());
            m.put("name", l.getName());
            m.put("subtitle", l.getSubtitle());
            m.put("description", l.getDescription());
            m.put("glyph", l.getGlyph());
            m.put("icon", l.getIcon());
            m.put("color", l.getColor());
            m.put("basePrice", l.getBasePrice());
            m.put("step", l.getStep());
            m.put("buyoutPrice", l.getBuyoutPrice());
            m.put("durationMin", l.getDurationMin());
            m.put("tags", l.getTags());
            m.put("sortNo", l.getSortNo());
            m.put("status", l.getStatus());
            m.put("currentPrice", l.getCurrentPrice());
            m.put("leaderName", l.getLeaderUserId() == null ? null : leaderNames.get(l.getLeaderUserId()));
            m.put("endAt", l.getEndTime() == null ? null : l.getEndTime().format(TIME_FMT));
            m.put("bidCount", bids);
            m.put("createdAt", l.getCreatedAt());
            return m;
        }).toList();
        return Result.ok(PageResult.of(list, total, p, s));
    }

    /** 新增拍品（立即按场次时长开拍） */
    @PostMapping("/lots")
    public Result<Void> create(@RequestHeader(value = "Authorization", required = false) String auth,
                               @RequestBody AuctionLot lot) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        String err = validate(lot, null);
        if (err != null) {
            return Result.fail(err);
        }
        if (lotMapper.selectCount(Wrappers.lambdaQuery(AuctionLot.class)
                .eq(AuctionLot::getCode, lot.getCode().trim())) > 0) {
            return Result.fail("拍品编码已存在：" + lot.getCode().trim());
        }
        normalize(lot);
        LocalDateTime now = LocalDateTime.now();
        lot.setCreatedAt(now);
        // runtime：以底价起拍
        lot.setCurrentPrice(lot.getBasePrice());
        lot.setLeaderUserId(null);
        lot.setEndTime(now.plusMinutes(lot.getDurationMin()));
        lotMapper.insert(lot);
        return Result.ok();
    }

    /** 修改拍品（编码编辑弹窗禁改；runtime 场次不受编辑影响，下一场起生效） */
    @PutMapping("/lots/{id}")
    public Result<Void> update(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id, @RequestBody AuctionLot lot) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        AuctionLot old = id == null ? null : lotMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "拍品不存在，可能已被删除");
        }
        String err = validate(lot, id);
        if (err != null) {
            return Result.fail(err);
        }
        normalize(lot);
        lot.setId(old.getId());
        lot.setCreatedAt(null);
        // runtime 三字段不随编辑变化（当前场次继续按原节奏走）
        lot.setCurrentPrice(null);
        lot.setLeaderUserId(old.getLeaderUserId());
        lot.setEndTime(null);
        lotMapper.updateById(lot);
        return Result.ok();
    }

    /** 上架 / 下架（下架即从拍卖行撤下，当前场次冻结不再结算） */
    @PutMapping("/lots/{id}/status")
    public Result<Void> changeStatus(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable("id") Long id,
                                     @RequestBody Map<String, Object> body) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        AuctionLot old = id == null ? null : lotMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "拍品不存在，可能已被删除");
        }
        int status;
        try {
            status = Integer.parseInt(String.valueOf(body.get("status")));
        } catch (Exception ex) {
            return Result.fail("状态参数不正确");
        }
        if (status != 0 && status != 1) {
            return Result.fail("状态参数不正确");
        }
        AuctionLot patch = new AuctionLot();
        patch.setId(old.getId());
        patch.setStatus(status);
        // 重新上架时若场次已过期，重开一场（避免一上来就被懒结算成流拍留痕）
        if (status == 1 && (old.getEndTime() == null || old.getEndTime().isBefore(LocalDateTime.now()))) {
            patch.setCurrentPrice(old.getBasePrice());
            patch.setEndTime(LocalDateTime.now().plusMinutes(
                    Math.max(5, old.getDurationMin() == null ? 60 : old.getDurationMin())));
        }
        lotMapper.updateById(patch);
        return Result.ok();
    }

    /** 删除拍品（已有出价/成交纪录时拒绝，可用下架替代） */
    @DeleteMapping("/lots/{id}")
    public Result<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                               @PathVariable("id") Long id) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        AuctionLot old = id == null ? null : lotMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "拍品不存在，可能已被删除");
        }
        long bids = bidMapper.selectCount(
                Wrappers.lambdaQuery(AuctionBid.class).eq(AuctionBid::getLotId, id));
        if (bids > 0) {
            return Result.fail("该拍品已有 " + bids + " 条出价记录，不能删除；可改为「下架」隐藏");
        }
        long deals = dealMapper.selectCount(
                Wrappers.lambdaQuery(AuctionDeal.class).eq(AuctionDeal::getLotId, id));
        if (deals > 0) {
            return Result.fail("该拍品已有 " + deals + " 条成交/流拍留档，不能删除；可改为「下架」隐藏");
        }
        lotMapper.deleteById(id);
        return Result.ok();
    }

    /** 成交 / 流拍纪录：类型 / 拍品筛选 + 分页（新纪录在前） */
    @GetMapping("/records")
    public Result<PageResult<Map<String, Object>>> records(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false, defaultValue = "-1") int dealType,
            @RequestParam(required = false, defaultValue = "0") long lotId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }

        int p = Math.max(1, page);
        int s = size <= 0 ? 10 : Math.min(size, MAX_SIZE);

        LambdaQueryWrapper<AuctionDeal> qw = Wrappers.lambdaQuery(AuctionDeal.class);
        if (dealType == 1 || dealType == 2 || dealType == 3) {
            qw.eq(AuctionDeal::getDealType, dealType);
        }
        if (lotId > 0) {
            qw.eq(AuctionDeal::getLotId, lotId);
        }
        long total = dealMapper.selectCount(qw);
        qw.orderByDesc(AuctionDeal::getId)
                .last("LIMIT " + s + " OFFSET " + (long) (p - 1) * s);
        List<AuctionDeal> rows = dealMapper.selectList(qw);

        Map<Long, String> winnerNames = new HashMap<>();
        for (AuctionDeal d : rows) {
            Long winnerId = d.getWinnerUserId();
            if (winnerId != null && !winnerNames.containsKey(winnerId)) {
                User u = userMapper.selectById(winnerId);
                winnerNames.put(winnerId, u == null ? "已注销用户"
                        : (u.getNickname() == null || u.getNickname().isEmpty() ? u.getUsername() : u.getNickname()));
            }
        }

        List<Map<String, Object>> list = rows.stream().map(d -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("lotId", d.getLotId());
            m.put("name", d.getName());
            m.put("glyph", d.getGlyph());
            m.put("color", d.getColor());
            m.put("price", d.getPrice());
            m.put("dealType", d.getDealType());
            m.put("winnerName", d.getWinnerUserId() == null ? null : winnerNames.get(d.getWinnerUserId()));
            m.put("createdAt", d.getCreatedAt());
            return m;
        }).toList();
        return Result.ok(PageResult.of(list, total, p, s));
    }

    // ------------------------------------------------------------------

    /** 字段校验；duplicateId 用于更新时排除自身后查编码重复。返回 null 表示通过。 */
    private String validate(AuctionLot l, Long duplicateId) {
        if (l == null) {
            return "请求体不能为空";
        }
        String code = l.getCode() == null ? "" : l.getCode().trim();
        if (code.isEmpty() || code.length() > 48) {
            return "拍品编码必填，且不超过 48 字";
        }
        if (!code.matches("[A-Za-z0-9_-]+")) {
            return "拍品编码只能包含字母、数字、下划线或中划线";
        }
        AuctionLot same = lotMapper.selectOne(
                Wrappers.lambdaQuery(AuctionLot.class).eq(AuctionLot::getCode, code));
        if (same != null && !same.getId().equals(duplicateId)) {
            return "拍品编码已存在：" + code;
        }
        String name = l.getName() == null ? "" : l.getName().trim();
        if (name.isEmpty() || name.length() > 64) {
            return "拍品名称必填，且不超过 64 字";
        }
        if (l.getBasePrice() == null) {
            l.setBasePrice(60);
        }
        if (l.getBasePrice() < 1) {
            return "底价至少 1 情绪值";
        }
        if (l.getStep() == null) {
            l.setStep(10);
        }
        if (l.getStep() < 1) {
            return "加价幅度至少 1 情绪值";
        }
        if (l.getBuyoutPrice() == null) {
            l.setBuyoutPrice(0);
        }
        if (l.getBuyoutPrice() < 0) {
            return "一口价不能为负数（0 = 不设一口价）";
        }
        if (l.getBuyoutPrice() > 0 && l.getBuyoutPrice() < l.getBasePrice()) {
            return "一口价不能低于底价";
        }
        if (l.getDurationMin() == null) {
            l.setDurationMin(60);
        }
        if (l.getDurationMin() < 5 || l.getDurationMin() > 1440) {
            return "场次时长需在 5 - 1440 分钟之间";
        }
        if (l.getSubtitle() != null && l.getSubtitle().length() > 128) {
            return "副标题不超过 128 字";
        }
        if (l.getDescription() != null && l.getDescription().length() > 500) {
            return "拍品详情不超过 500 字";
        }
        if (l.getGlyph() != null && l.getGlyph().length() > 16) {
            return "拍品图标不超过 16 字";
        }
        if (l.getIcon() != null && l.getIcon().length() > 255) {
            return "拍品图标图片路径不超过 255 字";
        }
        if (l.getColor() != null && l.getColor().length() > 16) {
            return "拍品主色不超过 16 字";
        }
        if (l.getTags() != null && l.getTags().length() > 255) {
            return "关键词标签不超过 255 字（英文逗号分隔）";
        }
        if (l.getSortNo() != null && l.getSortNo() < 0) {
            return "排序值不能为负数";
        }
        if (l.getStatus() != null && l.getStatus() != 0 && l.getStatus() != 1) {
            return "状态只能是上架或下架";
        }
        return null;
    }

    /** 入库前修剪公共字段 */
    private void normalize(AuctionLot l) {
        l.setCode(l.getCode().trim());
        l.setName(l.getName().trim());
        if (l.getStatus() == null) {
            l.setStatus(1);
        }
        if (l.getSortNo() == null) {
            l.setSortNo(0);
        }
    }
}
