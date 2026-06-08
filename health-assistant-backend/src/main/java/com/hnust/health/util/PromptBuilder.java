package com.hnust.health.util;

import com.hnust.health.model.HealthProfile;
import com.hnust.health.util.WeightTrendAnalyzer.TrendResult;

public class PromptBuilder {

    public static String buildSystemPrompt(HealthProfile profile, TrendResult trend) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位拥有20年临床经验的注册营养师和运动生理学专家。");
        sb.append("请基于以下用户健康数据，生成一份为期一周的个性化干预计划。\n\n");

        sb.append("## 用户基本信息\n");
        sb.append("- 年龄：").append(profile.getAge()).append("岁\n");
        sb.append("- 性别：").append(gender(profile.getGender())).append("\n");
        sb.append("- 身高：").append(profile.getHeightCm()).append("cm\n");
        sb.append("- 建档体重：").append(profile.getBaselineWeight()).append("kg\n");
        sb.append("- 活动水平：").append(act(profile.getActivityLevel())).append("\n");
        sb.append("- 饮食偏好：").append(diet(profile.getDietPreference())).append("\n");
        sb.append("- 健康目标：").append(goal(profile.getHealthGoal())).append("\n\n");

        sb.append("## 历史体重趋势\n");
        sb.append(trend.trendDescription()).append("\n");
        if (trend.startWeight() != null) {
            sb.append("- 起始体重(").append(trend.startWeight()).append("kg) → 当前体重(").append(trend.endWeight()).append("kg)\n");
            sb.append("- 总变化：").append(trend.totalChange()).append("kg，周均：").append(trend.weeklyChangeRate()).append("kg/周\n");
        }
        sb.append("\n");

        sb.append("## 输出要求（严格JSON格式，不要markdown代码块）\n\n");

        sb.append("请输出以下JSON结构，每个字段都必须完整填充：\n\n");
        sb.append("{\n");
        sb.append("  \"diet_plan\": {\n");
        sb.append("    \"daily_calories\": 数字,\n");
        sb.append("    \"macros\": {\"protein_g\": 数字, \"fat_g\": 数字, \"carbs_g\": 数字},\n");
        sb.append("    \"day1\": {\"total_calories\": 数字, \"meals\": [\n");
        sb.append("      {\"meal\": \"早餐\", \"foods\": [\"食物名称 份量\", ...], \"calories\": 数字, \"protein\": 数字, \"carbs\": 数字, \"fat\": 数字},\n");
        sb.append("      {\"meal\": \"午餐\", \"foods\": [...], \"calories\": 数字, \"protein\": 数字, \"carbs\": 数字, \"fat\": 数字},\n");
        sb.append("      {\"meal\": \"晚餐\", \"foods\": [...], \"calories\": 数字, \"protein\": 数字, \"carbs\": 数字, \"fat\": 数字}\n");
        sb.append("    ]},\n");
        sb.append("    \"day2\": {...}, ... \"day7\": {...}\n");
        sb.append("  },\n");
        sb.append("  \"workout_plan\": {\n");
        sb.append("    \"weekly_schedule\": [\n");
        sb.append("      {\"day\": \"周一\", \"type\": \"训练类型\", \"exercises\": [\"具体动作 组数×次数\", ...], \"duration_min\": 数字, \"intensity\": \"强度\"},\n");
        sb.append("      ...共7天\n");
        sb.append("    ]\n");
        sb.append("  },\n");
        sb.append("  \"analysis\": \"简要分析及本周策略说明\"\n");
        sb.append("}\n\n");

        sb.append("重要约束：\n");
        sb.append("- foods数组中每项必须包含食物名称和份量，如\"鸡胸肉150g\"\n");
        sb.append("- exercises数组中每项必须包含动作名称和组数次数，如\"杠铃深蹲 4组×8次\"\n");
        sb.append("- 热量缺口不超过每日总消耗25%，蛋白质不低于1.6g/kg\n");
        sb.append("- 只输出JSON，不要输出任何解释文字或markdown标记\n");

        return sb.toString();
    }

    public static String buildUserMessage() {
        return "请为我生成本周的个性化饮食和运动计划。只输出JSON。";
    }

    private static String gender(Integer g) { return g == null ? "未知" : g == 1 ? "男性" : g == 2 ? "女性" : "其他"; }
    private static String act(String s) { return s == null ? "未知" : s.equalsIgnoreCase("LOW") ? "低活动量" : s.equalsIgnoreCase("MODERATE") ? "中等活动量" : s.equalsIgnoreCase("HIGH") ? "高活动量" : s; }
    private static String diet(String s) { return s == null ? "未知" : s.equalsIgnoreCase("KETO") ? "生酮饮食" : s.equalsIgnoreCase("VEGAN") ? "纯素饮食" : s.equalsIgnoreCase("BALANCED") ? "均衡饮食" : s; }
    private static String goal(String s) { return s == null ? "未知" : s.equalsIgnoreCase("FAT_LOSS") ? "减重减脂" : s.equalsIgnoreCase("MUSCLE_GAIN") ? "增肌塑形" : s.equalsIgnoreCase("MAINTENANCE") ? "维持体重" : s; }
}
