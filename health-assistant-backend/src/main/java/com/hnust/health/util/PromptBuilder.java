package com.hnust.health.util;

import com.hnust.health.model.HealthProfile;
import com.hnust.health.util.WeightTrendAnalyzer.TrendResult;

/**
 * DeepSeek 大模型提示词构建工具
 * 负责将用户档案、历史体重趋势等信息组装为结构化 System Prompt
 */
public class PromptBuilder {

    /**
     * 构建发送给 DeepSeek 的系统提示词
     */
    public static String buildSystemPrompt(HealthProfile profile, TrendResult trend) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位拥有20年临床经验的注册营养师和运动生理学专家。");
        sb.append("请严格基于以下用户健康数据和体重变化趋势，生成一份为期一周的个性化干预计划。\n\n");

        sb.append("## 用户基本信息\n");
        sb.append("- 年龄：").append(profile.getAge()).append("岁\n");
        sb.append("- 性别：").append(getGenderLabel(profile.getGender())).append("\n");
        sb.append("- 身高：").append(profile.getHeightCm()).append("cm\n");
        sb.append("- 建档体重：").append(profile.getBaselineWeight()).append("kg\n");
        sb.append("- 活动水平：").append(getActivityLabel(profile.getActivityLevel())).append("\n");
        sb.append("- 饮食偏好：").append(getDietLabel(profile.getDietPreference())).append("\n");
        sb.append("- 健康目标：").append(getGoalLabel(profile.getHealthGoal())).append("\n\n");

        sb.append("## 历史体重趋势分析\n");
        sb.append(trend.trendDescription()).append("\n");
        if (trend.startWeight() != null) {
            sb.append("- 起始体重：").append(trend.startWeight()).append("kg\n");
            sb.append("- 当前体重：").append(trend.endWeight()).append("kg\n");
            sb.append("- 总变化量：").append(trend.totalChange()).append("kg\n");
            sb.append("- 周均变化率：").append(trend.weeklyChangeRate()).append("kg/周\n");
        }
        sb.append("\n");

        sb.append("## 输出要求\n");
        sb.append("请以严格的JSON格式输出，包含以下三个部分：\n");
        sb.append("1. diet_plan：饮食处方，包含每日三餐的建议食材、热量、宏量营养素分配\n");
        sb.append("2. workout_plan：运动处方，包含每周的运动类型、强度、时长和频率\n");
        sb.append("3. analysis：对当前体重趋势的简要分析及本周策略说明\n\n");
        sb.append("重要约束：\n");
        sb.append("- 如果用户处于减重平台期，应引入碳水循环或HIIT等突破策略\n");
        sb.append("- 热量缺口不应超过每日总消耗的25%\n");
        sb.append("- 确保蛋白质摄入不低于每公斤体重1.6g\n");
        sb.append("- 输出必须是合法的JSON格式，不要包含任何额外的解释文字\n");

        return sb.toString();
    }

    public static String buildUserMessage() {
        return "请基于以上信息，为我生成本周的个性化饮食和运动计划。";
    }

    private static String getGenderLabel(Integer gender) {
        if (gender == null) return "未知";
        return switch (gender) {
            case 1 -> "男性";
            case 2 -> "女性";
            default -> "其他";
        };
    }

    private static String getActivityLabel(String level) {
        if (level == null) return "未知";
        return switch (level.toUpperCase()) {
            case "LOW" -> "低活动量（久坐为主）";
            case "MODERATE" -> "中等活动量（每周3-4次运动）";
            case "HIGH" -> "高活动量（每日运动或体力劳动）";
            default -> level;
        };
    }

    private static String getDietLabel(String preference) {
        if (preference == null) return "未知";
        return switch (preference.toUpperCase()) {
            case "KETO" -> "生酮饮食";
            case "VEGAN" -> "纯素饮食";
            case "BALANCED" -> "均衡饮食";
            default -> preference;
        };
    }

    private static String getGoalLabel(String goal) {
        if (goal == null) return "未知";
        return switch (goal.toUpperCase()) {
            case "FAT_LOSS" -> "减重减脂";
            case "MUSCLE_GAIN" -> "增肌塑形";
            case "MAINTENANCE" -> "维持当前体重";
            default -> goal;
        };
    }
}
