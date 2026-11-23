package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.RealmLadder;
import com.fenglin.springboottest.controller.dto.FriendOverview;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.FriendRequest;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.entity.UserCultivation;
import com.fenglin.springboottest.entity.UserEmotion;
import com.fenglin.springboottest.mapper.FriendMapper;
import com.fenglin.springboottest.mapper.FriendRequestMapper;
import com.fenglin.springboottest.mapper.UserCultivationMapper;
import com.fenglin.springboottest.mapper.UserEmotionMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.FriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 好友系统实现（我的主页 · 红线挚友）。
 *
 * 数据约定：
 *  - star_friend          好友关系，双向两行（A-B 存 (A,B) 与 (B,A)）
 *  - star_friend_request  好友申请，同方向唯一（uk_req）；同意落双向好友 + status=2；
 *                         拒绝 status=3（可再次发起，复用原记录改回待处理）；撤回删除记录
 * 展示数据（昵称/阵营/修为/境界）实时从 star_user / star_user_emotion / star_user_cultivation 读。
 */
@Service
public class FriendServiceImpl implements FriendService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 搜索最多返回条数 */
    private static final int SEARCH_LIMIT = 10;

    /** 申请留言长度上限 */
    private static final int MESSAGE_MAX = 128;

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private FriendRequestMapper requestMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserEmotionMapper emotionMapper;

    @Autowired
    private UserCultivationMapper cultivationMapper;

    /* ================= 查询 ================= */

    @Override
    public FriendOverview overview(Long userId) {
        FriendOverview vo = new FriendOverview();

        // 我的好友：扫 user_id 侧，按结缘时间倒序
        List<Friend> links = friendMapper.selectList(Wrappers.lambdaQuery(Friend.class)
                .eq(Friend::getUserId, userId)
                .orderByDesc(Friend::getCreatedAt));
        vo.setFriendCount(links.size());

        Map<Long, User> users = loadUsers(links.stream().map(Friend::getFriendId).distinct().toList());
        List<FriendOverview.FriendCard> cards = new ArrayList<>(links.size());
        for (Friend link : links) {
            User u = users.get(link.getFriendId());
            if (u == null) {
                continue; // 用户已物理删除（后台删号时未清理好友行），跳过展示
            }
            FriendOverview.FriendCard card = toCard(u,
                    link.getCreatedAt() == null ? null : link.getCreatedAt().format(TIME_FMT));
            cards.add(card);
        }
        cards.sort(Comparator.comparing(FriendOverview.FriendCard::getSince,
                Comparator.nullsLast(Comparator.reverseOrder())));
        vo.setFriends(cards);

        // 待处理申请：收到的 + 发出的
        List<FriendRequest> requests = requestMapper.selectList(Wrappers.lambdaQuery(FriendRequest.class)
                .eq(FriendRequest::getStatus, FriendRequest.ST_PENDING)
                .and(w -> w.eq(FriendRequest::getToUserId, userId)
                        .or()
                        .eq(FriendRequest::getFromUserId, userId)));

        List<FriendRequest> incoming = new ArrayList<>();
        List<FriendRequest> outgoing = new ArrayList<>();
        for (FriendRequest req : requests) {
            if (req.getToUserId() != null && req.getToUserId().equals(userId)) {
                incoming.add(req);
            } else if (req.getFromUserId() != null && req.getFromUserId().equals(userId)) {
                outgoing.add(req);
            }
        }
        incoming.sort(Comparator.comparing(FriendRequest::getId).reversed());
        outgoing.sort(Comparator.comparing(FriendRequest::getId).reversed());
        vo.setIncoming(buildRequestCards(incoming, true));
        vo.setOutgoing(buildRequestCards(outgoing, false));
        vo.setServerTime(LocalDateTime.now().format(TIME_FMT));
        return vo;
    }

    @Override
    public List<FriendOverview.FriendCard> search(Long userId, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return Collections.emptyList();
        }
        if (kw.length() > 32) {
            throw new IllegalStateException("关键词太长了，换个短一点的试试");
        }

        List<User> users = userMapper.selectList(Wrappers.lambdaQuery(User.class)
                .eq(User::getStatus, 1)
                .and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw))
                .ne(User::getId, userId)
                .last("LIMIT " + SEARCH_LIMIT));
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        // 关系标记：已是好友 / 我发出的待处理 / 对方发给我的待处理
        Set<Long> friendIds = friendIdsOf(userId);
        Map<Long, Long> outgoingToReq = new HashMap<>();
        Map<Long, Long> incomingFromReq = new HashMap<>();
        for (FriendRequest req : requestMapper.selectList(Wrappers.lambdaQuery(FriendRequest.class)
                .eq(FriendRequest::getStatus, FriendRequest.ST_PENDING)
                .and(w -> w.eq(FriendRequest::getFromUserId, userId)
                        .or()
                        .eq(FriendRequest::getToUserId, userId)))) {
            if (userId.equals(req.getFromUserId())) {
                outgoingToReq.put(req.getToUserId(), req.getId());
            } else {
                incomingFromReq.put(req.getFromUserId(), req.getId());
            }
        }

        List<FriendOverview.FriendCard> list = new ArrayList<>(users.size());
        for (User u : users) {
            FriendOverview.FriendCard card = toCard(u, null);
            if (friendIds.contains(u.getId())) {
                card.setRelation("friend");
            } else if (outgoingToReq.containsKey(u.getId())) {
                card.setRelation("outgoing");
                card.setSince("待对方处理");
            } else if (incomingFromReq.containsKey(u.getId())) {
                card.setRelation("incoming");
            } else {
                card.setRelation("none");
            }
            list.add(card);
        }
        return list;
    }

    /* ================= 动作 ================= */

    @Override
    public FriendOverview sendRequest(Long userId, Long targetUserId, String message) {
        User target = loadActiveUser(targetUserId, "这位红线仙不存在或已停用");
        if (targetUserId.equals(userId)) {
            throw new IllegalStateException("和自己结缘？这条红线就不用牵了吧");
        }
        if (isFriend(userId, targetUserId)) {
            throw new IllegalStateException("你和「" + displayName(target) + "」已经是挚友啦");
        }
        String msg = message == null ? null : message.trim();
        if (msg != null && msg.length() > MESSAGE_MAX) {
            throw new IllegalStateException("留言最多 " + MESSAGE_MAX + " 个字");
        }

        LocalDateTime now = LocalDateTime.now();
        synchronized (this) {
            // 同方向已有待处理申请 → 拒绝重复；被拒过 → 复用原记录改回待处理
            FriendRequest exist = requestMapper.selectOne(Wrappers.lambdaQuery(FriendRequest.class)
                    .eq(FriendRequest::getFromUserId, userId)
                    .eq(FriendRequest::getToUserId, targetUserId));
            if (exist != null) {
                if (exist.getStatus() == FriendRequest.ST_PENDING) {
                    throw new IllegalStateException("申请已经在路上啦，等「" + displayName(target) + "」处理吧");
                }
                exist.setStatus(FriendRequest.ST_PENDING);
                exist.setMessage(msg);
                exist.setCreatedAt(now);
                exist.setHandledAt(null);
                requestMapper.updateById(exist);
            } else {
                // 反方向有待处理申请且对方还没同意 → 提示直接去同意对方的申请
                Long reverse = requestMapper.selectCount(Wrappers.lambdaQuery(FriendRequest.class)
                        .eq(FriendRequest::getFromUserId, targetUserId)
                        .eq(FriendRequest::getToUserId, userId)
                        .eq(FriendRequest::getStatus, FriendRequest.ST_PENDING));
                if (reverse != null && reverse > 0) {
                    throw new IllegalStateException("「" + displayName(target) + "」也向你递了红线，去「收到的申请」里同意吧");
                }
                FriendRequest req = new FriendRequest();
                req.setFromUserId(userId);
                req.setToUserId(targetUserId);
                req.setMessage(msg);
                req.setStatus(FriendRequest.ST_PENDING);
                req.setCreatedAt(now);
                try {
                    requestMapper.insert(req);
                } catch (DuplicateKeyException e) {
                    throw new IllegalStateException("申请已经在路上啦，等「" + displayName(target) + "」处理吧");
                }
            }
        }
        return overview(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized FriendOverview accept(Long userId, Long requestId) {
        FriendRequest req = loadRequest(requestId);
        if (!userId.equals(req.getToUserId())) {
            throw new IllegalStateException("这条申请不是递给你的");
        }
        if (req.getStatus() != FriendRequest.ST_PENDING) {
            throw new IllegalStateException("这条申请已被处理过啦");
        }
        User from = loadActiveUser(req.getFromUserId(), "发起人已注销或停用，无法同意");
        if (isFriend(userId, req.getFromUserId())) {
            // 兜底：已是好友（如另一方向先同意过）→ 直接把申请标掉
            req.setStatus(FriendRequest.ST_ACCEPTED);
            req.setHandledAt(LocalDateTime.now());
            requestMapper.updateById(req);
            return overview(userId);
        }

        LocalDateTime now = LocalDateTime.now();
        req.setStatus(FriendRequest.ST_ACCEPTED);
        req.setHandledAt(now);
        requestMapper.updateById(req);
        insertFriendPair(userId, req.getFromUserId(), now);

        System.out.println("[Friend] " + userId + " 与 " + from.getUsername() + " 结为挚友");
        return overview(userId);
    }

    @Override
    public FriendOverview reject(Long userId, Long requestId) {
        FriendRequest req = loadRequest(requestId);
        if (!userId.equals(req.getToUserId())) {
            throw new IllegalStateException("这条申请不是递给你的");
        }
        if (req.getStatus() != FriendRequest.ST_PENDING) {
            throw new IllegalStateException("这条申请已被处理过啦");
        }
        req.setStatus(FriendRequest.ST_REJECTED);
        req.setHandledAt(LocalDateTime.now());
        requestMapper.updateById(req);
        return overview(userId);
    }

    @Override
    public FriendOverview cancel(Long userId, Long requestId) {
        FriendRequest req = loadRequest(requestId);
        if (!userId.equals(req.getFromUserId())) {
            throw new IllegalStateException("只能撤回自己发出的申请");
        }
        if (req.getStatus() != FriendRequest.ST_PENDING) {
            throw new IllegalStateException("这条申请已被处理，无需撤回");
        }
        requestMapper.deleteById(requestId);
        return overview(userId);
    }

    @Override
    public FriendOverview removeFriend(Long userId, Long friendUserId) {
        User friend = loadActiveUser(friendUserId, "这位红线仙不存在或已停用");
        if (!isFriend(userId, friendUserId)) {
            throw new IllegalStateException("你和「" + displayName(friend) + "」本来就不是好友");
        }
        friendMapper.delete(Wrappers.lambdaQuery(Friend.class)
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, friendUserId));
        friendMapper.delete(Wrappers.lambdaQuery(Friend.class)
                .eq(Friend::getUserId, friendUserId).eq(Friend::getFriendId, userId));
        return overview(userId);
    }

    /* ================= 内部辅助 ================= */

    /** 双向插入好友两行（任一行已存在则跳过该行） */
    private void insertFriendPair(Long a, Long b, LocalDateTime now) {
        for (long[] pair : new long[][]{{a, b}, {b, a}}) {
            Long count = friendMapper.selectCount(Wrappers.lambdaQuery(Friend.class)
                    .eq(Friend::getUserId, pair[0]).eq(Friend::getFriendId, pair[1]));
            if (count != null && count > 0) {
                continue;
            }
            Friend row = new Friend();
            row.setUserId(pair[0]);
            row.setFriendId(pair[1]);
            row.setCreatedAt(now);
            try {
                friendMapper.insert(row);
            } catch (DuplicateKeyException ignore) {
                // 并发下唯一键兜底
            }
        }
    }

    private Set<Long> friendIdsOf(Long userId) {
        Set<Long> ids = new HashSet<>();
        for (Friend f : friendMapper.selectList(Wrappers.lambdaQuery(Friend.class)
                .eq(Friend::getUserId, userId))) {
            ids.add(f.getFriendId());
        }
        return ids;
    }

    private boolean isFriend(Long userId, Long otherId) {
        Long count = friendMapper.selectCount(Wrappers.lambdaQuery(Friend.class)
                .eq(Friend::getUserId, userId).eq(Friend::getFriendId, otherId));
        return count != null && count > 0;
    }

    private FriendRequest loadRequest(Long requestId) {
        FriendRequest req = requestId == null ? null : requestMapper.selectById(requestId);
        if (req == null) {
            throw new IllegalStateException("这条申请不存在或已被撤回");
        }
        return req;
    }

    private User loadActiveUser(Long id, String errText) {
        User u = id == null ? null : userMapper.selectById(id);
        if (u == null || u.getStatus() == null || u.getStatus() != 1) {
            throw new IllegalStateException(errText);
        }
        return u;
    }

    private List<FriendOverview.RequestCard> buildRequestCards(List<FriendRequest> requests, boolean incoming) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> otherIds = new ArrayList<>(requests.size());
        for (FriendRequest req : requests) {
            otherIds.add(incoming ? req.getFromUserId() : req.getToUserId());
        }
        Map<Long, User> users = loadUsers(otherIds);
        List<FriendOverview.RequestCard> list = new ArrayList<>(requests.size());
        for (FriendRequest req : requests) {
            Long otherId = incoming ? req.getFromUserId() : req.getToUserId();
            User u = users.get(otherId);
            FriendOverview.RequestCard card = new FriendOverview.RequestCard();
            card.setId(req.getId());
            card.setUserId(otherId);
            card.setMessage(req.getMessage());
            card.setCreatedAt(req.getCreatedAt() == null ? null : req.getCreatedAt().format(TIME_FMT));
            if (u != null) {
                card.setUsername(u.getUsername());
                card.setNickname(u.getNickname() == null || u.getNickname().isEmpty() ? u.getUsername() : u.getNickname());
                fillDisplay(card, u);
            } else {
                card.setUsername("已注销用户");
                card.setNickname("已注销用户");
            }
            list.add(card);
        }
        return list;
    }

    private Map<Long, User> loadUsers(List<Long> ids) {
        Map<Long, User> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (User u : userMapper.selectList(Wrappers.lambdaQuery(User.class).in(User::getId, ids))) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private FriendOverview.FriendCard toCard(User u, String since) {
        FriendOverview.FriendCard card = new FriendOverview.FriendCard();
        card.setUserId(u.getId());
        card.setUsername(u.getUsername());
        card.setNickname(u.getNickname() == null || u.getNickname().isEmpty() ? u.getUsername() : u.getNickname());
        fillDisplay(card, u);
        card.setSince(since);
        return card;
    }

    /** 填充阵营 / 情绪值 / 修为 / 境界（缺情绪或修为行按 0 处理） */
    private void fillDisplay(FriendOverview.FriendCard card, User u) {
        UserEmotion emotion = emotionMapper.selectById(u.getId());
        int emotionVal = emotion == null || emotion.getEmotion() == null ? 0 : emotion.getEmotion();
        int camp = emotion == null || emotion.getCamp() == null ? 1 : emotion.getCamp();
        UserCultivation cult = cultivationMapper.selectById(u.getId());
        int cultivation = cult == null || cult.getTotalCultivation() == null ? 0 : cult.getTotalCultivation();

        card.setCamp(camp);
        card.setCampText(camp == 2 ? "妖族" : "修士");
        card.setEmotion(emotionVal);
        card.setCultivation(cultivation);
        card.setRealmTitle((camp == 2 ? RealmLadder.DEMON_TITLES : RealmLadder.CULTIVATOR_TITLES)
                [RealmLadder.indexOf(cultivation)]);
    }

    private void fillDisplay(FriendOverview.RequestCard card, User u) {
        UserEmotion emotion = emotionMapper.selectById(u.getId());
        int camp = emotion == null || emotion.getCamp() == null ? 1 : emotion.getCamp();
        UserCultivation cult = cultivationMapper.selectById(u.getId());
        int cultivation = cult == null || cult.getTotalCultivation() == null ? 0 : cult.getTotalCultivation();

        card.setCamp(camp);
        card.setCampText(camp == 2 ? "妖族" : "修士");
        card.setCultivation(cultivation);
        card.setRealmTitle((camp == 2 ? RealmLadder.DEMON_TITLES : RealmLadder.CULTIVATOR_TITLES)
                [RealmLadder.indexOf(cultivation)]);
    }

    private static String displayName(User u) {
        return u.getNickname() == null || u.getNickname().isEmpty() ? u.getUsername() : u.getNickname();
    }
}
