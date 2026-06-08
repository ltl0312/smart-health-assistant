package com.hnust.health.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.health.model.AiPlan;
import com.hnust.health.model.HealthProfile;
import com.hnust.health.model.WeightRecord;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class MarkdownGenerator {

    private static final ObjectMapper om = new ObjectMapper();
    private static final Map<String, String> CN = new LinkedHashMap<>();
    static {
        CN.put("daily_calories", "每日热量目标"); CN.put("daily_calorie_target", "每日热量目标");
        CN.put("macronutrient_split", "宏量营养素分配"); CN.put("macros", "宏量营养素");
        CN.put("sample_menu", "示例菜单"); CN.put("sample_week", "一周食谱");
        CN.put("weekly_schedule", "每周训练安排"); CN.put("daily_meals", "每日餐食");
        CN.put("protein_g", "蛋白质"); CN.put("fat_g", "脂肪"); CN.put("carbs_g", "碳水");
        CN.put("protein", "蛋白质"); CN.put("fat", "脂肪"); CN.put("carbs", "碳水");
        CN.put("calories", "热量"); CN.put("total_calories", "总热量");
        CN.put("meals", "餐次"); CN.put("meal", "餐别"); CN.put("foods", "食物清单");
        CN.put("Breakfast", "早餐"); CN.put("Lunch", "午餐"); CN.put("Dinner", "晚餐"); CN.put("Snack", "加餐");
        CN.put("day", "日期"); CN.put("type", "训练类型"); CN.put("exercises", "训练动作");
        CN.put("duration_min", "时长"); CN.put("intensity", "强度"); CN.put("notes", "备注");
        CN.put("Monday", "周一"); CN.put("Tuesday", "周二"); CN.put("Wednesday", "周三");
        CN.put("Thursday", "周四"); CN.put("Friday", "周五"); CN.put("Saturday", "周六"); CN.put("Sunday", "周日");
        CN.put("description", "趋势分析"); CN.put("startWeight", "起始体重");
        CN.put("endWeight", "当前体重"); CN.put("totalChange", "总变化量"); CN.put("weeklyRate", "周均变化率");
        CN.put("day1","第一天"); CN.put("day2","第二天"); CN.put("day3","第三天");
        CN.put("day4","第四天"); CN.put("day5","第五天"); CN.put("day6","第六天"); CN.put("day7","第七天");
    }

    // ===== 单份报告 =====
    public static String generatePlanReport(AiPlan plan) {
        StringBuilder md = new StringBuilder();
        String time = plan.getCreatedAt() != null
                ? plan.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "未知";
        md.append("# 🏥 AI 健康干预计划报告\n\n");
        md.append("> **周期起始**：").append(plan.getCycleStartDate()).append("  \n");
        md.append("> **生成时间**：").append(time).append("\n\n---\n\n");

        md.append("## 📊 体重趋势分析\n\n");
        renderSection(md, plan.getMemoryContextSnapshot());

        md.append("## 🍽️ 饮食处方\n\n");
        renderDiet(md, plan.getDietPlanJson());

        md.append("## 🏃 运动处方\n\n");
        renderWorkout(md, plan.getWorkoutPlanJson());

        if (plan.getLlmReasoningChain() != null && !plan.getLlmReasoningChain().isBlank()) {
            md.append("## 🧠 AI 推理过程\n\n> ").append(plan.getLlmReasoningChain()).append("\n\n");
        }
        md.append("---\n\n*📝 由智能健康助手 AI 自动生成*\n");
        return md.toString();
    }

    // ===== 完整档案导出 =====
    public static String generateHealthExport(HealthProfile profile, List<WeightRecord> weights, List<AiPlan> plans) {
        StringBuilder md = new StringBuilder();
        md.append("# 📋 个人健康档案\n\n");
        if (profile != null) {
            md.append("## 👤 基本信息\n\n");
            md.append("| 项目 | 数据 |\n|:------|:-----|\n");
            md.append("| **年龄** | ").append(profile.getAge()).append(" 岁 |\n");
            md.append("| **性别** | ").append(gCN(profile.getGender())).append(" |\n");
            md.append("| **身高** | ").append(profile.getHeightCm()).append(" cm |\n");
            md.append("| **建档体重** | ").append(profile.getBaselineWeight()).append(" kg |\n");
            md.append("| **活动水平** | ").append(aCN(profile.getActivityLevel())).append(" |\n");
            md.append("| **饮食偏好** | ").append(dCN(profile.getDietPreference())).append(" |\n");
            md.append("| **健康目标** | ").append(gCN2(profile.getHealthGoal())).append(" |\n\n");
        }
        if (weights != null && !weights.isEmpty()) {
            md.append("## ⚖️ 体重记录\n\n");
            md.append("| 日期 | 体重 (kg) | BMI |\n|:------|:----------|:----|\n");
            for (WeightRecord w : weights)
                md.append("| ").append(w.getRecordDate()).append(" | **").append(w.getCurrentWeight()).append("** | ").append(w.getCalculatedBmi() != null ? w.getCalculatedBmi() : "--").append(" |\n");
            md.append("\n");
        }
        if (plans != null && !plans.isEmpty()) {
            md.append("## 🤖 AI 计划记录\n\n");
            for (AiPlan plan : plans) {
                String t = plan.getCreatedAt() != null ? plan.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) : "";
                md.append("### 📅 ").append(plan.getCycleStartDate()).append(t.isEmpty() ? "" : "（" + t + "）").append("\n\n");
                md.append("**📊 趋势**："); appendSnap(md, plan.getMemoryContextSnapshot());
                md.append("**🍽️ 饮食**："); appendDietS(md, plan.getDietPlanJson());
                md.append("**🏃 运动**："); appendSportS(md, plan.getWorkoutPlanJson());
                if (plan.getLlmReasoningChain() != null && !plan.getLlmReasoningChain().isBlank())
                    md.append("**🧠 分析**：").append(plan.getLlmReasoningChain()).append("\n");
                md.append("\n---\n\n");
            }
        }
        md.append("\n*📝 导出时间：").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("*\n");
        return md.toString();
    }

    // ===== 渲染：通用节点 → 列表 =====
    private static void renderSection(StringBuilder md, String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) { md.append("*（无数据）*\n\n"); return; }
        try {
            JsonNode root = om.readTree(jsonStr);
            if (root.isObject()) writeKV(md, root);
            else if (root.isArray()) for (JsonNode item : root) writeKV(md, item);
        } catch (Exception e) { md.append(jsonStr).append("\n"); }
        md.append("\n");
    }

    private static void writeKV(StringBuilder md, JsonNode node) {
        var it = node.fields();
        while (it.hasNext()) {
            var e = it.next();
            String label = CN.getOrDefault(e.getKey(), e.getKey());
            JsonNode v = e.getValue();
            if (v.isNumber()) {
                String u = e.getKey().contains("calorie") ? " kcal" : e.getKey().contains("weight") || e.getKey().contains("Weight") ? " kg" : e.getKey().contains("protein") || e.getKey().contains("fat") || e.getKey().contains("carbs") ? " g" : e.getKey().contains("duration") ? " 分钟" : e.getKey().contains("rate") ? " kg/周" : "";
                md.append("- **").append(label).append("**：").append(v.asText()).append(u).append("\n");
            } else if (v.isTextual()) {
                md.append("- **").append(label).append("**：").append(v.asText()).append("\n");
            }
        }
    }

    // ===== 渲染：饮食 → 表格 =====
    private static void renderDiet(StringBuilder md, String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) { md.append("*（无数据）*\n\n"); return; }
        try {
            JsonNode root = om.readTree(jsonStr);

            // 扁平摘要结构 → 简单列表
            if (root.has("daily_calories") || root.has("daily_calorie_target")) {
                writeKV(md, root); md.append("\n"); return;
            }

            // day1-day7 周计划 → 表格
            List<String> dayKeys = new ArrayList<>();
            for (int i = 1; i <= 7; i++) if (root.has("day" + i)) dayKeys.add("day" + i);
            if (!dayKeys.isEmpty()) {
                md.append("| 日期 | 餐别 | 食物 | 热量 | 蛋白质 | 碳水 | 脂肪 |\n");
                md.append("|:------|:------|:-----|:-----|:-------|:-----|:-----|\n");
                for (String dk : dayKeys) {
                    JsonNode day = root.get(dk);
                    JsonNode meals = day.has("meals") ? day.get("meals") : null;
                    if (meals != null && meals.isArray()) {
                        boolean first = true;
                        for (JsonNode meal : meals) {
                            String mealName = meal.has("meal") ? CN.getOrDefault(meal.get("meal").asText(), meal.get("meal").asText()) : "";
                            String foods = meal.has("foods") && meal.get("foods").isArray() ? arrJoin(meal.get("foods"), "、") : "";
                            String cal = meal.has("calories") ? meal.get("calories").asText() : "--";
                            String prot = meal.has("protein") ? meal.get("protein").asText() : "--";
                            String carb = meal.has("carbs") ? meal.get("carbs").asText() : "--";
                            String fat = meal.has("fat") ? meal.get("fat").asText() : "--";
                            md.append("| ").append(first ? "**" + CN.getOrDefault(dk, dk) + "**（" + (day.has("total_calories") ? day.get("total_calories").asText() + "kcal" : "--") + "）" : "").append(" | ").append(mealName).append(" | ").append(foods).append(" | ").append(cal).append(" | ").append(prot).append(" | ").append(carb).append(" | ").append(fat).append(" |\n");
                            first = false;
                        }
                    }
                }
                md.append("\n"); return;
            }

            // daily_meals 结构 → 表格
            if (root.has("daily_meals") && root.get("daily_meals").isArray()) {
                md.append("| 日期 | 餐别 | 食物 | 热量 | 蛋白质 | 碳水 | 脂肪 |\n");
                md.append("|:------|:------|:-----|:-----|:-------|:-----|:-----|\n");
                for (JsonNode day : root.get("daily_meals")) {
                    String dayName = day.has("day") ? CN.getOrDefault(day.get("day").asText(), day.get("day").asText()) : "";
                    JsonNode meals = day.has("meals") ? day.get("meals") : null;
                    if (meals != null && meals.isArray()) {
                        boolean first = true;
                        for (JsonNode meal : meals) {
                            String mn = meal.has("meal") ? CN.getOrDefault(meal.get("meal").asText(), meal.get("meal").asText()) : "";
                            String foods = meal.has("foods") && meal.get("foods").isArray() ? arrJoin(meal.get("foods"), "、") : "";
                            md.append("| ").append(first ? "**" + dayName + "**" : "").append(" | ").append(mn).append(" | ").append(foods).append(" | ").append(meal.has("calories") ? meal.get("calories").asText() : "--").append(" | ").append(meal.has("protein") ? meal.get("protein").asText() : "--").append(" | ").append(meal.has("carbs") ? meal.get("carbs").asText() : "--").append(" | ").append(meal.has("fat") ? meal.get("fat").asText() : "--").append(" |\n");
                            first = false;
                        }
                    }
                }
                md.append("\n"); return;
            }

            // fallback: 列表
            writeKV(md, root); md.append("\n");
        } catch (Exception e) { md.append(jsonStr).append("\n\n"); }
    }

    // ===== 渲染：运动 → 表格 =====
    private static void renderWorkout(StringBuilder md, String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) { md.append("*（无数据）*\n\n"); return; }
        try {
            JsonNode root = om.readTree(jsonStr);
            JsonNode schedule = root.has("weekly_schedule") ? root.get("weekly_schedule") : root;
            if (schedule.isArray()) {
                md.append("| 日期 | 类型 | 训练内容 | 时长 | 强度 |\n");
                md.append("|:------|:-----|:---------|:-----|:-----|\n");
                for (JsonNode day : schedule) {
                    String dayName = day.has("day") ? CN.getOrDefault(day.get("day").asText(), day.get("day").asText()) : "";
                    String type = day.has("type") ? day.get("type").asText() : "";
                    String exercises = day.has("exercises") && day.get("exercises").isArray() ? arrJoin(day.get("exercises"), "；") : day.has("notes") ? day.get("notes").asText() : "";
                    String dur = day.has("duration_min") ? day.get("duration_min").asText() + "分钟" : day.has("duration") ? day.get("duration").asText() : "--";
                    String intensity = day.has("intensity") ? day.get("intensity").asText() : "--";
                    md.append("| **").append(dayName).append("** | ").append(type).append(" | ").append(exercises).append(" | ").append(dur).append(" | ").append(intensity).append(" |\n");
                }
                md.append("\n"); return;
            }
            writeKV(md, root); md.append("\n");
        } catch (Exception e) { md.append(jsonStr).append("\n\n"); }
    }

    // ===== 工具 =====
    private static String arrJoin(JsonNode arr, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(arr.get(i).asText());
        }
        return sb.toString();
    }
    private static void appendSnap(StringBuilder md, String s) {
        if (s == null) { md.append("*无*\n"); return; }
        try { JsonNode n = om.readTree(s); md.append(n.has("description") ? n.get("description").asText() : "已记录").append("  \n"); }
        catch (Exception e) { md.append("已记录  \n"); }
    }
    private static void appendDietS(StringBuilder md, String s) {
        if (s == null) { md.append("*无*\n"); return; }
        try { JsonNode n = om.readTree(s); List<String> p = new ArrayList<>();
            if (n.has("daily_calories")) p.add("**" + n.get("daily_calories").asText() + " kcal/天**");
            if (n.has("day1")) p.add("七天详细计划"); if (n.has("daily_meals")) p.add(n.get("daily_meals").size() + "天计划");
            if (p.isEmpty()) p.add("已生成"); md.append(String.join("，", p)).append("  \n"); }
        catch (Exception e) { md.append("已生成  \n"); }
    }
    private static void appendSportS(StringBuilder md, String s) {
        if (s == null) { md.append("*无*\n"); return; }
        try { JsonNode n = om.readTree(s); JsonNode sc = n.has("weekly_schedule") ? n.get("weekly_schedule") : n;
            md.append(sc.isArray() ? "共 **" + sc.size() + " 天**训练  \n" : "已生成  \n"); }
        catch (Exception e) { md.append("已生成  \n"); }
    }
    private static String gCN(Integer g) { return g == null ? "未知" : g == 1 ? "男" : g == 2 ? "女" : "其他"; }
    private static String aCN(String s) { return s == null ? "未知" : s.equalsIgnoreCase("LOW") ? "低活动量" : s.equalsIgnoreCase("MODERATE") ? "中等活动量" : s.equalsIgnoreCase("HIGH") ? "高活动量" : s; }
    private static String dCN(String s) { return s == null ? "未知" : s.equalsIgnoreCase("KETO") ? "生酮饮食" : s.equalsIgnoreCase("VEGAN") ? "纯素饮食" : s.equalsIgnoreCase("BALANCED") ? "均衡饮食" : s; }
    private static String gCN2(String s) { return s == null ? "未知" : s.equalsIgnoreCase("FAT_LOSS") ? "减重减脂" : s.equalsIgnoreCase("MUSCLE_GAIN") ? "增肌塑形" : s.equalsIgnoreCase("MAINTENANCE") ? "维持体重" : s; }
}
