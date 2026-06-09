package com.hnust.health.util;

/**
 * 饮食健康评分器 — 基于关键词的简单启发式评估
 * 评分范围: -2(非常不健康) ~ +2(非常健康), 0=中性/无法判断
 */
public class MealHealthScorer {

    // 健康食材/做法 → 加分 (匹配到越多越健康, 上限+2)
    private static final String[] HEALTHY = {
            "蔬菜", "西兰花", "菠菜", "青菜", "白菜", "生菜", "黄瓜", "番茄", "西红柿",
            "胡萝卜", "芹菜", "芦笋", "莴笋", "娃娃菜", "油菜", "豆芽", "海带", "木耳",
            "水果", "蓝莓", "草莓", "苹果", "橙子", "香蕉", "猕猴桃", "圣女果",
            "鸡胸肉", "鸡胸", "去皮鸡腿", "鱼", "鲈鱼", "三文鱼", "龙利鱼", "虾", "虾仁", "白灼虾",
            "豆腐", "豆浆", "牛奶", "酸奶", "鸡蛋", "水煮蛋", "煮鸡蛋", "蛋清",
            "燕麦", "燕麦片", "全麦", "荞麦", "糙米", "藜麦", "杂粮", "红薯", "紫薯", "玉米", "南瓜", "芋头",
            "蒸", "水煮", "清蒸", "清炒", "凉拌", "清炖", "沙拉",
            "坚果", "核桃", "杏仁", "粗粮", "小米", "发糕",
    };

    // 不健康食材/做法 → 扣分 (下限-2)
    private static final String[] UNHEALTHY = {
            "炸", "油炸", "烧烤", "烤串", "火锅", "麻辣烫",
            "奶茶", "可乐", "汽水", "雪碧", "芬达",
            "薯片", "蛋糕", "冰淇淋", "冰激凌", "巧克力", "糖果", "甜点", "饼干",
            "汉堡", "方便面", "泡面", "披萨",
            "肥肉", "红烧肉", "回锅肉", "五花肉", "腊肉", "香肠", "培根",
            "奶油", "黄油", "猪油",
            "啤酒", "白酒", "红酒", "酒精",
    };

    /**
     * 对食物描述打分
     * @param foodDesc 食物描述文本
     * @param mealType 餐次类型 (BREAKFAST/LUNCH/DINNER/SNACK)
     * @param isSkip   是否为"不吃/跳过"记录
     * @return 健康评分 -2 ~ +2
     */
    public static int score(String foodDesc, String mealType, boolean isSkip) {
        // 不吃 = 扣分
        if (isSkip) return -1;

        if (foodDesc == null || foodDesc.isBlank()) return 0;

        int healthy = 0;
        int unhealthy = 0;

        for (String kw : HEALTHY) {
            if (foodDesc.contains(kw)) healthy++;
        }
        for (String kw : UNHEALTHY) {
            if (foodDesc.contains(kw)) unhealthy++;
        }

        // 特殊规则: 加餐吃水果/酸奶/坚果 → 偏健康
        if ("SNACK".equals(mealType)) {
            if (foodDesc.contains("水果") || foodDesc.contains("酸奶") || foodDesc.contains("牛奶")
                    || foodDesc.contains("坚果") || foodDesc.contains("鸡蛋")) {
                healthy++;
            }
        }

        // 计算得分
        int raw = healthy - unhealthy;
        return clamp(raw, -2, 2);
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
