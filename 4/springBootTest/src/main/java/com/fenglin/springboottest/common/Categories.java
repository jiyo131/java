package com.fenglin.springboottest.common;

import java.util.Map;

/**
 * 商店分类编码 → 中文名，供商店 / 修炼两个模块共用。
 */
public final class Categories {

    private static final Map<String, String> TEXT = Map.of(
            "fabao", "法宝",
            "danyao", "丹药",
            "gongfa", "功法");

    private Categories() {
    }

    public static String textOf(String category) {
        return TEXT.getOrDefault(category, "宝贝");
    }
}
