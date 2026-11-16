package com.fenglin.springboottest.common;

import com.fenglin.springboottest.controller.dto.ProfileVo;

import java.util.ArrayList;
import java.util.List;

/**
 * 境界阶梯（2026-11-16 起由「累计修为」驱动，与情绪值/商店货币分离）。
 *
 * 修士境界：练气期 → 筑基期 → 金丹期 → 元婴期 → 化神期
 * 妖族境界：幼妖期 → 妖卒期 → 妖将期 → 妖王期 → 妖皇期
 *
 * 境界只看累计修为（只增不减），因此花钱兑换商品不会掉境界。
 */
public final class RealmLadder {

    /** 境界阈值（升序），修为达到即晋升 */
    public static final int[] FLOORS = {0, 60, 180, 360, 600};
    public static final String[] CULTIVATOR_TITLES = {"练气期", "筑基期", "金丹期", "元婴期", "化神期"};
    public static final String[] DEMON_TITLES = {"幼妖期", "妖卒期", "妖将期", "妖王期", "妖皇期"};

    private RealmLadder() {
    }

    /** 按累计修为生成一条境界线：标记当前境 / 已达成 / 本境进度 */
    public static List<ProfileVo.RealmNode> build(String[] titles, int cultivation) {
        int currentIdx = 0;
        for (int i = 0; i < FLOORS.length; i++) {
            if (cultivation >= FLOORS[i]) {
                currentIdx = i;
            }
        }

        List<ProfileVo.RealmNode> list = new ArrayList<>(titles.length);
        for (int i = 0; i < titles.length; i++) {
            ProfileVo.RealmNode node = new ProfileVo.RealmNode();
            node.setIndex(i + 1);
            node.setTitle(titles[i]);
            node.setFloor(FLOORS[i]);
            node.setReached(cultivation >= FLOORS[i]);
            node.setCurrent(i == currentIdx);

            Integer next = i + 1 < FLOORS.length ? FLOORS[i + 1] : null;
            node.setNextThreshold(next);
            if (next == null) {
                node.setProgress(100);
            } else {
                int span = next - FLOORS[i];
                int pct = span <= 0 ? 100 : (int) Math.round((cultivation - FLOORS[i]) * 100.0 / span);
                node.setProgress(Math.max(0, Math.min(100, pct)));
            }
            list.add(node);
        }
        return list;
    }

    /** 某个修为值对应的境界下标（0 起） */
    public static int indexOf(int cultivation) {
        int index = 0;
        for (int i = 0; i < FLOORS.length; i++) {
            if (cultivation >= FLOORS[i]) {
                index = i;
            }
        }
        return index;
    }
}
