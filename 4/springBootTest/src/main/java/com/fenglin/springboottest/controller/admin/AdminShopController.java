package com.fenglin.springboottest.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.PageResult;
import com.fenglin.springboottest.entity.ShopItem;
import com.fenglin.springboottest.entity.ShopRecord;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.ShopItemMapper;
import com.fenglin.springboottest.mapper.ShopRecordMapper;
import com.fenglin.springboottest.service.AdminGuard;
import com.fenglin.springboottest.service.UserService;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台 · 商店管理（首页「兑换商店」数据的增删改查 + 兑换记录查询）。
 *
 * <p>商品分类：fabao 法宝 / danyao 丹药 / gongfa 功法；status：1 上架 / 0 下架；
 * stock=-1 不限量。已有兑换记录的商品不允许删除（避免 star_shop_record 悬空），可下架替代。</p>
 */
@RestController
@RequestMapping("/api/admin/shop")
public class AdminShopController {

    private static final int MAX_SIZE = 50;
    private static final Set<String> CATEGORIES = Set.of(
            ShopItem.CAT_FABAO, ShopItem.CAT_DANYAO, ShopItem.CAT_GONGFA);

    @Autowired
    private AdminGuard adminGuard;

    @Autowired
    private ShopItemMapper itemMapper;

    @Autowired
    private ShopRecordMapper recordMapper;

    @Autowired
    private UserService userService;

    /* ==================== 商品管理 ==================== */

    /** 商品列表：关键词 / 分类 / 状态筛选 + 分页（含下架数据） */
    @GetMapping("/items")
    public Result<PageResult<Map<String, Object>>> itemList(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "-1") int status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }

        int p = Math.max(1, page);
        int s = size <= 0 ? 10 : Math.min(size, MAX_SIZE);

        LambdaQueryWrapper<ShopItem> qw = Wrappers.lambdaQuery(ShopItem.class);
        if (category != null && CATEGORIES.contains(category)) {
            qw.eq(ShopItem::getCategory, category);
        }
        if (status == 0 || status == 1) {
            qw.eq(ShopItem::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().replaceAll("[%_\\\\]", "");
            qw.and(w -> w.like(ShopItem::getName, kw)
                    .or().like(ShopItem::getCode, kw)
                    .or().like(ShopItem::getSubtitle, kw)
                    .or().like(ShopItem::getTags, kw));
        }

        long total = itemMapper.selectCount(qw);
        qw.orderByAsc(ShopItem::getSortNo).orderByAsc(ShopItem::getId)
                .last("LIMIT " + s + " OFFSET " + (long) (p - 1) * s);
        List<ShopItem> rows = itemMapper.selectList(qw);

        List<Map<String, Object>> list = rows.stream().map(this::itemToMap).toList();
        return Result.ok(PageResult.of(list, total, p, s));
    }

    /** 新增商品 */
    @PostMapping("/items")
    public Result<Void> createItem(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @RequestBody ShopItem item) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        String err = validateItem(item, null);
        if (err != null) {
            return Result.fail(err);
        }
        if (itemMapper.selectCount(Wrappers.lambdaQuery(ShopItem.class)
                .eq(ShopItem::getCode, item.getCode().trim())) > 0) {
            return Result.fail("商品编码已存在：" + item.getCode().trim());
        }
        normalizeItem(item);
        item.setCreatedAt(LocalDateTime.now());
        itemMapper.insert(item);
        return Result.ok();
    }

    /** 修改商品 */
    @PutMapping("/items/{id}")
    public Result<Void> updateItem(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @PathVariable("id") Long id, @RequestBody ShopItem item) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        ShopItem old = id == null ? null : itemMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "商品不存在，可能已被删除");
        }
        String err = validateItem(item, id);
        if (err != null) {
            return Result.fail(err);
        }
        normalizeItem(item);
        item.setId(old.getId());
        item.setCreatedAt(null); // 不覆盖创建时间
        itemMapper.updateById(item);
        return Result.ok();
    }

    /** 上架 / 下架 */
    @PutMapping("/items/{id}/status")
    public Result<Void> changeItemStatus(@RequestHeader(value = "Authorization", required = false) String auth,
                                         @PathVariable("id") Long id,
                                         @RequestBody Map<String, Object> body) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        ShopItem old = id == null ? null : itemMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "商品不存在，可能已被删除");
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
        ShopItem patch = new ShopItem();
        patch.setId(old.getId());
        patch.setStatus(status);
        itemMapper.updateById(patch);
        return Result.ok();
    }

    /** 删除商品（已有兑换记录时拒绝，可用下架替代） */
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @PathVariable("id") Long id) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }
        ShopItem old = id == null ? null : itemMapper.selectById(id);
        if (old == null) {
            return Result.fail(404, "商品不存在，可能已被删除");
        }
        long records = recordMapper.selectCount(
                Wrappers.lambdaQuery(ShopRecord.class).eq(ShopRecord::getItemId, id));
        if (records > 0) {
            return Result.fail("该商品已有 " + records + " 条兑换记录，不能删除；可改为「下架」隐藏");
        }
        itemMapper.deleteById(id);
        return Result.ok();
    }

    /* ==================== 兑换记录 ==================== */

    /**
     * 兑换记录列表：状态（1 未使用 / 2 已服用）/ 商品筛选 + 分页，倒序（新纪录在前）。
     * 行内附用户名与商品名，供后台直接阅读。
     */
    @GetMapping("/records")
    public Result<PageResult<Map<String, Object>>> recordList(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(required = false, defaultValue = "-1") int used,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        if (adminGuard.adminOf(auth) == null) {
            return Result.fail(401, "未登录或登录已失效");
        }

        int p = Math.max(1, page);
        int s = size <= 0 ? 10 : Math.min(size, MAX_SIZE);

        LambdaQueryWrapper<ShopRecord> qw = Wrappers.lambdaQuery(ShopRecord.class);
        if (used == 1 || used == 2) {
            qw.eq(ShopRecord::getUsed, used);
        }
        if (itemId != null && itemId > 0) {
            qw.eq(ShopRecord::getItemId, itemId);
        }

        long total = recordMapper.selectCount(qw);
        qw.orderByDesc(ShopRecord::getId)
                .last("LIMIT " + s + " OFFSET " + (long) (p - 1) * s);
        List<ShopRecord> rows = recordMapper.selectList(qw);

        // 批量取名：用户名 / 商品名（缺失时给占位文案）
        Set<Long> userIds = rows.stream().map(ShopRecord::getUserId).collect(Collectors.toSet());
        Set<Long> itemIds = rows.stream().map(ShopRecord::getItemId).collect(Collectors.toSet());
        Map<Long, String> userNames = userIds.isEmpty() ? Map.of() : userService.listByIds(userIds)
                .stream().collect(Collectors.toMap(User::getId, u -> u.getNickname() != null
                        ? u.getNickname() : u.getUsername(), (a, b) -> a));
        Map<Long, ShopItem> items = itemIds.isEmpty() ? Map.of() : itemMapper.selectBatchIds(itemIds)
                .stream().collect(Collectors.toMap(ShopItem::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> list = new ArrayList<>();
        for (ShopRecord r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("username", userNames.getOrDefault(r.getUserId(), "已注销用户"));
            ShopItem item = items.get(r.getItemId());
            m.put("itemId", r.getItemId());
            m.put("itemName", item == null ? "已删除商品" : item.getName());
            m.put("itemGlyph", item == null ? "🎁" : item.getGlyph());
            m.put("itemIcon", item == null ? null : item.getIcon());
            m.put("itemCategory", item == null ? null : item.getCategory());
            m.put("price", r.getPrice());
            m.put("used", r.getUsed());
            m.put("createdAt", r.getCreatedAt());
            list.add(m);
        }
        return Result.ok(PageResult.of(list, total, p, s));
    }

    // ------------------------------------------------------------------

    private Map<String, Object> itemToMap(ShopItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("code", i.getCode());
        m.put("category", i.getCategory());
        m.put("name", i.getName());
        m.put("subtitle", i.getSubtitle());
        m.put("description", i.getDescription());
        m.put("glyph", i.getGlyph());
        m.put("icon", i.getIcon());
        m.put("color", i.getColor());
        m.put("price", i.getPrice());
        m.put("expGain", i.getExpGain());
        m.put("stock", i.getStock());
        m.put("tags", i.getTags());
        m.put("sortNo", i.getSortNo());
        m.put("status", i.getStatus());
        m.put("createdAt", i.getCreatedAt());
        return m;
    }

    /** 商品字段校验；duplicateId 用于更新时排除自身后查编码重复。返回 null 表示通过。 */
    private String validateItem(ShopItem i, Long duplicateId) {
        if (i == null) {
            return "请求体不能为空";
        }
        String code = i.getCode() == null ? "" : i.getCode().trim();
        if (code.isEmpty() || code.length() > 48) {
            return "商品编码必填，且不超过 48 字";
        }
        if (!code.matches("[A-Za-z0-9_-]+")) {
            return "商品编码只能包含字母、数字、下划线或中划线";
        }
        ShopItem same = itemMapper.selectOne(
                Wrappers.lambdaQuery(ShopItem.class).eq(ShopItem::getCode, code));
        if (same != null && !same.getId().equals(duplicateId)) {
            return "商品编码已存在：" + code;
        }
        if (i.getCategory() == null || !CATEGORIES.contains(i.getCategory())) {
            return "商品分类只能是 fabao 法宝 / danyao 丹药 / gongfa 功法";
        }
        String name = i.getName() == null ? "" : i.getName().trim();
        if (name.isEmpty() || name.length() > 64) {
            return "商品名称必填，且不超过 64 字";
        }
        if (i.getPrice() == null) {
            i.setPrice(10);
        }
        if (i.getPrice() < 0) {
            return "兑换所需情绪值不能为负数";
        }
        if (i.getExpGain() == null) {
            i.setExpGain(0);
        }
        if (i.getExpGain() < 0) {
            return "服用/参悟可得修为不能为负数";
        }
        if (i.getStock() == null) {
            i.setStock(-1);
        }
        if (i.getStock() < -1) {
            return "库存只能是 -1（不限量）或不小于 0 的整数";
        }
        if (i.getSubtitle() != null && i.getSubtitle().length() > 128) {
            return "副标题不超过 128 字";
        }
        if (i.getDescription() != null && i.getDescription().length() > 500) {
            return "商品详情不超过 500 字";
        }
        if (i.getGlyph() != null && i.getGlyph().length() > 16) {
            return "商品图标不超过 16 字";
        }
        if (i.getIcon() != null && i.getIcon().length() > 255) {
            return "商品图标图片路径不超过 255 字";
        }
        if (i.getColor() != null && i.getColor().length() > 16) {
            return "商品主色不超过 16 字";
        }
        if (i.getTags() != null && i.getTags().length() > 255) {
            return "关键词标签不超过 255 字（英文逗号分隔）";
        }
        if (i.getSortNo() != null && i.getSortNo() < 0) {
            return "排序值不能为负数";
        }
        if (i.getStatus() != null && i.getStatus() != 0 && i.getStatus() != 1) {
            return "状态只能是上架或下架";
        }
        return null;
    }

    /** 入库前修剪公共字段 */
    private void normalizeItem(ShopItem i) {
        i.setCode(i.getCode().trim());
        i.setName(i.getName().trim());
        if (i.getStatus() == null) {
            i.setStatus(1);
        }
        if (i.getSortNo() == null) {
            i.setSortNo(0);
        }
    }
}
