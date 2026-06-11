package com.hnust.health.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.health.model.AiPlan;
import com.hnust.health.model.DailyCheckin;
import com.hnust.health.model.HealthProfile;
import com.hnust.health.model.WeightRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MarkdownGenerator {

    private static final ObjectMapper om = new ObjectMapper();
    private static final String[] WEEK_EN = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
    private static final String[] WEEK_CN = {"周一","周二","周三","周四","周五","周六","周日"};

    // ===== 单份报告 =====
    public static String generatePlanReport(AiPlan plan) {
        return generatePlanReport(plan, null);
    }

    public static String generatePlanReport(AiPlan plan, List<DailyCheckin> checkins) {
        StringBuilder md = new StringBuilder();
        String time = plan.getCreatedAt() != null
                ? plan.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "未知";
        md.append("# 🏥 AI 健康干预计划报告\n\n");
        md.append("> **周期起始**：").append(plan.getCycleStartDate()).append("  \n");
        md.append("> **生成时间**：").append(time).append("\n\n---\n\n");

        // 体重趋势
        md.append("## 📊 体重趋势分析\n\n");
        md.append(formatSnapshot(plan.getMemoryContextSnapshot())).append("\n\n");

        // 饮食表格
        md.append("## 🍽️ 饮食处方\n\n");
        md.append(formatDietTable(plan.getDietPlanJson())).append("\n\n");

        // 运动表格
        md.append("## 🏃 运动处方\n\n");
        md.append(formatWorkoutTable(plan.getWorkoutPlanJson())).append("\n\n");

        // 每日打卡摘要
        if (checkins != null && !checkins.isEmpty()) {
            md.append("## ✅ 本周打卡记录\n\n");
            md.append(formatCheckinSummary(checkins, plan.getCycleStartDate())).append("\n\n");
        }

        // AI推理 — 每行加 > 前缀，合并多余空行
        if (plan.getLlmReasoningChain() != null && !plan.getLlmReasoningChain().isBlank()) {
            md.append("## 🧠 AI 推理过程\n\n");
            String reasoning = plan.getLlmReasoningChain().replaceAll("\\n{3,}", "\n\n");
            for (String line : reasoning.split("\n")) {
                md.append("> ").append(line).append("\n");
            }
            md.append("\n");
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
            md.append("| **性别** | ").append(profile.getGender()==1?"男":profile.getGender()==2?"女":"其他").append(" |\n");
            md.append("| **身高** | ").append(profile.getHeightCm()).append(" cm |\n");
            md.append("| **建档体重** | ").append(profile.getBaselineWeight()).append(" kg |\n");
            md.append("| **活动水平** | ").append(labelAct(profile.getActivityLevel())).append(" |\n");
            md.append("| **饮食偏好** | ").append(labelDiet(profile.getDietPreference())).append(" |\n");
            md.append("| **健康目标** | ").append(labelGoal(profile.getHealthGoal())).append(" |\n\n");
        }
        if (weights != null && !weights.isEmpty()) {
            md.append("## ⚖️ 体重记录\n\n");
            md.append("| 日期 | 体重 (kg) | BMI |\n|:------|:----------|:----|\n");
            for (WeightRecord w : weights)
                md.append("| ").append(w.getRecordDate()).append(" | **").append(w.getCurrentWeight()).append("** | ").append(w.getCalculatedBmi()!=null?w.getCalculatedBmi():"--").append(" |\n");
            md.append("\n");
        }
        if (plans != null && !plans.isEmpty()) {
            md.append("## 🤖 AI 计划记录\n\n");
            for (AiPlan plan : plans) {
                String t = plan.getCreatedAt() != null ? plan.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) : "";
                md.append("### 📅 ").append(plan.getCycleStartDate()).append(t.isEmpty()?"":"（"+t+"）").append("\n\n");
                md.append("**📊 趋势**：").append(formatSnapshot(plan.getMemoryContextSnapshot())).append("  \n");
                md.append("**🍽️ 饮食**：").append(dietSummary(plan.getDietPlanJson())).append("  \n");
                md.append("**🏃 运动**：").append(workoutSummary(plan.getWorkoutPlanJson())).append("  \n");
                if (plan.getLlmReasoningChain() != null && !plan.getLlmReasoningChain().isBlank())
                    md.append("**🧠 分析**：").append(plan.getLlmReasoningChain()).append("\n");
                md.append("\n---\n\n");
            }
        }
        md.append("\n*📝 导出时间：").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("*\n");
        return md.toString();
    }

    // ===== 打卡摘要 → 表格 =====
    private static String formatCheckinSummary(List<DailyCheckin> checkins, LocalDate cycleStart) {
        if (checkins == null || checkins.isEmpty()) return "*（暂无打卡记录）*";
        LocalDate cycleEnd = cycleStart.plusDays(6);

        // 按日期分组
        Map<LocalDate, List<DailyCheckin>> byDate = new LinkedHashMap<>();
        for (DailyCheckin c : checkins) {
            LocalDate d = c.getRecordDate();
            if (!d.isBefore(cycleStart) && !d.isAfter(cycleEnd)) {
                byDate.computeIfAbsent(d, k -> new ArrayList<>()).add(c);
            }
        }
        if (byDate.isEmpty()) return "*（本周暂无打卡记录）*";

        StringBuilder t = new StringBuilder();
        t.append("| 日期 | 饮食 | 运动 | 饮水 | 健康分 |\n");
        t.append("|:------|:-----|:-----|:-----|:------|\n");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        String[] weekLabels = {"周一","周二","周三","周四","周五","周六","周日"};

        for (int i = 0; i < 7; i++) {
            LocalDate d = cycleStart.plusDays(i);
            List<DailyCheckin> dayList = byDate.getOrDefault(d, Collections.emptyList());

            Set<String> meals = new LinkedHashSet<>();
            List<String> exercises = new ArrayList<>();
            int waterCups = 0;
            int totalHealthScore = 0;

            for (DailyCheckin c : dayList) {
                switch (c.getCheckinType()) {
                    case "MEAL" -> {
                        String label = translateMealName(c.getMealType());
                        String desc = c.getFoodDesc() != null ? c.getFoodDesc() : "";
                        if (desc.length() > 12) desc = desc.substring(0, 12) + "…";
                        String scoreStr = "";
                        if (c.getHealthScore() != null && c.getHealthScore() != 0) {
                            scoreStr = (c.getHealthScore() > 0 ? "+" : "") + c.getHealthScore();
                        }
                        meals.add(label + (desc.isEmpty() ? "" : " " + desc) + (scoreStr.isEmpty() ? "" : "(" + scoreStr + ")"));
                        totalHealthScore += c.getHealthScore() != null ? c.getHealthScore() : 0;
                    }
                    case "EXERCISE" -> {
                        String ex = c.getExerciseType() != null ? c.getExerciseType() : "运动";
                        ex += c.getDurationMin() != null ? " " + c.getDurationMin() + "min" : "";
                        exercises.add(ex);
                    }
                    case "WATER" -> waterCups += c.getWaterCups() != null ? c.getWaterCups() : 0;
                }
            }

            String mealStr = meals.isEmpty() ? "--" : String.join("<br>", meals);
            String exStr = exercises.isEmpty() ? "--" : String.join("<br>", exercises);
            String waterStr = waterCups > 0 ? waterCups + "杯" : "--";
            String scoreStr = dayList.isEmpty() ? "--" : (totalHealthScore > 0 ? "+" : "") + totalHealthScore;

            t.append("| **").append(weekLabels[i]).append("** | ")
             .append(mealStr).append(" | ")
             .append(exStr).append(" | ")
             .append(waterStr).append(" | ")
             .append(scoreStr).append(" |\n");
        }
        return t.toString();
    }

    // ===== 趋势快照 → 文本 =====
    private static String formatSnapshot(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return "无数据";
        try {
            JsonNode n = om.readTree(jsonStr);
            return n.has("description") ? n.get("description").asText() : "已记录";
        } catch (Exception e) { return "已记录"; }
    }

    // ===== 饮食 → 完整表格 =====
    private static String formatDietTable(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return "*（暂无饮食处方）*";
        try {
            JsonNode root = om.readTree(jsonStr);
            StringBuilder t = new StringBuilder();

            // 热量标头 — 兼容 daily_calories_target 和 daily_calories
            if (root.has("daily_calories_target"))
                t.append("**每日热量目标**: ").append(root.get("daily_calories_target").asText()).append(" kcal  \n");
            else if (root.has("daily_calories"))
                t.append("**每日热量目标**: ").append(root.get("daily_calories").asText()).append(" kcal  \n");

            // 宏量营养素 — 兼容 macro_distribution(百分比) 和 macros(克数)
            JsonNode macroNode = root.has("macro_distribution") ? root.get("macro_distribution")
                    : root.has("macros") ? root.get("macros") : null;
            if (macroNode != null) {
                t.append("**宏量营养素**: ");
                if (macroNode.has("protein_pct") || macroNode.has("protein_g")) {
                    String p = macroNode.has("protein_pct") ? macroNode.get("protein_pct").asText() + "%" : macroNode.get("protein_g").asText() + "g";
                    t.append("蛋白质 ").append(p).append("  ");
                }
                if (macroNode.has("carbs_pct") || macroNode.has("carbs_g")) {
                    String c = macroNode.has("carbs_pct") ? macroNode.get("carbs_pct").asText() + "%" : macroNode.get("carbs_g").asText() + "g";
                    t.append("碳水 ").append(c).append("  ");
                }
                if (macroNode.has("fat_pct") || macroNode.has("fat_g")) {
                    String f = macroNode.has("fat_pct") ? macroNode.get("fat_pct").asText() + "%" : macroNode.get("fat_g").asText() + "g";
                    t.append("脂肪 ").append(f);
                }
                t.append("  \n");
            }
            t.append("\n");

            // 数组格式: [{day, breakfast/lunch/dinner/snack (direct)}, 或 {day, meals:[{meal,items}]}]
            if (root.isArray()) {
                // 检测是哪种数组格式
                JsonNode first = root.get(0);
                if (first.has("breakfast") || first.has("lunch") || first.has("dinner")) {
                    // 直接字段格式: breakfast/lunch/dinner/snack 作为day对象的直接属性
                    t.append("| 日期 | 早餐 | 午餐 | 晚餐 | 加餐 |\n");
                    t.append("|:------|:-----|:-----|:-----|:-----|\n");
                    for (JsonNode day : root) {
                        t.append("| **").append(day.has("day")?day.get("day").asText():"--").append("** | ")
                         .append(day.has("breakfast")?day.get("breakfast").asText():"--").append(" | ")
                         .append(day.has("lunch")?day.get("lunch").asText():"--").append(" | ")
                         .append(day.has("dinner")?day.get("dinner").asText():"--").append(" | ")
                         .append(day.has("snack")?day.get("snack").asText():"--").append(" |\n");
                    }
                    return t.toString();
                }
                // meals格式: 兼容 meals数组 或 meals对象(键=餐名), 字段名 meal/type/name
                t.append("| 日期 | 餐别 | 食物 |\n");
                t.append("|:------|:------|:-----|\n");
                for (JsonNode day : root) {
                    String dayName = day.has("day") ? day.get("day").asText() : "";
                    if (day.has("meals")) {
                        JsonNode meals = day.get("meals");
                        if (meals.isObject()) {
                            // meals是对象: 收集→翻译→排序→渲染
                            List<String[]> mealEntries = new ArrayList<>();
                            var fields = meals.fields();
                            while (fields.hasNext()) {
                                var entry = fields.next();
                                mealEntries.add(new String[]{translateMealName(entry.getKey()), entry.getValue().asText()});
                            }
                            mealEntries.sort((a, b) -> Integer.compare(mealOrder(a[0]), mealOrder(b[0])));
                            boolean firstObj = true;
                            for (String[] me : mealEntries) {
                                t.append("| ").append(firstObj ? "**"+dayName+"**" : "").append(" | ")
                                 .append(me[0]).append(" | ").append(me[1]).append(" |\n");
                                firstObj = false;
                            }
                        } else if (meals.isArray()) {
                            // meals是数组: 收集→排序→翻译→渲染
                            List<String[]> sorted = new ArrayList<>();
                            for (JsonNode meal : meals) {
                                String rawName = meal.has("meal") ? meal.get("meal").asText() :
                                                meal.has("type") ? meal.get("type").asText() :
                                                meal.has("name") ? meal.get("name").asText() :
                                                meal.has("time") ? meal.get("time").asText() : "--";
                                String foods = "--";
                                if (meal.has("items")) {
                                    foods = meal.get("items").isArray() ? arrJoin(meal.get("items")) : meal.get("items").asText();
                                } else if (meal.has("foods")) {
                                    foods = meal.get("foods").isArray() ? arrJoin(meal.get("foods")) : meal.get("foods").asText();
                                } else if (meal.has("content")) {
                                    foods = meal.get("content").asText();
                                }
                                sorted.add(new String[]{translateMealName(rawName), foods});
                            }
                            sorted.sort((a, b) -> Integer.compare(mealOrder(a[0]), mealOrder(b[0])));
                            boolean firstMeal = true;
                            for (String[] entry : sorted) {
                                t.append("| ").append(firstMeal ? "**"+dayName+"**" : "").append(" | ")
                                 .append(entry[0]).append(" | ").append(entry[1]).append(" |\n");
                                firstMeal = false;
                            }
                        }
                    } else {
                        String foods = day.has("items") ? (day.get("items").isArray()?arrJoin(day.get("items")):day.get("items").asText()) : "--";
                        t.append("| **").append(dayName).append("** | -- | ").append(foods).append(" |\n");
                    }
                }
                return t.toString();
            }

            // 星期键名格式: {"monday":{breakfast/lunch/dinner/snack}, "tuesday":...}
            if (root.has("monday") || root.has("Monday")) {
                String[] weekKeys = {"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};
                t.append("| 日期 | 早餐 | 午餐 | 晚餐 | 加餐 |\n");
                t.append("|:------|:-----|:-----|:-----|:-----|\n");
                for (int i = 0; i < 7; i++) {
                    JsonNode day = root.get(weekKeys[i]);
                    if (day == null) day = root.get(weekKeys[i].substring(0,1).toUpperCase() + weekKeys[i].substring(1));
                    if (day == null) continue;
                    t.append("| **").append(WEEK_CN[i]).append("** | ")
                     .append(day.has("breakfast") ? translateMealName(day.get("breakfast").asText().length() > 30 ? day.get("breakfast").asText().substring(0,30)+"…" : day.get("breakfast").asText()) : "--").append(" | ")
                     .append(day.has("lunch") ? day.get("lunch").asText() : "--").append(" | ")
                     .append(day.has("dinner") ? day.get("dinner").asText() : "--").append(" | ")
                     .append(day.has("snack") ? day.get("snack").asText() : "--").append(" |\n");
                }
                return t.toString();
            }

            // days 格式 → 表格
            if (root.has("days")) {
                JsonNode days = root.get("days");
                t.append("| 日期 | 早餐 | 午餐 | 晚餐 | 加餐 |\n");
                t.append("|:------|:-----|:-----|:-----|:-----|\n");
                for (int i = 0; i < 7; i++) {
                    JsonNode day = days.get(WEEK_EN[i]);
                    if (day == null) continue;
                    t.append("| **").append(WEEK_CN[i]).append("** | ")
                     .append(day.has("breakfast")?day.get("breakfast").asText():"--").append(" | ")
                     .append(day.has("lunch")?day.get("lunch").asText():"--").append(" | ")
                     .append(day.has("dinner")?day.get("dinner").asText():"--").append(" | ")
                     .append(day.has("snacks")?day.get("snacks").asText():"--").append(" |\n");
                }
                return t.toString();
            }

            // day1-day7 格式（两种子格式：meals数组 或 直接 breakfast/lunch/dinner 字段）
            for (int i = 1; i <= 7; i++) {
                if (root.has("day"+i)) {
                    JsonNode firstDay = root.get("day"+i);

                    // 子格式A：day对象直接包含 breakfast/lunch/dinner/snack 字符串字段
                    if (firstDay.has("breakfast") || firstDay.has("lunch") || firstDay.has("dinner")) {
                        t.append("| 日期 | 早餐 | 午餐 | 晚餐 | 加餐 |\n");
                        t.append("|:------|:-----|:-----|:-----|:-----|\n");
                        for (int j = 1; j <= 7; j++) {
                            JsonNode day = root.get("day"+j);
                            if (day == null) continue;
                            t.append("| **").append("第").append(j).append("天").append("** | ")
                             .append(day.has("breakfast") ? day.get("breakfast").asText() : "--").append(" | ")
                             .append(day.has("lunch") ? day.get("lunch").asText() : "--").append(" | ")
                             .append(day.has("dinner") ? day.get("dinner").asText() : "--").append(" | ")
                             .append(day.has("snack") ? day.get("snack").asText() : day.has("snacks") ? day.get("snacks").asText() : "--").append(" |\n");
                        }
                        return t.toString();
                    }

                    // 子格式B：day对象包含 meals 数组 [{meal, foods, calories, protein, carbs, fat}]
                    JsonNode meals = firstDay.has("meals") ? firstDay.get("meals") : null;
                    if (meals != null && meals.isArray()) {
                        t.append("| 日期 | 餐别 | 食物 | 热量 | 蛋白质 | 碳水 | 脂肪 |\n");
                        t.append("|:------|:------|:-----|:-----|:-------|:-----|:-----|\n");
                        for (int j = 1; j <= 7; j++) {
                            JsonNode day = root.get("day"+j);
                            if (day == null) continue;
                            JsonNode dayMeals = day.has("meals") ? day.get("meals") : null;
                            if (dayMeals != null && dayMeals.isArray()) {
                                boolean first = true;
                                for (JsonNode meal : dayMeals) {
                                    String mn = meal.has("meal") ? meal.get("meal").asText() : "";
                                    String foods = meal.has("foods") && meal.get("foods").isArray() ? arrJoin(meal.get("foods")) : "";
                                    t.append("| ").append(first ? "**第"+j+"天**("+ (day.has("total_calories")?day.get("total_calories").asText()+"kcal":"") +")" : "").append(" | ").append(mn).append(" | ").append(foods).append(" | ").append(meal.has("calories")?meal.get("calories").asText():"--").append(" | ").append(meal.has("protein")?meal.get("protein").asText():"--").append(" | ").append(meal.has("carbs")?meal.get("carbs").asText():"--").append(" | ").append(meal.has("fat")?meal.get("fat").asText():"--").append(" |\n");
                                    first = false;
                                }
                            }
                        }
                        return t.toString();
                    }
                }
            }

            return "*（暂无法解析饮食格式）*";
        } catch (Exception e) { return "*（饮食数据解析失败）*"; }
    }

    // ===== 运动 → 完整表格 =====
    private static String formatWorkoutTable(String jsonStr) {
        if (jsonStr == null || jsonStr.isBlank()) return "*（暂无运动处方）*";
        try {
            JsonNode root = om.readTree(jsonStr);
            StringBuilder t = new StringBuilder();

            if (root.has("weekly_overview"))
                t.append("**本周概览**: ").append(root.get("weekly_overview").asText()).append("  \n\n");

            // 数组格式: [{day, workout/activity/content/type, exercises/note}, ...]
            if (root.isArray()) {
                t.append("| 日期 | 类型 | 训练内容 |\n");
                t.append("|:------|:-----|:---------|\n");
                for (JsonNode day : root) {
                    String dayName = day.has("day") ? day.get("day").asText() : "--";
                    String type = day.has("type") ? day.get("type").asText() :
                                 day.has("workout") || day.has("activity") || day.has("activities") ? "运动" : "--";
                    String content = "--";
                    if (day.has("exercises") && day.get("exercises").isArray()) {
                        content = arrJoin(day.get("exercises"));
                    } else if (day.has("activities")) {
                        content = day.get("activities").isArray() ? arrJoin(day.get("activities")) : day.get("activities").asText();
                    } else if (day.has("workout")) {
                        content = day.get("workout").asText();
                    } else if (day.has("activity")) {
                        content = day.get("activity").asText();
                    } else if (day.has("content")) {
                        content = day.get("content").asText();
                    } else if (day.has("note")) {
                        content = day.get("note").asText();
                    }
                    if (day.has("duration")) content += " (" + day.get("duration").asText() + ")";
                    if (day.has("reps")) content += " · " + day.get("reps").asText();
                    t.append("| **").append(dayName).append("** | ").append(type).append(" | ").append(content).append(" |\n");
                }
                return t.toString();
            }

            // 星期键名格式: {"monday":"快走30分钟...", "tuesday":"..."}
            if (root.has("monday") || root.has("Monday")) {
                String[] weekKeys = {"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};
                t.append("| 日期 | 训练内容 |\n");
                t.append("|:------|:---------|\n");
                for (int i = 0; i < 7; i++) {
                    JsonNode day = root.get(weekKeys[i]);
                    if (day == null) day = root.get(weekKeys[i].substring(0,1).toUpperCase() + weekKeys[i].substring(1));
                    String content = day != null ? (day.isTextual() ? day.asText() : day.toString()) : "--";
                    t.append("| **").append(WEEK_CN[i]).append("** | ").append(content).append(" |\n");
                }
                return t.toString();
            }

            // days 格式
            if (root.has("days")) {
                JsonNode days = root.get("days");
                t.append("| 日期 | 类型 | 训练内容 |\n");
                t.append("|:------|:-----|:---------|\n");
                for (int i = 0; i < 7; i++) {
                    JsonNode day = days.get(WEEK_EN[i]);
                    if (day == null) continue;
                    t.append("| **").append(WEEK_CN[i]).append("** | ")
                     .append(day.has("type")?day.get("type").asText():"--").append(" | ")
                     .append(day.has("content")?day.get("content").asText():"--").append(" |\n");
                }
                return t.toString();
            }

            // weekly_schedule 格式
            if (root.has("weekly_schedule") && root.get("weekly_schedule").isArray()) {
                t.append("| 日期 | 类型 | 训练内容 | 时长 | 强度 |\n");
                t.append("|:------|:-----|:---------|:-----|:-----|\n");
                for (JsonNode day : root.get("weekly_schedule")) {
                    t.append("| **").append(day.has("day")?day.get("day").asText():"--").append("** | ")
                     .append(day.has("type")?day.get("type").asText():"--").append(" | ")
                     .append(day.has("exercises")&&day.get("exercises").isArray()?arrJoin(day.get("exercises")):day.has("notes")?day.get("notes").asText():"--").append(" | ")
                     .append(day.has("duration_min")?day.get("duration_min").asText()+"分钟":"--").append(" | ")
                     .append(day.has("intensity")?day.get("intensity").asText():"--").append(" |\n");
                }
                return t.toString();
            }

            // day1-day7 格式（每天一个纯文本字符串）
            for (int i = 1; i <= 7; i++) {
                if (root.has("day"+i) && root.get("day"+i).isTextual()) {
                    t.append("| 日期 | 训练内容 |\n");
                    t.append("|:------|:---------|\n");
                    for (int j = 1; j <= 7; j++) {
                        JsonNode day = root.get("day"+j);
                        if (day == null || !day.isTextual()) continue;
                        t.append("| **第").append(j).append("天** | ").append(day.asText()).append(" |\n");
                    }
                    return t.toString();
                }
            }

            return "*（暂无法解析运动格式）*";
        } catch (Exception e) { return "*（运动数据解析失败）*"; }
    }

    // ===== 摘要 =====
    private static String dietSummary(String s) {
        if (s == null) return "无";
        try { JsonNode n = om.readTree(s); if (n.has("daily_calories_target")) return "**"+n.get("daily_calories_target").asText()+" kcal/天** · 7天详细"; if (n.has("day1")) return "7天详细计划"; return "已生成"; } catch (Exception e) { return "已生成"; }
    }
    private static String workoutSummary(String s) {
        if (s == null) return "无";
        try { JsonNode n = om.readTree(s); if (n.has("weekly_overview")) return n.get("weekly_overview").asText().substring(0, Math.min(60, n.get("weekly_overview").asText().length()))+"..."; if (n.has("weekly_schedule")) return n.get("weekly_schedule").size()+" 天训练"; return "已生成"; } catch (Exception e) { return "已生成"; }
    }

    /** 翻译餐次名称为中文，并按早餐→午餐→晚餐→加餐的顺序给出排序权重 */
    private static int mealOrder(String name) {
        if (name == null) return 99;
        String n = name.toLowerCase();
        if (n.contains("breakfast") || n.contains("早餐") || n.contains("早")) return 1;
        if (n.contains("lunch") || n.contains("午餐") || n.contains("午")) return 2;
        if (n.contains("dinner") || n.contains("晚餐") || n.contains("晚")) return 3;
        if (n.contains("snack") || n.contains("加餐") || n.contains("小食")) return 4;
        return 99;
    }
    private static String translateMealName(String name) {
        if (name == null) return "--";
        String n = name.toLowerCase();
        if (n.contains("breakfast") || n.contains("早餐") || n.contains("早")) return "早餐";
        if (n.contains("lunch") || n.contains("午餐") || n.contains("午")) return "午餐";
        if (n.contains("dinner") || n.contains("晚餐") || n.contains("晚")) return "晚餐";
        if (n.contains("snack") || n.contains("加餐") || n.contains("小食")) return "加餐";
        return name; // keep original if can't translate
    }

    private static String arrJoin(JsonNode arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) { if (i > 0) sb.append("；"); sb.append(arr.get(i).asText()); }
        return sb.toString();
    }
    private static String labelAct(String s) { return s==null?"":s.equalsIgnoreCase("LOW")?"低活动量":s.equalsIgnoreCase("MODERATE")?"中等活动量":s.equalsIgnoreCase("HIGH")?"高活动量":s; }
    private static String labelDiet(String s) { return s==null?"":s.equalsIgnoreCase("KETO")?"生酮饮食":s.equalsIgnoreCase("VEGAN")?"纯素饮食":s.equalsIgnoreCase("BALANCED")?"均衡饮食":s; }
    private static String labelGoal(String s) { return s==null?"":s.equalsIgnoreCase("FAT_LOSS")?"减重减脂":s.equalsIgnoreCase("MUSCLE_GAIN")?"增肌塑形":s.equalsIgnoreCase("MAINTENANCE")?"维持体重":s; }
}
