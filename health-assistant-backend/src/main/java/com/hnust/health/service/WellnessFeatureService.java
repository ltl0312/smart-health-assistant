package com.hnust.health.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.health.config.DeepSeekConfig;
import com.hnust.health.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WellnessFeatureService {

    private static final List<String> MEAL_TYPES = List.of("BREAKFAST", "LUNCH", "DINNER", "SNACK");
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbcTemplate;
    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getGoal(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, user_id, goal_type, target_weight, daily_water_ml, weekly_exercise_times,
                       weekly_exercise_minutes, target_date, status, created_at, updated_at
                FROM health_goal WHERE user_id = ? AND status = 'ACTIVE'
                ORDER BY updated_at DESC LIMIT 1
                """, userId);
        if (!rows.isEmpty()) {
            return normalize(rows.get(0));
        }
        Map<String, Object> profile = getProfile(userId);
        Map<String, Object> goal = new LinkedHashMap<>();
        goal.put("id", null);
        goal.put("userId", userId);
        goal.put("goalType", stringValue(profile.get("healthGoal"), "MAINTENANCE"));
        goal.put("targetWeight", latestWeight(userId));
        goal.put("dailyWaterMl", 2000);
        goal.put("weeklyExerciseTimes", 3);
        goal.put("weeklyExerciseMinutes", 150);
        goal.put("targetDate", LocalDate.now().plusMonths(3).toString());
        goal.put("status", "DRAFT");
        return goal;
    }

    @Transactional
    public Map<String, Object> saveGoal(Long userId, Map<String, Object> request) {
        Long id = longValue(request.get("id"));
        String goalType = stringValue(request.get("goalType"), stringValue(request.get("goal_type"), "MAINTENANCE"));
        BigDecimal targetWeight = decimalValue(request.get("targetWeight"));
        int dailyWaterMl = intValue(request.get("dailyWaterMl"), 2000);
        int weeklyTimes = intValue(request.get("weeklyExerciseTimes"), 3);
        int weeklyMinutes = intValue(request.get("weeklyExerciseMinutes"), 150);
        LocalDate targetDate = dateValue(request.get("targetDate"), LocalDate.now().plusMonths(3));

        if (id == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO health_goal(user_id, goal_type, target_weight, daily_water_ml,
                        weekly_exercise_times, weekly_exercise_minutes, target_date, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, userId);
                ps.setString(2, goalType);
                ps.setBigDecimal(3, targetWeight);
                ps.setInt(4, dailyWaterMl);
                ps.setInt(5, weeklyTimes);
                ps.setInt(6, weeklyMinutes);
                ps.setDate(7, Date.valueOf(targetDate));
                return ps;
            }, keyHolder);
            id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        } else {
            jdbcTemplate.update("""
                    UPDATE health_goal SET goal_type = ?, target_weight = ?, daily_water_ml = ?,
                    weekly_exercise_times = ?, weekly_exercise_minutes = ?, target_date = ?, status = 'ACTIVE'
                    WHERE id = ? AND user_id = ?
                    """, goalType, targetWeight, dailyWaterMl, weeklyTimes, weeklyMinutes, Date.valueOf(targetDate), id, userId);
        }
        return getGoal(userId);
    }

    public Map<String, Object> reportSummary(Long userId, int days) {
        int safeDays = List.of(7, 30, 90).contains(days) ? days : 30;
        LocalDate start = LocalDate.now().minusDays(safeDays - 1L);
        List<Map<String, Object>> weights = jdbcTemplate.queryForList("""
                SELECT record_date, current_weight, calculated_bmi
                FROM weight_record WHERE user_id = ? AND record_date >= ?
                ORDER BY record_date ASC
                """, userId, Date.valueOf(start));
        List<Map<String, Object>> checkins = jdbcTemplate.queryForList("""
                SELECT record_date, checkin_type, SUM(COALESCE(water_cups, 0)) water_cups,
                       SUM(COALESCE(duration_min, 0)) duration_min, AVG(health_score) avg_score,
                       COUNT(*) total
                FROM daily_checkin WHERE user_id = ? AND record_date >= ?
                GROUP BY record_date, checkin_type ORDER BY record_date ASC
                """, userId, Date.valueOf(start));

        int totalWaterMl = checkins.stream()
                .filter(r -> "WATER".equalsIgnoreCase(String.valueOf(r.get("checkin_type"))))
                .mapToInt(r -> intValue(r.get("water_cups"), 0) * 250)
                .sum();
        int exerciseMinutes = checkins.stream()
                .filter(r -> "EXERCISE".equalsIgnoreCase(String.valueOf(r.get("checkin_type"))))
                .mapToInt(r -> intValue(r.get("duration_min"), 0))
                .sum();
        double mealScore = checkins.stream()
                .filter(r -> "MEAL".equalsIgnoreCase(String.valueOf(r.get("checkin_type"))))
                .mapToDouble(r -> decimalValue(r.get("avg_score"), BigDecimal.ZERO).doubleValue())
                .average().orElse(0);

        Map<String, Object> latest = weights.isEmpty() ? Map.of() : normalize(weights.get(weights.size() - 1));
        Map<String, Object> first = weights.isEmpty() ? Map.of() : normalize(weights.get(0));
        BigDecimal weightDelta = weights.size() < 2 ? BigDecimal.ZERO :
                decimalValue(latest.get("currentWeight"), BigDecimal.ZERO).subtract(decimalValue(first.get("currentWeight"), BigDecimal.ZERO));

        Map<String, Object> goal = getGoal(userId);
        Map<String, Object> progress = goalProgress(goal, latest);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", safeDays);
        result.put("startDate", start.toString());
        result.put("endDate", LocalDate.now().toString());
        result.put("goal", goal);
        result.put("latestWeight", latest.getOrDefault("currentWeight", null));
        result.put("latestBmi", latest.getOrDefault("calculatedBmi", null));
        result.put("weightDelta", weightDelta);
        result.put("totalWaterMl", totalWaterMl);
        result.put("avgWaterMl", safeDays == 0 ? 0 : totalWaterMl / safeDays);
        result.put("exerciseMinutes", exerciseMinutes);
        result.put("mealScore", Math.round(mealScore * 10.0) / 10.0);
        result.put("checkinDays", countDistinctCheckinDays(userId, start));
        result.put("healthScore", healthScore(latest, totalWaterMl, exerciseMinutes, safeDays));
        result.put("progress", progress);
        result.put("weightTrend", normalizeList(weights));
        result.put("checkinTrend", normalizeList(checkins));
        result.put("summary", buildSummary(safeDays, weightDelta, totalWaterMl, exerciseMinutes, mealScore));
        return result;
    }

    @Transactional
    public Map<String, Object> estimateMeal(Long userId, Map<String, Object> request) {
        String foodName = stringValue(request.get("foodName"), stringValue(request.get("food_name"), "均衡餐"));
        String amount = stringValue(request.get("amount"), "1份");
        MealEstimate estimate = calculateMealEstimate(foodName, amount);
        int calories = estimate.calories();
        BigDecimal protein = decimal(estimate.protein());
        BigDecimal carbs = decimal(estimate.carbs());
        BigDecimal fat = decimal(estimate.fat());
        int healthScore = estimate.healthScore();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO nutrition_estimate(user_id, food_name, amount, calories, protein_g, carbs_g, fat_g, health_score)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, foodName);
            ps.setString(3, amount);
            ps.setInt(4, calories);
            ps.setBigDecimal(5, protein);
            ps.setBigDecimal(6, carbs);
            ps.setBigDecimal(7, fat);
            ps.setInt(8, healthScore);
            return ps;
        }, keyHolder);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", Objects.requireNonNull(keyHolder.getKey()).longValue());
        result.put("foodName", foodName);
        result.put("amount", amount);
        result.put("calories", calories);
        result.put("proteinG", protein);
        result.put("carbsG", carbs);
        result.put("fatG", fat);
        result.put("healthScore", healthScore);
        result.put("source", "LOCAL_NUTRITION_DB");
        result.put("recognizedItems", estimate.items().size());
        result.put("breakdown", estimate.items());
        result.put("unmatchedKeywords", estimate.unmatched());
        result.put("confidence", estimate.confidence());
        result.put("tip", mealTip(estimate));
        return result;
    }

    @Transactional
    public Map<String, Object> estimateExercise(Long userId, Map<String, Object> request) {
        String exerciseType = stringValue(request.get("exerciseType"), stringValue(request.get("exercise_type"), "walking"));
        int duration = Math.max(1, intValue(request.get("durationMin"), intValue(request.get("duration_min"), 30)));
        BigDecimal weight = decimalValue(request.get("weightKg"), latestWeight(userId));
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            weight = BigDecimal.valueOf(65);
        }
        ExerciseRule rule = exerciseRule(exerciseType);
        double met = rule.met();
        int calories = (int) Math.round(met * 3.5 * weight.doubleValue() / 200 * duration);
        String intensity = met >= 7 ? "HIGH" : met >= 4 ? "MEDIUM" : "LOW";
        BigDecimal perMinute = BigDecimal.valueOf(calories / (double) duration).setScale(1, RoundingMode.HALF_UP);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        BigDecimal finalWeight = weight;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO exercise_estimate(user_id, exercise_type, duration_min, weight_kg, calories, intensity)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setString(2, exerciseType);
            ps.setInt(3, duration);
            ps.setBigDecimal(4, finalWeight);
            ps.setInt(5, calories);
            ps.setString(6, intensity);
            return ps;
        }, keyHolder);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", Objects.requireNonNull(keyHolder.getKey()).longValue());
        result.put("exerciseType", rule.name());
        result.put("inputType", exerciseType);
        result.put("durationMin", duration);
        result.put("weightKg", finalWeight);
        result.put("calories", calories);
        result.put("intensity", intensity);
        result.put("intensityLabel", intensityLabel(intensity));
        result.put("met", met);
        result.put("perMinuteCalories", perMinute);
        result.put("suggestion", rule.suggestion());
        result.put("source", "LOCAL_RULE");
        return result;
    }

    @Transactional
    public List<Map<String, Object>> alerts(Long userId) {
        refreshAlerts(userId);
        return normalizeList(jdbcTemplate.queryForList("""
                SELECT id, alert_type, title, message, severity, is_read, created_at, read_at
                FROM health_alert WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC LIMIT 30
                """, userId));
    }

    @Transactional
    public void markAlertRead(Long userId, Long id) {
        jdbcTemplate.update("UPDATE health_alert SET is_read = 1, read_at = NOW() WHERE id = ? AND user_id = ?", id, userId);
    }

    @Transactional
    public List<Map<String, Object>> reminders(Long userId) {
        ensureReminders(userId);
        return normalizeList(jdbcTemplate.queryForList("""
                SELECT id, reminder_type, reminder_key, title, message, group_type, action_view, due_at, is_done, created_at
                FROM user_reminder WHERE user_id = ? AND is_done = 0
                ORDER BY COALESCE(due_at, created_at) ASC LIMIT 20
                """, userId));
    }

    @Transactional
    public void markReminderDone(Long userId, Long id) {
        jdbcTemplate.update("""
                UPDATE user_reminder SET is_done = 1 WHERE id = ? AND user_id = ?
                """, id, userId);
    }

    @Transactional
    public Map<String, Object> latestPlan(Long userId) {
        Long planId = queryLong("""
                SELECT id FROM ai_plan
                WHERE user_id = ? AND COALESCE(status, 'APPROVED') = 'APPROVED'
                ORDER BY cycle_start_date DESC, id DESC LIMIT 1
                """, userId);
        if (planId == null) {
            planId = createFallbackPlan(userId, monday(LocalDate.now(APP_ZONE)), "APPROVED");
        }
        ensurePlanCalendar(userId, planId);
        return plan(userId, planId);
    }

    @Transactional
    public Map<String, Object> pendingPlan(Long userId) {
        Long planId = queryLong("""
                SELECT id FROM ai_plan
                WHERE user_id = ? AND status = 'PENDING_REVIEW'
                ORDER BY cycle_start_date DESC, id DESC LIMIT 1
                """, userId);
        if (planId == null) {
            return new LinkedHashMap<>();
        }
        ensurePlanCalendar(userId, planId);
        return plan(userId, planId);
    }

    @Transactional
    public Map<String, Object> approvePlan(Long userId, Long planId) {
        LocalDate cycleStart = queryDate("SELECT cycle_start_date FROM ai_plan WHERE id = ? AND user_id = ?", planId, userId);
        if (cycleStart == null) {
            throw new BusinessException(404, "计划不存在");
        }
        jdbcTemplate.update("""
                UPDATE ai_plan SET status = 'REJECTED'
                WHERE user_id = ? AND id <> ? AND cycle_start_date = ? AND COALESCE(status, 'APPROVED') = 'APPROVED'
                """, userId, planId, Date.valueOf(cycleStart));
        jdbcTemplate.update("""
                UPDATE ai_plan SET status = 'APPROVED' WHERE id = ? AND user_id = ?
                """, planId, userId);
        ensurePlanCalendar(userId, planId);
        return plan(userId, planId);
    }

    @Transactional
    public void rejectPlan(Long userId, Long planId) {
        int updated = jdbcTemplate.update("""
                UPDATE ai_plan SET status = 'REJECTED' WHERE id = ? AND user_id = ? AND status = 'PENDING_REVIEW'
                """, planId, userId);
        if (updated == 0) {
            throw new BusinessException(404, "没有可放弃的待审核计划");
        }
    }

    public Map<String, Object> plan(Long userId, Long planId) {
        ensurePlanCalendar(userId, planId);
        Map<String, Object> plan = normalize(queryOne("""
                SELECT id, user_id, cycle_start_date, memory_context_snapshot, diet_plan_json,
                       workout_plan_json, llm_reasoning_chain, COALESCE(status, 'APPROVED') status, created_at
                FROM ai_plan WHERE id = ? AND user_id = ?
                """, planId, userId));
        if (plan.isEmpty()) {
            throw new BusinessException(404, "计划不存在");
        }
        plan.put("calendar", planCalendar(userId, planId));
        plan.put("progress", planProgress(userId, planId));
        plan.put("summary", planSummary(userId, planId));
        return plan;
    }

    @Transactional
    public List<Map<String, Object>> planHistory(Long userId) {
        List<Map<String, Object>> rows = normalizeList(jdbcTemplate.queryForList("""
                SELECT id, user_id, cycle_start_date, COALESCE(status, 'APPROVED') status, created_at
                FROM ai_plan
                WHERE user_id = ?
                ORDER BY cycle_start_date DESC, id DESC
                LIMIT 20
                """, userId));
        for (Map<String, Object> row : rows) {
            Long planId = longValue(row.get("id"));
            try {
                ensurePlanCalendar(userId, planId);
                LocalDate normalizedStart = queryDate("SELECT cycle_start_date FROM ai_plan WHERE id = ? AND user_id = ?", planId, userId);
                row.put("cycleStartDate", normalizedStart == null ? row.get("cycleStartDate") : normalizedStart.toString());
                row.put("progress", planProgress(userId, planId));
                row.put("summary", planSummary(userId, planId));
            } catch (RuntimeException ex) {
                row.put("progress", Map.of("completionRate", 0, "streakDays", 0));
                row.put("summary", Map.of(
                        "mealItems", 0,
                        "exerciseMinutes", 0,
                        "dailyMealCalories", 0,
                        "focusTags", List.of("旧计划待修复")
                ));
                row.put("loadError", "旧计划内容解析失败，可重新生成或查看当前计划");
            }
        }
        return rows;
    }

    @Transactional
    public String exportPlanMarkdown(Long userId, Long planId) {
        Map<String, Object> plan = plan(userId, planId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> days = (List<Map<String, Object>>) plan.getOrDefault("calendar", List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> progress = (Map<String, Object>) plan.getOrDefault("progress", Map.of());
        StringBuilder md = new StringBuilder();
        md.append("# SmartHealth 周计划\n\n");
        md.append("- 周期开始：").append(value(plan.get("cycleStartDate"), "--")).append("\n");
        md.append("- 状态：").append(value(plan.get("status"), "APPROVED")).append("\n");
        md.append("- 完成率：").append(value(progress.get("completionRate"), "0")).append("%\n");
        md.append("- 连续执行：").append(value(progress.get("streakDays"), "0")).append(" 天\n\n");
        for (Map<String, Object> day : days) {
            md.append("## ").append(weekName(intValue(day.get("weekday"), 1))).append(" ")
                    .append(value(day.get("planDate"), "")).append("\n\n");
            md.append("重点：").append(value(day.get("focus"), "健康执行")).append("\n\n");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) day.getOrDefault("items", List.of());
            for (Map<String, Object> item : items) {
                md.append("- [").append("DONE".equals(item.get("status")) ? "x" : " ").append("] ")
                        .append(value(item.get("title"), "计划项"))
                        .append("：").append(value(item.get("description"), ""))
                        .append("（").append(value(item.get("status"), "PENDING")).append("）\n");
            }
            md.append("\n");
        }
        return md.toString();
    }

    @Transactional
    public List<Map<String, Object>> planCalendar(Long userId, Long planId) {
        ensurePlanCalendar(userId, planId);
        List<Map<String, Object>> days = normalizeList(jdbcTemplate.queryForList("""
                SELECT id, plan_id, plan_date, weekday, focus FROM plan_day
                WHERE plan_id = ? AND user_id = ? ORDER BY plan_date ASC
                """, planId, userId));
        for (Map<String, Object> day : days) {
            Long dayId = longValue(day.get("id"));
            day.put("items", normalizeList(jdbcTemplate.queryForList("""
                    SELECT i.id, i.item_type, i.meal_type, i.title, i.description, i.calories,
                           i.duration_min, i.intensity, i.sort_order, COALESCE(e.status, 'PENDING') status,
                           e.checked_at
                    FROM plan_item i
                    LEFT JOIN plan_execution e ON e.plan_item_id = i.id AND e.user_id = ?
                    WHERE i.plan_day_id = ? ORDER BY i.sort_order ASC, i.id ASC
                    """, userId, dayId)));
        }
        return days;
    }

    @Transactional
    public Map<String, Object> checkinPlanItem(Long userId, Long planId, Long itemId, Map<String, Object> request) {
        String status = stringValue(request.get("status"), "DONE").toUpperCase(Locale.ROOT);
        if (!List.of("DONE", "SKIPPED", "PENDING").contains(status)) {
            status = "DONE";
        }
        String note = stringValue(request.get("note"), null);
        if ("PENDING".equals(status)) {
            jdbcTemplate.update("""
                    DELETE FROM plan_execution WHERE plan_item_id = ? AND plan_id = ? AND user_id = ?
                    """, itemId, planId, userId);
            return planProgress(userId, planId);
        }
        jdbcTemplate.update("""
                INSERT INTO plan_execution(plan_item_id, plan_id, user_id, status, note, checked_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE status = VALUES(status), note = VALUES(note), checked_at = NOW()
                """, itemId, planId, userId, status, note);
        return planProgress(userId, planId);
    }

    public Map<String, Object> planProgress(Long userId, Long planId) {
        int total = intValue(queryScalar("SELECT COUNT(*) FROM plan_item WHERE plan_id = ? AND user_id = ?", planId, userId), 0);
        int done = intValue(queryScalar("""
                SELECT COUNT(*) FROM plan_execution WHERE plan_id = ? AND user_id = ? AND status = 'DONE'
                """, planId, userId), 0);
        int skipped = intValue(queryScalar("""
                SELECT COUNT(*) FROM plan_execution WHERE plan_id = ? AND user_id = ? AND status = 'SKIPPED'
                """, planId, userId), 0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", planId);
        result.put("totalItems", total);
        result.put("doneItems", done);
        result.put("skippedItems", skipped);
        result.put("completionRate", total == 0 ? 0 : Math.round(done * 1000.0 / total) / 10.0);
        result.put("streakDays", executionStreak(userId, planId));
        return result;
    }

    private Map<String, Object> planSummary(Long userId, Long planId) {
        Map<String, Object> row = normalize(queryOne("""
                SELECT COUNT(*) total_items,
                       SUM(CASE WHEN item_type = 'MEAL' THEN 1 ELSE 0 END) meal_items,
                       SUM(CASE WHEN item_type = 'EXERCISE' THEN 1 ELSE 0 END) exercise_items,
                       COALESCE(SUM(CASE WHEN item_type = 'MEAL' THEN calories ELSE 0 END), 0) total_calories,
                       COALESCE(SUM(CASE WHEN item_type = 'EXERCISE' THEN duration_min ELSE 0 END), 0) exercise_minutes
                FROM plan_item
                WHERE plan_id = ? AND user_id = ?
                """, planId, userId));
        LocalDate start = queryDate("SELECT MIN(plan_date) FROM plan_day WHERE plan_id = ? AND user_id = ?", planId, userId);
        LocalDate end = queryDate("SELECT MAX(plan_date) FROM plan_day WHERE plan_id = ? AND user_id = ?", planId, userId);
        int mealItems = intValue(row.get("mealItems"), 0);
        int totalCalories = intValue(row.get("totalCalories"), 0);
        List<String> focusTags = jdbcTemplate.queryForList("""
                SELECT focus FROM (
                    SELECT focus, MIN(plan_date) first_date
                    FROM plan_day
                    WHERE plan_id = ? AND user_id = ? AND focus IS NOT NULL AND focus <> ''
                    GROUP BY focus
                    ORDER BY first_date ASC
                    LIMIT 4
                ) t
                """, String.class, planId, userId);
        Map<String, Object> summary = new LinkedHashMap<>(row);
        summary.put("startDate", start == null ? null : start.toString());
        summary.put("endDate", end == null ? null : end.toString());
        summary.put("avgMealCalories", mealItems == 0 ? 0 : Math.round(totalCalories * 10.0 / mealItems) / 10.0);
        summary.put("dailyMealCalories", start == null || end == null ? 0 : Math.round(totalCalories * 10.0 / (end.toEpochDay() - start.toEpochDay() + 1)) / 10.0);
        summary.put("focusTags", focusTags);
        return summary;
    }

    public Map<String, Object> weeklyReview(Long userId, String week) {
        String weekCode = week == null || week.isBlank() ? currentWeekCode() : week;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, week_code, summary, good_points, risks, next_suggestions, source, created_at, updated_at
                FROM weekly_review WHERE user_id = ? AND week_code = ?
                """, userId, weekCode);
        if (!rows.isEmpty()) {
            return normalize(rows.get(0));
        }
        return generateWeeklyReview(userId, weekCode);
    }

    @Transactional
    public Map<String, Object> generateWeeklyReview(Long userId, String week) {
        String weekCode = week == null || week.isBlank() ? currentWeekCode() : week;
        Map<String, Object> summary = reportSummary(userId, 7);
        String good = intValue(summary.get("exerciseMinutes"), 0) >= 120 ? "运动执行较稳定。" : "已经开始记录健康数据，这是形成习惯的第一步。";
        String risks = alerts(userId).stream()
                .filter(a -> !"1".equals(String.valueOf(a.get("isRead"))))
                .findFirst()
                .map(a -> String.valueOf(a.get("message")))
                .orElse("暂无明显风险，继续保持规律记录。");
        String next = "下周优先保证饮水和每周运动分钟数，饮食继续采用高蛋白、低油炸的组合。";
        String text = "本周健康分 " + summary.get("healthScore") + "，运动 " + summary.get("exerciseMinutes")
                + " 分钟，平均饮水约 " + summary.get("avgWaterMl") + " ml/天。";
        jdbcTemplate.update("""
                INSERT INTO weekly_review(user_id, week_code, summary, good_points, risks, next_suggestions, source)
                VALUES (?, ?, ?, ?, ?, ?, 'LOCAL_RULE')
                ON DUPLICATE KEY UPDATE summary = VALUES(summary), good_points = VALUES(good_points),
                risks = VALUES(risks), next_suggestions = VALUES(next_suggestions), updated_at = NOW()
                """, userId, weekCode, text, good, risks, next);
        return weeklyReview(userId, weekCode);
    }

    public String printableReportHtml(Long userId, int days) {
        Map<String, Object> report = reportSummary(userId, days);
        Map<String, Object> profile = getProfile(userId);
        Map<String, Object> goal = getGoal(userId);
        return """
                <!doctype html><html><head><meta charset="utf-8"><title>SmartHealth Report</title>
                <style>
                body{font-family:Arial,'Microsoft YaHei',sans-serif;margin:32px;color:#172033;line-height:1.6}
                h1{font-size:28px;margin:0 0 8px}.muted{color:#64748b}.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:24px 0}
                .card{border:1px solid #dbe3ef;border-radius:10px;padding:14px}.num{font-size:24px;font-weight:700}
                table{width:100%%;border-collapse:collapse;margin-top:18px}td,th{border-bottom:1px solid #e2e8f0;padding:8px;text-align:left}
                @media print{button{display:none}body{margin:18mm}}
                </style></head><body>
                <button onclick="window.print()">打印 / 保存 PDF</button>
                <h1>SmartHealth 健康报告</h1><div class="muted">%s 至 %s，%s 天</div>
                <div class="grid">
                  <div class="card"><div class="muted">当前体重</div><div class="num">%s kg</div></div>
                  <div class="card"><div class="muted">当前 BMI</div><div class="num">%s</div></div>
                  <div class="card"><div class="muted">运动分钟</div><div class="num">%s</div></div>
                  <div class="card"><div class="muted">健康分</div><div class="num">%s</div></div>
                </div>
                <h2>档案与目标</h2>
                <p>目标：%s；目标体重：%s kg；每日饮水：%s ml；每周运动：%s 分钟。</p>
                <h2>周期总结</h2><p>%s</p>
                <h2>建议</h2><p>保持每日至少一次打卡。若体重或 BMI 出现明显异常，请优先调整饮食结构并咨询专业人士。</p>
                </body></html>
                """.formatted(
                report.get("startDate"), report.get("endDate"), report.get("days"),
                value(report.get("latestWeight"), "--"), value(report.get("latestBmi"), "--"),
                value(report.get("exerciseMinutes"), "0"), value(report.get("healthScore"), "0"),
                value(goal.get("goalType"), value(profile.get("healthGoal"), "MAINTENANCE")),
                value(goal.get("targetWeight"), "--"), value(goal.get("dailyWaterMl"), "2000"),
                value(goal.get("weeklyExerciseMinutes"), "150"), value(report.get("summary"), "")
        );
    }

    public List<Map<String, Object>> articles(Long userId, String category) {
        String goal = stringValue(getGoal(userId).get("goalType"), null);
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT a.id, a.category_code, c.name category_name, a.title, a.summary, a.cover_url,
                       a.target_goal, a.status, a.view_count,
                       GREATEST(1, CEIL(CHAR_LENGTH(a.content) / 450)) reading_minutes,
                       (SELECT COUNT(*) FROM health_article x WHERE x.status = 'PUBLISHED' AND x.category_code = a.category_code) category_count,
                       a.created_at, a.updated_at
                FROM health_article a
                LEFT JOIN article_category c ON c.code = a.category_code
                WHERE a.status = 'PUBLISHED'
                """);
        if (category != null && !category.isBlank()) {
            sql.append(" AND a.category_code = ?");
            args.add(category);
        }
        sql.append(" ORDER BY CASE WHEN a.target_goal = ? THEN 0 ELSE 1 END, a.updated_at DESC LIMIT 80");
        args.add(goal);
        return normalizeList(jdbcTemplate.queryForList(sql.toString(), args.toArray()));
    }

    @Transactional
    public Map<String, Object> article(Long userId, Long id) {
        int updated = jdbcTemplate.update("""
                UPDATE health_article SET view_count = view_count + 1 WHERE id = ? AND status = 'PUBLISHED'
                """, id);
        if (updated == 0) {
            throw new BusinessException(404, "文章不存在或未发布");
        }
        jdbcTemplate.update("INSERT INTO article_view_log(article_id, user_id) VALUES (?, ?)", id, userId);
        Map<String, Object> article = normalize(queryOne("""
                SELECT a.id, a.category_code, c.name category_name, a.title, a.summary, a.content, a.cover_url,
                       a.target_goal, a.status, a.view_count,
                       GREATEST(1, CEIL(CHAR_LENGTH(a.content) / 450)) reading_minutes,
                       a.created_at, a.updated_at
                FROM health_article a
                LEFT JOIN article_category c ON c.code = a.category_code
                WHERE a.id = ? AND a.status = 'PUBLISHED'
                """, id));
        return article;
    }

    public Map<String, Object> adminDashboard() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", queryScalar("SELECT COUNT(*) FROM sys_user"));
        result.put("activeUsers", queryScalar("SELECT COUNT(*) FROM sys_user WHERE status = 1"));
        result.put("todayCheckins", queryScalar("SELECT COUNT(*) FROM daily_checkin WHERE record_date = CURDATE()"));
        result.put("aiCalls", queryScalar("SELECT COUNT(*) FROM ai_call_log"));
        result.put("plans", queryScalar("SELECT COUNT(*) FROM ai_plan"));
        result.put("articles", queryScalar("SELECT COUNT(*) FROM health_article"));
        result.put("mysql", "UP");
        result.put("redis", "UNKNOWN");
        result.put("aiConfigured", deepSeekConfig.getApiKey() != null && !deepSeekConfig.getApiKey().isBlank()
                && !deepSeekConfig.getApiKey().startsWith("replace"));
        return result;
    }

    public List<Map<String, Object>> adminUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return normalizeList(jdbcTemplate.queryForList("""
                    SELECT id, username, email, status, avatar_url, phone, nickname, bio, role, created_at, updated_at
                    FROM sys_user ORDER BY created_at DESC LIMIT 100
                    """));
        }
        String like = "%" + keyword + "%";
        return normalizeList(jdbcTemplate.queryForList("""
                SELECT id, username, email, status, avatar_url, phone, nickname, bio, role, created_at, updated_at
                FROM sys_user WHERE username LIKE ? OR email LIKE ? OR nickname LIKE ?
                ORDER BY created_at DESC LIMIT 100
                """, like, like, like));
    }

    public List<Map<String, Object>> adminArticles(String keyword, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT id, category_code, title, summary, content, cover_url, target_goal, status, view_count, created_at, updated_at
                FROM health_article WHERE 1 = 1
                """);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            args.add(status);
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (title LIKE ? OR summary LIKE ? OR content LIKE ?)");
            String like = "%" + keyword + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY updated_at DESC LIMIT 100");
        return normalizeList(jdbcTemplate.queryForList(sql.toString(), args.toArray()));
    }

    public Map<String, Object> adminArticle(Long id) {
        return normalize(queryOne("""
                SELECT id, category_code, title, summary, content, cover_url, target_goal, status, view_count, created_at, updated_at
                FROM health_article WHERE id = ?
                """, id));
    }

    public void updateUserStatus(Long adminId, Long id, Map<String, Object> request) {
        if (Objects.equals(adminId, id)) {
            throw new BusinessException(400, "不能禁用当前登录的管理员账号");
        }
        int status = intValue(request.get("status"), 1);
        jdbcTemplate.update("UPDATE sys_user SET status = ? WHERE id = ?", status, id);
    }

    public Map<String, Object> aiStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean configured = deepSeekConfig.getApiKey() != null && !deepSeekConfig.getApiKey().isBlank()
                && !deepSeekConfig.getApiKey().startsWith("replace");
        result.put("configured", configured);
        result.put("baseUrl", deepSeekConfig.getBaseUrl());
        result.put("model", deepSeekConfig.getModel());
        result.put("mode", configured ? "REMOTE_WITH_LOCAL_FALLBACK" : "LOCAL_FALLBACK_ONLY");
        result.put("recentCalls", normalizeList(jdbcTemplate.queryForList("""
                SELECT feature, status, message, created_at FROM ai_call_log ORDER BY created_at DESC LIMIT 20
                """)));
        return result;
    }

    @Transactional
    public Map<String, Object> saveArticle(Long adminId, Long id, Map<String, Object> request) {
        String category = stringValue(request.get("categoryCode"), "DIET");
        String title = stringValue(request.get("title"), "未命名文章");
        String summary = stringValue(request.get("summary"), "");
        String content = stringValue(request.get("content"), "");
        String coverUrl = stringValue(request.get("coverUrl"), null);
        String targetGoal = stringValue(request.get("targetGoal"), null);
        String status = stringValue(request.get("status"), "DRAFT");
        if (id == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO health_article(category_code, title, summary, content, cover_url, target_goal, status, author_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, category);
                ps.setString(2, title);
                ps.setString(3, summary);
                ps.setString(4, content);
                ps.setString(5, coverUrl);
                ps.setString(6, targetGoal);
                ps.setString(7, status);
                ps.setLong(8, adminId);
                return ps;
            }, keyHolder);
            id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        } else {
            jdbcTemplate.update("""
                    UPDATE health_article SET category_code = ?, title = ?, summary = ?, content = ?,
                    cover_url = ?, target_goal = ?, status = ? WHERE id = ?
                    """, category, title, summary, content, coverUrl, targetGoal, status, id);
        }
        return normalize(queryOne("SELECT * FROM health_article WHERE id = ?", id));
    }

    public void deleteArticle(Long id) {
        jdbcTemplate.update("UPDATE health_article SET status = 'OFFLINE' WHERE id = ?", id);
    }

    private Map<String, Object> getProfile(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT user_id, age, gender, height_cm, baseline_weight, activity_level, diet_preference, health_goal
                FROM health_profile WHERE user_id = ?
                """, userId);
        return rows.isEmpty() ? Map.of("healthGoal", "MAINTENANCE") : normalize(rows.get(0));
    }

    private BigDecimal latestWeight(Long userId) {
        Object value = queryScalar("""
                SELECT current_weight FROM weight_record WHERE user_id = ? ORDER BY record_date DESC LIMIT 1
                """, userId);
        return decimalValue(value, null);
    }

    private Map<String, Object> goalProgress(Map<String, Object> goal, Map<String, Object> latest) {
        BigDecimal target = decimalValue(goal.get("targetWeight"), null);
        BigDecimal current = decimalValue(latest.get("currentWeight"), null);
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("targetWeight", target);
        progress.put("currentWeight", current);
        if (target == null || current == null) {
            progress.put("weightGap", null);
            progress.put("percent", 0);
        } else {
            progress.put("weightGap", current.subtract(target));
            progress.put("percent", Math.max(0, Math.min(100, 100 - Math.abs(current.subtract(target).doubleValue()) * 10)));
        }
        return progress;
    }

    private int healthScore(Map<String, Object> latest, int totalWaterMl, int exerciseMinutes, int days) {
        BigDecimal bmi = decimalValue(latest.get("calculatedBmi"), null);
        int score = 75;
        if (bmi != null) {
            score -= (int) Math.round(Math.abs(bmi.doubleValue() - 22) * 4);
        }
        score += Math.min(12, exerciseMinutes / Math.max(1, days));
        score += Math.min(10, totalWaterMl / Math.max(1, days) / 250);
        return Math.max(0, Math.min(100, score));
    }

    private String buildSummary(int days, BigDecimal weightDelta, int waterMl, int exerciseMinutes, double mealScore) {
        return "最近 " + days + " 天体重变化 " + weightDelta + " kg，累计饮水 " + waterMl
                + " ml，运动 " + exerciseMinutes + " 分钟，饮食评分 " + Math.round(mealScore * 10.0) / 10.0 + "。";
    }

    private int countDistinctCheckinDays(Long userId, LocalDate start) {
        return intValue(queryScalar("""
                SELECT COUNT(DISTINCT record_date) FROM daily_checkin WHERE user_id = ? AND record_date >= ?
                """, userId, Date.valueOf(start)), 0);
    }

    private void refreshAlerts(Long userId) {
        Map<String, Object> report = reportSummary(userId, 7);
        BigDecimal bmi = decimalValue(report.get("latestBmi"), null);
        if (bmi != null && bmi.doubleValue() >= 28) {
            upsertAlert(userId, "BMI", "BMI 偏高", "当前 BMI 为 " + bmi + "，建议控制总热量并保持每周运动。", "WARN", "BMI_HIGH");
        } else if (bmi != null && bmi.doubleValue() < 18.5) {
            upsertAlert(userId, "BMI", "BMI 偏低", "当前 BMI 为 " + bmi + "，建议增加优质蛋白和力量训练。", "WARN", "BMI_LOW");
        }
        if (Math.abs(decimalValue(report.get("weightDelta"), BigDecimal.ZERO).doubleValue()) >= 2.5) {
            upsertAlert(userId, "WEIGHT", "体重波动较大", "最近 7 天体重变化超过 2.5kg，请关注饮食、睡眠和测量时间。", "WARN", "WEIGHT_SWING_7D");
        }
        if (intValue(report.get("checkinDays"), 0) <= 4) {
            upsertAlert(userId, "CHECKIN", "打卡频率偏低", "最近 7 天打卡天数不足，建议每天至少完成一次记录。", "INFO", "CHECKIN_LOW_7D");
        }
        Map<String, Object> goal = getGoal(userId);
        if (intValue(report.get("avgWaterMl"), 0) < intValue(goal.get("dailyWaterMl"), 2000) * 0.7) {
            upsertAlert(userId, "WATER", "饮水不足", "最近平均饮水低于目标的 70%，可以分早中晚三段完成。", "INFO", "WATER_LOW_7D");
        }
        if (intValue(report.get("exerciseMinutes"), 0) < intValue(goal.get("weeklyExerciseMinutes"), 150) * 0.6) {
            upsertAlert(userId, "EXERCISE", "运动量偏低", "本周运动分钟数低于目标，建议安排 2-3 次轻中强度运动。", "INFO", "EXERCISE_LOW_7D");
        }
    }

    private void upsertAlert(Long userId, String type, String title, String message, String severity, String key) {
        jdbcTemplate.update("""
                INSERT INTO health_alert(user_id, alert_type, title, message, severity, alert_key)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE title = VALUES(title), message = VALUES(message), severity = VALUES(severity), created_at = NOW()
                """, userId, type, title, message, severity, key);
    }

    private void ensureReminders(Long userId) {
        jdbcTemplate.update("""
                UPDATE user_reminder SET is_done = 1
                WHERE user_id = ? AND reminder_key IS NULL AND is_done = 0
                """, userId);

        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDate weekStart = monday(today);
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        Map<String, Object> goal = getGoal(userId);
        if (goal == null || goal.isEmpty()) {
            log.warn("ensureReminders: goal is empty for userId={}, skipping", userId);
            return;
        }

        // --- 今日饮水 ---
        try {
            int waterTarget = intValue(goal.get("dailyWaterMl"), 2000);
            int waterMl = intValue(queryScalar("""
                    SELECT COALESCE(SUM(water_cups), 0) * 250 FROM daily_checkin
                    WHERE user_id = ? AND record_date = ? AND checkin_type = 'WATER'
                    """, userId, Date.valueOf(today)), 0);
            if (waterMl < waterTarget) {
                upsertReminder(userId, "WATER", "WATER_" + today, "TODAY", "checkin",
                        "今日饮水还差 " + Math.max(0, waterTarget - waterMl) + " ml",
                        "根据你的每日饮水目标生成。现在已记录 " + waterMl + " ml，分两到三次补齐更容易坚持。",
                        now.plusHours(2));
            }
        } catch (Exception e) {
            log.error("ensureReminders WATER failed for userId={}", userId, e);
        }

        // --- 今日打卡 ---
        try {
            int todayCheckins = intValue(queryScalar("""
                    SELECT COUNT(*) FROM daily_checkin WHERE user_id = ? AND record_date = ?
                    """, userId, Date.valueOf(today)), 0);
            if (todayCheckins == 0) {
                upsertReminder(userId, "CHECKIN", "CHECKIN_" + today, "TODAY", "checkin",
                        "今天还没有打卡",
                        "这条提醒会根据当天记录动态生成。先补一条饮食、饮水或运动记录即可。",
                        now.withHour(Math.min(21, Math.max(now.getHour() + 1, 18))).withMinute(0));
            }
        } catch (Exception e) {
            log.error("ensureReminders CHECKIN failed for userId={}", userId, e);
        }

        // --- 本周体重 ---
        try {
            int weightRecordsThisWeek = intValue(queryScalar("""
                    SELECT COUNT(*) FROM weight_record
                    WHERE user_id = ? AND record_date BETWEEN ? AND ?
                    """, userId, Date.valueOf(weekStart), Date.valueOf(weekStart.plusDays(6))), 0);
            if (weightRecordsThisWeek == 0) {
                upsertReminder(userId, "WEIGHT", "WEIGHT_" + weekStart, "TODAY", "dashboard",
                        "本周体重未记录",
                        "本周只需要记录一次体重。记录后看板会显示'本周体重已记录'。",
                        now.plusHours(3));
            }
        } catch (Exception e) {
            log.error("ensureReminders WEIGHT failed for userId={}", userId, e);
        }

        // --- 补录体重 ---
        try {
            for (int i = 1; i <= 2; i++) {
                LocalDate start = weekStart.minusWeeks(i);
                int count = intValue(queryScalar("""
                        SELECT COUNT(*) FROM weight_record
                        WHERE user_id = ? AND record_date BETWEEN ? AND ?
                        """, userId, Date.valueOf(start), Date.valueOf(start.plusDays(6))), 0);
                if (count == 0) {
                    upsertReminder(userId, "WEIGHT_BACKFILL", "WEIGHT_BACKFILL_" + start, "TODAY", "dashboard",
                            "可补录 " + start.getMonthValue() + "." + start.getDayOfMonth() + " 这一周体重",
                            "系统允许补录前两周体重。补齐后趋势图和周计划复盘会更准确。",
                            now.plusHours(4));
                }
            }
        } catch (Exception e) {
            log.error("ensureReminders WEIGHT_BACKFILL failed for userId={}", userId, e);
        }

        // --- 运动进度 ---
        try {
            int weeklyTarget = intValue(goal.get("weeklyExerciseMinutes"), 150);
            int elapsedDays = Math.max(1, today.getDayOfWeek().getValue());
            int expectedMinutes = (int) Math.ceil(weeklyTarget * elapsedDays / 7.0);
            int exerciseMinutes = intValue(queryScalar("""
                    SELECT COALESCE(SUM(duration_min), 0) FROM daily_checkin
                    WHERE user_id = ? AND checkin_type = 'EXERCISE' AND record_date BETWEEN ? AND ?
                    """, userId, Date.valueOf(weekStart), Date.valueOf(today)), 0);
            if (exerciseMinutes < expectedMinutes) {
                upsertReminder(userId, "EXERCISE", "EXERCISE_" + weekStart, "TODAY", "checkin",
                        "本周运动进度偏慢",
                        "当前已记录 " + exerciseMinutes + " 分钟，按你的目标本周此时建议达到约 " + expectedMinutes + " 分钟。",
                        now.plusHours(5));
            }
        } catch (Exception e) {
            log.error("ensureReminders EXERCISE failed for userId={}", userId, e);
        }

        // --- 周计划待执行项 ---
        try {
            Long approvedPlanId = queryLong("""
                    SELECT id FROM ai_plan WHERE user_id = ? AND COALESCE(status, 'APPROVED') = 'APPROVED'
                    ORDER BY cycle_start_date DESC, id DESC LIMIT 1
                    """, userId);
            if (approvedPlanId != null) {
                int pendingItems = intValue(queryScalar("""
                        SELECT COUNT(*) FROM plan_item i
                        JOIN plan_day d ON d.id = i.plan_day_id
                        LEFT JOIN plan_execution e ON e.plan_item_id = i.id AND e.user_id = ?
                        WHERE i.plan_id = ? AND d.plan_date <= ? AND COALESCE(e.status, 'PENDING') = 'PENDING'
                        """, userId, approvedPlanId, Date.valueOf(today)), 0);
                if (pendingItems > 0) {
                    upsertReminder(userId, "PLAN", "PLAN_PENDING_" + approvedPlanId + "_" + today, "PLAN", "plan",
                            "周计划有 " + pendingItems + " 项待执行",
                            "这条提醒来自已应用周计划。完成、跳过或调整状态后，进度会同步更新。",
                            now.plusHours(1));
                }
            }
        } catch (Exception e) {
            log.error("ensureReminders PLAN_PENDING failed for userId={}", userId, e);
        }

        // --- 待审核计划 ---
        try {
            Long pendingPlanId = queryLong("""
                    SELECT id FROM ai_plan WHERE user_id = ? AND status = 'PENDING_REVIEW'
                    ORDER BY cycle_start_date DESC, id DESC LIMIT 1
                    """, userId);
            if (pendingPlanId != null) {
                upsertReminder(userId, "PLAN_REVIEW", "PLAN_REVIEW_" + pendingPlanId, "PLAN", "plan",
                        "有一份 AI 周计划等待审核",
                        "AI 生成的计划不会自动应用。请进入周计划页确认后再应用。",
                        now.plusMinutes(30));
            } else {
                // 没有待审核计划时，清理旧 PLAN_REVIEW 提醒
                jdbcTemplate.update("""
                        UPDATE user_reminder SET is_done = 1
                        WHERE user_id = ? AND reminder_type = 'PLAN_REVIEW' AND is_done = 0
                        """, userId);
            }
        } catch (Exception e) {
            log.error("ensureReminders PLAN_REVIEW failed for userId={}", userId, e);
        }

        // --- 习惯建议 (RISK) ---
        refreshRiskReminders(userId, today, weekStart, now, goal);
    }

    private void refreshRiskReminders(Long userId, LocalDate today, LocalDate weekStart,
                                      LocalDateTime now, Map<String, Object> goal) {
        // 饮水习惯：7天内饮水达标（>=目标）的天数 < 3
        try {
            int waterTarget = intValue(goal.get("dailyWaterMl"), 2000);
            int waterOkDays = intValue(queryScalar("""
                    SELECT COUNT(*) FROM (
                      SELECT record_date, COALESCE(SUM(water_cups), 0) * 250 AS total
                      FROM daily_checkin WHERE user_id = ? AND checkin_type = 'WATER'
                        AND record_date >= ?
                      GROUP BY record_date HAVING total >= ?
                    ) t
                    """, userId, Date.valueOf(today.minusDays(6)), waterTarget), 0);
            if (waterOkDays < 3) {
                upsertReminder(userId, "RISK_WATER", "RISK_WATER_" + weekStart, "RISK", "dashboard",
                        "饮水习惯待养成",
                        "最近7天仅 " + waterOkDays + " 天达到饮水目标（" + waterTarget + " ml）。建议每天分早中晚三段完成。",
                        now.plusHours(6));
            }
        } catch (Exception e) {
            log.error("refreshRiskReminders WATER failed for userId={}", userId, e);
        }

        // 运动规律：7天内运动天数 < 2
        try {
            int exerciseDays = intValue(queryScalar("""
                    SELECT COUNT(DISTINCT record_date) FROM daily_checkin
                    WHERE user_id = ? AND checkin_type = 'EXERCISE' AND record_date >= ?
                    """, userId, Date.valueOf(today.minusDays(6))), 0);
            if (exerciseDays < 2) {
                upsertReminder(userId, "RISK_EXERCISE", "RISK_EXERCISE_" + weekStart, "RISK", "dashboard",
                        "运动规律性不足",
                        "最近7天仅运动 " + exerciseDays + " 天，建议每周至少安排2-3次，哪怕20分钟快走也有帮助。",
                        now.plusHours(6));
            }
        } catch (Exception e) {
            log.error("refreshRiskReminders EXERCISE failed for userId={}", userId, e);
        }

        // 饮食结构：7天内平均评分 < 50
        try {
            Double avgMealScore = (Double) queryScalar("""
                    SELECT AVG(health_score) FROM daily_checkin
                    WHERE user_id = ? AND checkin_type = 'MEAL' AND record_date >= ?
                    """, userId, Date.valueOf(today.minusDays(6)));
            if (avgMealScore != null && avgMealScore < 50) {
                upsertReminder(userId, "RISK_MEAL", "RISK_MEAL_" + weekStart, "RISK", "dashboard",
                        "饮食结构可优化",
                        "最近7天饮食评分均值 " + Math.round(avgMealScore) + " 分。建议适当增加蛋白质和蔬菜占比。",
                        now.plusHours(6));
            }
        } catch (Exception e) {
            log.error("refreshRiskReminders MEAL failed for userId={}", userId, e);
        }

        // 打卡质量：连续打卡 >= 5 天但饮水或运动某天为 0
        try {
            int streak = intValue(queryScalar("""
                    SELECT COUNT(DISTINCT record_date) FROM daily_checkin
                    WHERE user_id = ? AND record_date >= ?
                    """, userId, Date.valueOf(today.minusDays(6))), 0);
            if (streak >= 5) {
                int waterZeroDays = intValue(queryScalar("""
                        SELECT COUNT(*) FROM (
                          SELECT record_date FROM daily_checkin
                          WHERE user_id = ? AND record_date >= ?
                          GROUP BY record_date HAVING COALESCE(SUM(water_cups), 0) = 0
                        ) t
                        """, userId, Date.valueOf(today.minusDays(6))), 0);
                int exerciseZeroDays = intValue(queryScalar("""
                        SELECT COUNT(*) FROM (
                          SELECT record_date FROM daily_checkin
                          WHERE user_id = ? AND checkin_type = 'EXERCISE' AND record_date >= ?
                        ) t2
                        """, userId, Date.valueOf(today.minusDays(6))), 0);
                if (waterZeroDays >= 3 || exerciseZeroDays >= 5) {
                    upsertReminder(userId, "RISK_CHECKIN_QUALITY", "RISK_CHECKIN_QUALITY_" + weekStart, "RISK", "dashboard",
                            "打卡连续但质量可提升",
                            "最近" + streak + "天连续打卡，但饮水或运动覆盖不足，建议在打卡中增加实际行为记录。",
                            now.plusHours(6));
                }
            }
        } catch (Exception e) {
            log.error("refreshRiskReminders CHECKIN_QUALITY failed for userId={}", userId, e);
        }

        // 体重缺口：有历史体重但连续3周未记录
        try {
            Long anyWeight = queryLong("""
                    SELECT COUNT(*) FROM weight_record WHERE user_id = ?
                    """, userId);
            if (anyWeight != null && anyWeight > 0) {
                int missingWeeks = 0;
                for (int i = 0; i < 3; i++) {
                    LocalDate ws = weekStart.minusWeeks(i);
                    int c = intValue(queryScalar("""
                            SELECT COUNT(*) FROM weight_record
                            WHERE user_id = ? AND record_date BETWEEN ? AND ?
                            """, userId, Date.valueOf(ws), Date.valueOf(ws.plusDays(6))), 0);
                    if (c == 0) missingWeeks++;
                }
                if (missingWeeks >= 3) {
                    upsertReminder(userId, "RISK_WEIGHT_GAP", "RISK_WEIGHT_GAP_" + weekStart, "RISK", "dashboard",
                            "体重记录有缺口",
                            "最近3周均未记录体重。系统允许补录前两周数据，补齐后趋势分析更准确。",
                            now.plusHours(6));
                }
            }
        } catch (Exception e) {
            log.error("refreshRiskReminders WEIGHT_GAP failed for userId={}", userId, e);
        }
    }

    private void upsertReminder(Long userId, String type, String key, String groupType, String actionView,
                                String title, String message, LocalDateTime dueAt) {
        jdbcTemplate.update("""
                INSERT INTO user_reminder(user_id, reminder_type, reminder_key, title, message, group_type, action_view, due_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE title = VALUES(title), message = VALUES(message),
                group_type = VALUES(group_type), action_view = VALUES(action_view), due_at = VALUES(due_at)
                """, userId, type, key, title, message, groupType, actionView, dueAt);
    }

    private String weekName(int weekday) {
        return switch (weekday) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "计划日";
        };
    }

    private Long createFallbackPlan(Long userId, LocalDate start, String status) {
        LocalDate weekStart = monday(start == null ? LocalDate.now(APP_ZONE) : start);
        String goalType = stringValue(getGoal(userId).get("goalType"), "MAINTENANCE");
        List<PlanDayTemplate> localDays = localPlanTemplates(goalType, userId + weekStart.toEpochDay());
        String dietJson = localDietTemplateJson(localDays);
        String workoutJson = localWorkoutTemplateJson(localDays);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO ai_plan(user_id, cycle_start_date, memory_context_snapshot, diet_plan_json, workout_plan_json, llm_reasoning_chain, status)
                    VALUES (?, ?, '{}', ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId);
            ps.setDate(2, Date.valueOf(weekStart));
            ps.setString(3, dietJson);
            ps.setString(4, workoutJson);
            ps.setString(5, "AI 不可用或尚未生成计划，使用本地规则模板。");
            ps.setString(6, status);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private void ensurePlanCalendar(Long userId, Long planId) {
        Map<String, Object> planRow = normalize(queryOne("""
                SELECT cycle_start_date, diet_plan_json, workout_plan_json
                FROM ai_plan WHERE id = ? AND user_id = ?
                """, planId, userId));
        if (planRow.isEmpty()) {
            return;
        }
        LocalDate start = dateValue(planRow.get("cycleStartDate"), null);
        if (start == null) {
            start = monday(LocalDate.now(APP_ZONE));
        }
        LocalDate weekStart = monday(start);
        if (!weekStart.equals(start)) {
            jdbcTemplate.update("UPDATE ai_plan SET cycle_start_date = ? WHERE id = ? AND user_id = ?",
                    Date.valueOf(weekStart), planId, userId);
        }

        int exists = intValue(queryScalar("SELECT COUNT(*) FROM plan_day WHERE plan_id = ? AND user_id = ?", planId, userId), 0);
        if (exists > 0 && !calendarNeedsRebuild(userId, planId, weekStart)) {
            return;
        }
        List<PlanDayTemplate> days;
        try {
            days = buildPlanTemplates(
                    userId,
                    planId,
                    weekStart,
                    stringValue(planRow.get("dietPlanJson"), "{}"),
                    stringValue(planRow.get("workoutPlanJson"), "{}")
            );
        } catch (RuntimeException ex) {
            String goalType = stringValue(getGoal(userId).get("goalType"), "MAINTENANCE");
            days = localPlanTemplates(goalType, planId + weekStart.toEpochDay());
        }
        if (exists > 0) {
            deletePlanCalendar(userId, planId);
        }
        for (int i = 0; i < 7; i++) {
            PlanDayTemplate day = days.get(i);
            LocalDate date = weekStart.plusDays(i);
            Long dayId = insertPlanDay(planId, userId, date, i + 1, day.focus());
            int order = 1;
            for (MealTemplate meal : day.meals()) {
                insertPlanItem(dayId, planId, userId, "MEAL", meal.mealType(), meal.title(),
                        meal.description(), meal.calories(), null, null, order++);
            }
            WorkoutTemplate workout = day.workout();
            if (workout != null) {
                insertPlanItem(dayId, planId, userId, "EXERCISE", null, workout.title(),
                        workout.description(), null, workout.durationMin(), workout.intensity(), order);
            }
        }
    }

    private boolean calendarNeedsRebuild(Long userId, Long planId, LocalDate weekStart) {
        List<Map<String, Object>> days = jdbcTemplate.queryForList("""
                SELECT plan_date, weekday FROM plan_day
                WHERE plan_id = ? AND user_id = ? ORDER BY plan_date ASC
                """, planId, userId);
        if (days.size() != 7) {
            return true;
        }
        for (int i = 0; i < 7; i++) {
            LocalDate expected = weekStart.plusDays(i);
            LocalDate actual = dateValue(days.get(i).get("plan_date"), null);
            int weekday = intValue(days.get(i).get("weekday"), 0);
            if (!expected.equals(actual) || weekday != i + 1) {
                return true;
            }
        }
        int itemCount = intValue(queryScalar("SELECT COUNT(*) FROM plan_item WHERE plan_id = ? AND user_id = ?", planId, userId), 0);
        if (itemCount < 35) {
            return true;
        }
        int legacyCount = intValue(queryScalar("""
                SELECT COUNT(*) FROM plan_item
                WHERE plan_id = ? AND user_id = ?
                  AND title IN ('燕麦蛋白早餐', '鸡胸糙米午餐', '鱼肉蔬菜午餐', '清爽高纤晚餐', '坚果酸奶加餐')
                """, planId, userId), 0);
        return legacyCount >= 18;
    }

    private void deletePlanCalendar(Long userId, Long planId) {
        jdbcTemplate.update("DELETE FROM plan_execution WHERE plan_id = ? AND user_id = ?", planId, userId);
        jdbcTemplate.update("DELETE FROM plan_item WHERE plan_id = ? AND user_id = ?", planId, userId);
        jdbcTemplate.update("DELETE FROM plan_day WHERE plan_id = ? AND user_id = ?", planId, userId);
    }

    private Long insertPlanDay(Long planId, Long userId, LocalDate date, int weekday, String focus) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO plan_day(plan_id, user_id, plan_date, weekday, focus) VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, planId);
            ps.setLong(2, userId);
            ps.setDate(3, Date.valueOf(date));
            ps.setInt(4, weekday);
            ps.setString(5, focus);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private void insertPlanItem(Long dayId, Long planId, Long userId, String type, String mealType, String title,
                                String description, Integer calories, Integer duration, String intensity, int order) {
        jdbcTemplate.update("""
                INSERT INTO plan_item(plan_day_id, plan_id, user_id, item_type, meal_type, title, description, calories, duration_min, intensity, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, dayId, planId, userId, type, mealType, title, description, calories, duration, intensity, order);
    }

    private List<PlanDayTemplate> buildPlanTemplates(Long userId, Long planId, LocalDate weekStart,
                                                     String dietJson, String workoutJson) {
        String goalType = stringValue(getGoal(userId).get("goalType"), "MAINTENANCE");
        List<PlanDayTemplate> fallback = localPlanTemplates(goalType, planId + weekStart.toEpochDay());
        boolean localTemplate = isLocalTemplate(dietJson) || isLocalTemplate(workoutJson);
        List<PlanDayTemplate> parsedDiet = localTemplate ? List.of() : parseDietPlan(dietJson);
        List<WorkoutTemplate> parsedWorkouts = localTemplate ? List.of() : parseWorkoutPlan(workoutJson);

        List<PlanDayTemplate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            PlanDayTemplate base = fallback.get(i);
            PlanDayTemplate dietDay = i < parsedDiet.size() ? parsedDiet.get(i) : null;
            String focus = stringValue(dietDay == null ? null : dietDay.focus(), base.focus());
            List<MealTemplate> meals = mergeMeals(base.meals(), dietDay == null ? List.of() : dietDay.meals());
            WorkoutTemplate workout = i < parsedWorkouts.size() ? parsedWorkouts.get(i) : base.workout();
            days.add(new PlanDayTemplate(focus, meals, workout));
        }
        return days;
    }

    private boolean isLocalTemplate(String json) {
        JsonNode root = readJson(json);
        return root != null && "LOCAL_TEMPLATE".equalsIgnoreCase(text(root, "source", ""));
    }

    private List<MealTemplate> mergeMeals(List<MealTemplate> fallback, List<MealTemplate> parsed) {
        Map<String, MealTemplate> byType = new LinkedHashMap<>();
        for (MealTemplate meal : fallback) {
            byType.put(meal.mealType(), meal);
        }
        for (int i = 0; i < parsed.size(); i++) {
            MealTemplate meal = parsed.get(i);
            String type = stringValue(meal.mealType(), i < MEAL_TYPES.size() ? MEAL_TYPES.get(i) : "SNACK");
            byType.put(type, new MealTemplate(type, meal.title(), meal.description(), meal.calories()));
        }
        return MEAL_TYPES.stream().map(byType::get).filter(Objects::nonNull).toList();
    }

    private List<PlanDayTemplate> parseDietPlan(String json) {
        JsonNode root = readJson(json);
        if (root == null || root.isMissingNode() || root.isNull()) {
            return List.of();
        }
        List<PlanDayTemplate> days = new ArrayList<>();
        JsonNode daysNode = root.get("days");
        if (daysNode != null && daysNode.isArray()) {
            for (JsonNode dayNode : daysNode) {
                days.add(new PlanDayTemplate(text(dayNode, "focus", "本日执行"), parseMealList(dayNode.get("meals")), null));
            }
        }
        if (days.isEmpty()) {
            for (int i = 1; i <= 7; i++) {
                JsonNode dayNode = root.get("day" + i);
                if (dayNode != null && dayNode.isObject()) {
                    days.add(new PlanDayTemplate(text(dayNode, "focus", "本日执行"), parseMealList(dayNode.get("meals")), null));
                }
            }
        }
        return days.stream().limit(7).toList();
    }

    private List<MealTemplate> parseMealList(JsonNode mealsNode) {
        if (mealsNode == null || !mealsNode.isArray()) {
            return List.of();
        }
        List<MealTemplate> meals = new ArrayList<>();
        int index = 0;
        for (JsonNode mealNode : mealsNode) {
            if (mealNode.isTextual()) {
                String type = index < MEAL_TYPES.size() ? MEAL_TYPES.get(index) : "SNACK";
                String title = mealNode.asText();
                meals.add(new MealTemplate(type, title, "AI 建议餐食：按七分饱执行，保留蔬菜和优质蛋白。", estimateCalories(title, 320 + index * 80)));
            } else if (mealNode.isObject()) {
                String type = mealType(text(mealNode, "meal", index < MEAL_TYPES.size() ? MEAL_TYPES.get(index) : "SNACK"));
                List<String> foods = stringList(mealNode.get("foods"));
                String title = foods.isEmpty() ? text(mealNode, "title", text(mealNode, "name", mealName(type) + "安排")) : String.join(" + ", foods);
                String description = foods.isEmpty()
                        ? "AI 建议：" + title
                        : "食材：" + String.join("、", foods) + "。按计划份量执行，优先少油烹调。";
                int calories = intValue(nodeValue(mealNode, "calories"), estimateCalories(title, 320 + index * 80));
                meals.add(new MealTemplate(type, title, description, calories));
            }
            index++;
        }
        return meals;
    }

    private List<WorkoutTemplate> parseWorkoutPlan(String json) {
        JsonNode root = readJson(json);
        if (root == null || root.isMissingNode() || root.isNull()) {
            return List.of();
        }
        JsonNode schedule = root.get("weekly_schedule");
        if (schedule == null) {
            schedule = root.get("schedule");
        }
        if (schedule == null) {
            schedule = root.get("items");
        }
        if (schedule == null || !schedule.isArray()) {
            return List.of();
        }
        List<WorkoutTemplate> workouts = new ArrayList<>();
        for (JsonNode node : schedule) {
            if (!node.isObject()) {
                continue;
            }
            String title = text(node, "type", text(node, "title", "综合训练"));
            List<String> exercises = stringList(node.get("exercises"));
            String detail = exercises.isEmpty() ? title : String.join("、", exercises);
            int duration = intValue(firstPresent(node, "duration_min", "durationMin", "minutes"), 30);
            String intensity = normalizeIntensity(text(node, "intensity", "MEDIUM"));
            workouts.add(new WorkoutTemplate(title, detail + "，约 " + duration + " 分钟，结束后做 5 分钟放松。", duration, intensity));
        }
        return workouts.stream().limit(7).toList();
    }

    private List<PlanDayTemplate> localPlanTemplates(String goalType, long seed) {
        int offset = Math.floorMod((int) seed, 7);
        List<PlanDayTemplate> source = switch (goalType) {
            case "FAT_LOSS" -> fatLossTemplates();
            case "MUSCLE_GAIN" -> muscleGainTemplates();
            default -> maintenanceTemplates();
        };
        List<PlanDayTemplate> rotated = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            PlanDayTemplate day = source.get((i + offset) % source.size());
            rotated.add(new PlanDayTemplate(day.focus(), day.meals(), day.workout()));
        }
        return rotated;
    }

    private List<PlanDayTemplate> fatLossTemplates() {
        return List.of(
                planDay("轻盈启动", workout("快走", "快走 35 分钟，保持微喘但可以说完整句子。", 35, "MEDIUM"),
                        meal("BREAKFAST", "希腊酸奶燕麦碗", "无糖酸奶 180g、燕麦 35g、蓝莓一小把。", 330),
                        meal("LUNCH", "鸡胸糙米蔬菜盘", "鸡胸肉 140g、糙米饭 120g、西兰花 250g。", 520),
                        meal("DINNER", "番茄豆腐鱼片汤", "鱼片 120g、豆腐 150g、番茄和绿叶菜，少油。", 430),
                        meal("SNACK", "苹果配杏仁", "苹果 1 个，杏仁 10g，避免额外甜饮。", 160)),
                planDay("稳定血糖", workout("力量循环", "深蹲、俯卧撑、划船动作各 3 组，动作慢而稳。", 40, "MEDIUM"),
                        meal("BREAKFAST", "鸡蛋全麦早餐", "水煮蛋 2 个、全麦吐司 1 片、黄瓜番茄。", 360),
                        meal("LUNCH", "牛肉杂粮午餐", "瘦牛肉 100g、杂粮饭 120g、凉拌蔬菜。", 540),
                        meal("DINNER", "虾仁菌菇蔬菜", "虾仁 140g、菌菇 150g、绿叶菜 250g。", 390),
                        meal("SNACK", "无糖豆浆", "无糖豆浆 250ml，训练日可加半根香蕉。", 130)),
                planDay("有氧燃脂", workout("慢跑间歇", "慢跑 5 分钟加快走 3 分钟循环 4 次。", 32, "MEDIUM"),
                        meal("BREAKFAST", "牛奶燕麦鸡蛋", "牛奶 250ml、燕麦 30g、水煮蛋 1 个。", 350),
                        meal("LUNCH", "鱼肉红薯餐", "清蒸鱼 150g、红薯 180g、蔬菜 250g。", 500),
                        meal("DINNER", "鸡肉生菜卷", "鸡肉 120g、生菜、彩椒，主食减半。", 380),
                        meal("SNACK", "酸奶奇亚籽", "无糖酸奶 150g，搭配少量奇亚籽。", 150)),
                planDay("低压恢复", workout("瑜伽拉伸", "髋部、胸椎、肩颈拉伸，配合腹式呼吸。", 25, "LOW"),
                        meal("BREAKFAST", "豆腐蔬菜蛋饼", "鸡蛋 1 个、豆腐 100g、蔬菜碎。", 320),
                        meal("LUNCH", "鸡肉荞麦面", "鸡肉 120g、荞麦面 150g、青菜两拳。", 510),
                        meal("DINNER", "冬瓜虾仁汤", "虾仁 120g、冬瓜、菌菇，少盐。", 350),
                        meal("SNACK", "小番茄奶酪", "小番茄一盒，低脂奶酪 1 片。", 140)),
                planDay("力量保肌", workout("下肢力量", "腿举或深蹲、臀桥、箭步蹲各 3-4 组。", 45, "MEDIUM"),
                        meal("BREAKFAST", "高蛋白三明治", "全麦面包、鸡蛋、低脂奶酪和生菜。", 390),
                        meal("LUNCH", "虾仁米饭便当", "虾仁 150g、米饭 130g、蔬菜 250g。", 520),
                        meal("DINNER", "牛肉蔬菜锅", "瘦牛肉 100g、豆腐 100g、蔬菜 300g。", 430),
                        meal("SNACK", "蛋白牛奶", "牛奶 250ml，晚间饥饿时优先选择。", 135)),
                planDay("周末控制", workout("骑行", "轻中等强度骑行，注意补水和膝盖反馈。", 45, "MEDIUM"),
                        meal("BREAKFAST", "香蕉燕麦杯", "香蕉半根、燕麦 35g、酸奶 150g。", 340),
                        meal("LUNCH", "外卖减脂公式", "优先点清蒸/炖煮蛋白，米饭半份，蔬菜加量。", 560),
                        meal("DINNER", "豆腐青菜鸡蛋汤", "豆腐、青菜、鸡蛋，少油少盐。", 360),
                        meal("SNACK", "坚果限量", "坚果 15g，提前分装避免超量。", 90)),
                planDay("复盘调整", workout("散步恢复", "饭后散步 15 分钟两次，睡前轻拉伸。", 30, "LOW"),
                        meal("BREAKFAST", "玉米鸡蛋早餐", "玉米半根、鸡蛋 1 个、牛奶 200ml。", 330),
                        meal("LUNCH", "鱼肉蔬菜饭", "鱼肉 140g、米饭 120g、蔬菜两拳。", 500),
                        meal("DINNER", "清爽沙拉加蛋白", "沙拉菜、鸡胸或虾仁，酱料单独放。", 370),
                        meal("SNACK", "低糖水果", "选择莓果、苹果或猕猴桃，控制一拳大小。", 120))
        );
    }

    private List<PlanDayTemplate> muscleGainTemplates() {
        return List.of(
                planDay("高蛋白启动", workout("上肢力量", "卧推/俯卧撑、划船、肩推、核心各 3-4 组。", 50, "MEDIUM"),
                        meal("BREAKFAST", "燕麦牛奶鸡蛋", "燕麦 50g、牛奶 300ml、鸡蛋 2 个。", 520),
                        meal("LUNCH", "牛肉米饭力量餐", "瘦牛肉 150g、米饭 220g、蔬菜 200g。", 760),
                        meal("DINNER", "鱼肉土豆蔬菜", "鱼肉 180g、土豆 200g、蔬菜一大份。", 650),
                        meal("SNACK", "酸奶香蕉坚果", "酸奶 200g、香蕉 1 根、坚果 15g。", 330)),
                planDay("下肢训练", workout("下肢力量", "深蹲、罗马尼亚硬拉、臀桥、提踵各 4 组。", 55, "HIGH"),
                        meal("BREAKFAST", "全麦鸡蛋三明治", "全麦面包 2 片、鸡蛋 2 个、低脂奶酪。", 560),
                        meal("LUNCH", "鸡肉意面", "鸡胸肉 160g、意面 220g、番茄蔬菜酱。", 790),
                        meal("DINNER", "虾仁豆腐饭", "虾仁 150g、豆腐 150g、米饭 180g。", 680),
                        meal("SNACK", "训练后牛奶", "牛奶 300ml，加一份水果补糖原。", 260)),
                planDay("主动恢复", workout("灵活性训练", "髋、踝、胸椎灵活性和 20 分钟轻快走。", 35, "LOW"),
                        meal("BREAKFAST", "酸奶麦片水果", "酸奶 250g、麦片 45g、莓果。", 470),
                        meal("LUNCH", "鱼肉杂粮饭", "鱼肉 170g、杂粮饭 200g、蔬菜。", 720),
                        meal("DINNER", "鸡肉菌菇面", "鸡肉 150g、面条 200g、菌菇青菜。", 680),
                        meal("SNACK", "花生酱吐司", "全麦吐司 1 片，薄涂花生酱。", 220)),
                planDay("渐进加量", workout("推拉训练", "推、拉动作交替，最后加 2 组核心抗旋。", 50, "MEDIUM"),
                        meal("BREAKFAST", "牛奶玉米鸡蛋", "牛奶 300ml、玉米 1 根、鸡蛋 2 个。", 520),
                        meal("LUNCH", "鸡腿饭升级版", "去皮鸡腿 180g、米饭 220g、蔬菜。", 780),
                        meal("DINNER", "牛肉红薯蔬菜", "牛肉 150g、红薯 250g、蔬菜。", 700),
                        meal("SNACK", "豆浆加餐", "无糖豆浆 300ml，搭配水果。", 240)),
                planDay("容量训练", workout("全身训练", "深蹲、卧推、划船、硬拉变式，控制总量。", 60, "HIGH"),
                        meal("BREAKFAST", "蛋白燕麦碗", "燕麦 55g、牛奶、鸡蛋和少量坚果。", 600),
                        meal("LUNCH", "三文鱼米饭餐", "鱼肉 180g、米饭 220g、蔬菜。", 800),
                        meal("DINNER", "鸡胸土豆沙拉", "鸡胸肉 180g、土豆 250g、橄榄油少量。", 710),
                        meal("SNACK", "酸奶水果杯", "酸奶 250g、香蕉半根。", 260)),
                planDay("轻有氧", workout("骑行恢复", "轻中等骑行，不追求速度，帮助恢复。", 35, "LOW"),
                        meal("BREAKFAST", "豆腐鸡蛋卷", "豆腐 150g、鸡蛋 2 个、全麦饼。", 540),
                        meal("LUNCH", "牛肉荞麦面", "牛肉 150g、荞麦面 220g、青菜。", 760),
                        meal("DINNER", "虾仁米饭蔬菜", "虾仁 180g、米饭 180g、蔬菜。", 660),
                        meal("SNACK", "坚果牛奶", "牛奶 250ml、坚果 20g。", 250)),
                planDay("周复盘", workout("拉伸与核心", "核心稳定 15 分钟，拉伸 20 分钟。", 35, "LOW"),
                        meal("BREAKFAST", "鸡蛋牛奶吐司", "鸡蛋 2 个、牛奶 300ml、吐司 2 片。", 580),
                        meal("LUNCH", "鸡肉糙米饭", "鸡肉 170g、糙米饭 220g、蔬菜。", 740),
                        meal("DINNER", "鱼肉豆腐汤面", "鱼肉、豆腐、面条和青菜。", 680),
                        meal("SNACK", "睡前酸奶", "酸奶 180g，帮助补足蛋白质。", 160))
        );
    }

    private List<PlanDayTemplate> maintenanceTemplates() {
        return List.of(
                planDay("均衡启动", workout("快走", "快走 30 分钟，维持可对话强度。", 30, "MEDIUM"),
                        meal("BREAKFAST", "燕麦鸡蛋牛奶", "燕麦 40g、鸡蛋 1 个、牛奶 250ml。", 410),
                        meal("LUNCH", "鸡肉米饭蔬菜", "鸡肉 130g、米饭 160g、蔬菜两拳。", 610),
                        meal("DINNER", "鱼肉豆腐青菜", "鱼肉 120g、豆腐 100g、青菜。", 480),
                        meal("SNACK", "水果酸奶", "低糖水果一份、酸奶 120g。", 180)),
                planDay("力量维护", workout("力量训练", "深蹲、推、拉、核心各 3 组。", 40, "MEDIUM"),
                        meal("BREAKFAST", "全麦鸡蛋早餐", "全麦面包、鸡蛋、番茄和牛奶。", 430),
                        meal("LUNCH", "牛肉杂粮餐", "牛肉 120g、杂粮饭 160g、蔬菜。", 650),
                        meal("DINNER", "虾仁蔬菜面", "虾仁 120g、面条适量、绿叶菜。", 500),
                        meal("SNACK", "坚果小份", "坚果 15g，搭配无糖茶。", 100)),
                planDay("心肺提升", workout("慢跑", "慢跑 25 分钟，加热身和放松各 5 分钟。", 35, "MEDIUM"),
                        meal("BREAKFAST", "酸奶麦片杯", "酸奶、麦片、苹果丁。", 390),
                        meal("LUNCH", "鱼肉红薯蔬菜", "鱼肉 140g、红薯 180g、蔬菜。", 590),
                        meal("DINNER", "鸡蛋豆腐汤饭", "鸡蛋、豆腐、少量米饭和青菜。", 470),
                        meal("SNACK", "香蕉半根", "运动日前后补充半根香蕉。", 60)),
                planDay("恢复拉伸", workout("瑜伽", "基础瑜伽和肩颈髋部拉伸。", 30, "LOW"),
                        meal("BREAKFAST", "玉米牛奶鸡蛋", "玉米、牛奶、鸡蛋，简单稳定。", 400),
                        meal("LUNCH", "鸡肉荞麦面", "鸡肉、荞麦面、青菜和菌菇。", 600),
                        meal("DINNER", "清蒸鱼蔬菜", "清蒸鱼、蔬菜，主食按饥饿感调整。", 460),
                        meal("SNACK", "小番茄", "小番茄或黄瓜，降低晚间零食冲动。", 60)),
                planDay("抗阻强化", workout("器械训练", "腿部、背部、胸肩各选 1-2 个动作。", 45, "MEDIUM"),
                        meal("BREAKFAST", "蛋白三明治", "全麦面包、鸡蛋、低脂奶酪。", 450),
                        meal("LUNCH", "虾仁米饭便当", "虾仁、米饭、蔬菜，少油。", 620),
                        meal("DINNER", "牛肉蔬菜锅", "牛肉、豆腐、菌菇和绿叶菜。", 520),
                        meal("SNACK", "牛奶", "牛奶 250ml，补充钙和蛋白。", 135)),
                planDay("户外活动", workout("骑行", "骑行或户外步行，注意补水。", 45, "MEDIUM"),
                        meal("BREAKFAST", "香蕉燕麦牛奶", "燕麦、牛奶、香蕉半根。", 420),
                        meal("LUNCH", "外食均衡盘", "蛋白一掌、主食一拳、蔬菜两拳。", 650),
                        meal("DINNER", "豆腐青菜汤", "豆腐、青菜、鸡蛋，少油清淡。", 430),
                        meal("SNACK", "酸奶", "无糖酸奶 150g。", 110)),
                planDay("周总结", workout("散步恢复", "饭后散步两次，每次 15 分钟。", 30, "LOW"),
                        meal("BREAKFAST", "鸡蛋吐司水果", "鸡蛋、吐司 1 片、水果一份。", 410),
                        meal("LUNCH", "鱼肉米饭蔬菜", "鱼肉、米饭、蔬菜，保持平衡。", 600),
                        meal("DINNER", "鸡胸沙拉", "鸡胸肉、沙拉菜、少量主食。", 450),
                        meal("SNACK", "坚果酸奶", "酸奶 120g、坚果 10g。", 170))
        );
    }

    private PlanDayTemplate planDay(String focus, WorkoutTemplate workout, MealTemplate... meals) {
        return new PlanDayTemplate(focus, List.of(meals), workout);
    }

    private MealTemplate meal(String type, String title, String description, int calories) {
        return new MealTemplate(type, title, description, calories);
    }

    private WorkoutTemplate workout(String title, String description, int duration, String intensity) {
        return new WorkoutTemplate(title, description, duration, intensity);
    }

    private String localDietTemplateJson(List<PlanDayTemplate> days) {
        List<Map<String, Object>> jsonDays = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) {
            PlanDayTemplate day = days.get(i);
            jsonDays.add(Map.of(
                    "day", i + 1,
                    "focus", day.focus(),
                    "meals", day.meals().stream().map(MealTemplate::title).toList()
            ));
        }
        return json(Map.of("source", "LOCAL_TEMPLATE", "principle", "根据目标生成的差异化 7 天饮食模板", "days", jsonDays));
    }

    private String localWorkoutTemplateJson(List<PlanDayTemplate> days) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) {
            WorkoutTemplate workout = days.get(i).workout();
            items.add(Map.of(
                    "day", weekName(i + 1),
                    "type", workout.title(),
                    "durationMin", workout.durationMin(),
                    "intensity", workout.intensity()
            ));
        }
        return json(Map.of("source", "LOCAL_TEMPLATE", "principle", "根据目标生成的差异化 7 天运动模板", "weekly_schedule", items));
    }

    private JsonNode readJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readTree(json);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object nodeValue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.numberValue();
        }
        return value.asText();
    }

    private Object firstPresent(JsonNode node, String... fields) {
        for (String field : fields) {
            Object value = nodeValue(node, field);
            if (value != null && !String.valueOf(value).isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field, String fallback) {
        Object value = nodeValue(node, field);
        return value == null ? fallback : stringValue(value, fallback);
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode item : node) {
                if (!item.isNull() && !item.asText().isBlank()) {
                    values.add(item.asText());
                }
            }
            return values;
        }
        return node.asText().isBlank() ? List.of() : List.of(node.asText());
    }

    private String mealType(String value) {
        String text = value == null ? "" : value.toUpperCase(Locale.ROOT);
        if (text.contains("早餐") || text.contains("BREAKFAST")) return "BREAKFAST";
        if (text.contains("午餐") || text.contains("LUNCH")) return "LUNCH";
        if (text.contains("晚餐") || text.contains("DINNER")) return "DINNER";
        if (text.contains("加餐") || text.contains("SNACK")) return "SNACK";
        return text.isBlank() ? "SNACK" : text;
    }

    private String mealName(String type) {
        return switch (type) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            case "DINNER" -> "晚餐";
            default -> "加餐";
        };
    }

    private String normalizeIntensity(String value) {
        String text = value == null ? "" : value.toUpperCase(Locale.ROOT);
        if (text.contains("高") || text.contains("HIGH")) return "HIGH";
        if (text.contains("低") || text.contains("LOW") || text.contains("恢复")) return "LOW";
        return "MEDIUM";
    }

    private int estimateCalories(String text, int fallback) {
        MealEstimate estimate = calculateMealEstimate(text, "1份");
        return estimate.confidence().equals("LOW") ? fallback : Math.max(80, estimate.calories());
    }

    private int executionStreak(Long userId, Long planId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT d.plan_date, COUNT(i.id) total,
                       SUM(CASE WHEN e.status = 'DONE' THEN 1 ELSE 0 END) done
                FROM plan_day d
                JOIN plan_item i ON i.plan_day_id = d.id
                LEFT JOIN plan_execution e ON e.plan_item_id = i.id AND e.user_id = ?
                WHERE d.plan_id = ? AND d.user_id = ?
                GROUP BY d.plan_date ORDER BY d.plan_date DESC
                """, userId, planId, userId);
        int streak = 0;
        for (Map<String, Object> row : rows) {
            int total = intValue(row.get("total"), 0);
            int done = intValue(row.get("done"), 0);
            if (total > 0 && done >= Math.ceil(total * 0.6)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private MealEstimate calculateMealEstimate(String foodName, String amount) {
        String combinedText = (foodName + " " + amount).toLowerCase(Locale.ROOT);
        List<FoodHit> hits = detectFoodHits(combinedText);
        if (hits.isEmpty()) {
            FoodRule fallback = mixedMealRule();
            ParsedQuantity quantity = globalQuantity(amount, fallback, 1);
            Map<String, Object> item = buildFoodBreakdown(fallback, quantity, "估算默认餐");
            return new MealEstimate((Integer) item.get("calories"),
                    ((BigDecimal) item.get("proteinG")).doubleValue(),
                    ((BigDecimal) item.get("carbsG")).doubleValue(),
                    ((BigDecimal) item.get("fatG")).doubleValue(),
                    58,
                    List.of(item),
                    List.of(foodName),
                    "LOW");
        }

        ParsedQuantity totalQuantity = parseFirstQuantity(amount, null);
        double totalDefault = hits.stream().mapToDouble(hit -> hit.rule().defaultQuantity()).sum();
        List<Map<String, Object>> items = new ArrayList<>();
        double calories = 0;
        double protein = 0;
        double carbs = 0;
        double fat = 0;
        double weightedScore = 0;

        for (int i = 0; i < hits.size(); i++) {
            FoodHit hit = hits.get(i);
            Integer nextIndex = i + 1 < hits.size() ? hits.get(i + 1).index() : null;
            ParsedQuantity quantity = quantityAroundHit(combinedText, hit, nextIndex);
            if (quantity == null && totalQuantity != null && hits.size() > 1 && isMassOrVolume(totalQuantity.unit())) {
                double share = totalDefault <= 0 ? 1.0 / hits.size() : hit.rule().defaultQuantity() / totalDefault;
                quantity = new ParsedQuantity(totalQuantity.grams() * share, totalQuantity.unit(),
                        Math.round(totalQuantity.grams() * share) + "g（按总量拆分）");
            }
            if (quantity == null) {
                quantity = globalQuantity(amount, hit.rule(), hits.size());
            }
            Map<String, Object> item = buildFoodBreakdown(hit.rule(), quantity, hit.alias());
            items.add(item);
            double itemCalories = ((Number) item.get("calories")).doubleValue();
            calories += itemCalories;
            protein += ((BigDecimal) item.get("proteinG")).doubleValue();
            carbs += ((BigDecimal) item.get("carbsG")).doubleValue();
            fat += ((BigDecimal) item.get("fatG")).doubleValue();
            weightedScore += hit.rule().score() * Math.max(1, itemCalories);
        }

        int totalCalories = (int) Math.round(calories);
        int baseScore = totalCalories == 0 ? 60 : (int) Math.round(weightedScore / Math.max(1, calories));
        int score = Math.max(35, Math.min(96, baseScore
                + (protein >= 25 ? 4 : 0)
                - (fat >= 35 ? 7 : 0)
                - (totalCalories >= 900 ? 8 : 0)));
        List<String> unmatched = unmatchedFoodText(foodName, hits);
        String confidence = unmatched.isEmpty() ? "HIGH" : "MEDIUM";
        return new MealEstimate(totalCalories, protein, carbs, fat, score, items, unmatched, confidence);
    }

    private List<FoodHit> detectFoodHits(String text) {
        Map<String, FoodHit> byFood = new LinkedHashMap<>();
        for (FoodRule rule : foodRules()) {
            FoodHit best = null;
            for (String alias : rule.aliases()) {
                int index = text.indexOf(alias.toLowerCase(Locale.ROOT));
                if (index >= 0 && (best == null || index < best.index() || alias.length() > best.alias().length())) {
                    best = new FoodHit(rule, index, alias);
                }
            }
            if (best != null) {
                byFood.put(rule.name(), best);
            }
        }
        return byFood.values().stream()
                .sorted(Comparator.comparingInt(FoodHit::index))
                .toList();
    }

    private ParsedQuantity quantityAroundHit(String text, FoodHit hit, Integer nextIndex) {
        if (text == null || hit == null || hit.index() < 0 || hit.index() >= text.length()) {
            return null;
        }
        int aliasEnd = Math.min(text.length(), hit.index() + hit.alias().length());
        int afterEnd = nextIndex != null && nextIndex > aliasEnd
                ? nextIndex
                : Math.min(text.length(), aliasEnd + 18);
        afterEnd = Math.max(aliasEnd, Math.min(text.length(), afterEnd));
        String after = text.substring(aliasEnd, afterEnd);
        ParsedQuantity parsedAfter = parseFirstQuantity(after, hit.rule());
        if (parsedAfter != null) {
            return parsedAfter;
        }
        int beforeEnd = Math.max(0, Math.min(text.length(), hit.index()));
        int beforeStart = Math.max(0, beforeEnd - 14);
        String before = text.substring(beforeStart, beforeEnd);
        return parseLastQuantity(before, hit.rule());
    }

    private ParsedQuantity globalQuantity(String amount, FoodRule rule, int foodCount) {
        ParsedQuantity parsed = parseFirstQuantity(amount, rule);
        if (parsed != null && (foodCount == 1 || !isMassOrVolume(parsed.unit()))) {
            return parsed;
        }
        double multiplier = servingMultiplier(amount);
        return new ParsedQuantity(rule.defaultQuantity() * multiplier, rule.defaultUnit(),
                formatQuantity(rule.defaultQuantity() * multiplier, rule.defaultUnit()));
    }

    private ParsedQuantity parseFirstQuantity(String text, FoodRule rule) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?|半|一|两|二|三|四|五|六|七|八|九|十)\\s*(kg|千克|g|克|斤|ml|毫升|杯|碗|份|个|颗|只|片|勺)").matcher(text.toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return null;
        }
        return toQuantity(matcher.group(1), matcher.group(2), rule);
    }

    private ParsedQuantity parseLastQuantity(String text, FoodRule rule) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("([0-9]+(?:\\.[0-9]+)?|半|一|两|二|三|四|五|六|七|八|九|十)\\s*(kg|千克|g|克|斤|ml|毫升|杯|碗|份|个|颗|只|片|勺)\\s*$").matcher(text.toLowerCase(Locale.ROOT));
        ParsedQuantity result = null;
        while (matcher.find()) {
            result = toQuantity(matcher.group(1), matcher.group(2), rule);
        }
        return result;
    }

    private ParsedQuantity toQuantity(String numberText, String unit, FoodRule rule) {
        double number = numericValue(numberText);
        FoodRule safeRule = rule == null ? mixedMealRule() : rule;
        return switch (unit) {
            case "kg", "千克" -> new ParsedQuantity(number * 1000, "g", formatQuantity(number * 1000, "g"));
            case "斤" -> new ParsedQuantity(number * 500, "g", formatQuantity(number * 500, "g"));
            case "g", "克" -> new ParsedQuantity(number, "g", formatQuantity(number, "g"));
            case "ml", "毫升" -> new ParsedQuantity(number, "ml", formatQuantity(number, "ml"));
            case "杯" -> new ParsedQuantity(number * 250, "ml", formatQuantity(number * 250, "ml"));
            case "碗", "份" -> new ParsedQuantity(number * safeRule.defaultQuantity(), safeRule.defaultUnit(),
                    numberText + unit + "（约 " + formatQuantity(number * safeRule.defaultQuantity(), safeRule.defaultUnit()) + "）");
            case "个", "颗", "只" -> new ParsedQuantity(number * safeRule.defaultQuantity(), safeRule.defaultUnit(),
                    numberText + unit + "（约 " + formatQuantity(number * safeRule.defaultQuantity(), safeRule.defaultUnit()) + "）");
            case "片" -> new ParsedQuantity(number * Math.min(35, safeRule.defaultQuantity()), "g",
                    numberText + unit + "（约 " + formatQuantity(number * Math.min(35, safeRule.defaultQuantity()), "g") + "）");
            case "勺" -> new ParsedQuantity(number * 15, "g", numberText + unit + "（约 " + formatQuantity(number * 15, "g") + "）");
            default -> new ParsedQuantity(safeRule.defaultQuantity(), safeRule.defaultUnit(),
                    formatQuantity(safeRule.defaultQuantity(), safeRule.defaultUnit()));
        };
    }

    private Map<String, Object> buildFoodBreakdown(FoodRule rule, ParsedQuantity quantity, String alias) {
        double factor = quantity.grams() / 100.0;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("foodName", rule.name());
        item.put("matchedKeyword", alias);
        item.put("quantity", Math.round(quantity.grams() * 10.0) / 10.0);
        item.put("unit", quantity.unit());
        item.put("displayAmount", quantity.display());
        item.put("calories", (int) Math.round(rule.caloriesPer100() * factor));
        item.put("proteinG", decimal(rule.proteinPer100() * factor));
        item.put("carbsG", decimal(rule.carbsPer100() * factor));
        item.put("fatG", decimal(rule.fatPer100() * factor));
        item.put("healthScore", rule.score());
        return item;
    }

    private List<String> unmatchedFoodText(String foodName, List<FoodHit> hits) {
        String cleaned = foodName.toLowerCase(Locale.ROOT);
        for (FoodHit hit : hits) {
            for (String alias : hit.rule().aliases()) {
                cleaned = cleaned.replace(alias.toLowerCase(Locale.ROOT), " ");
            }
        }
        cleaned = cleaned.replaceAll("[0-9.]+\\s*(kg|千克|g|克|斤|ml|毫升|杯|碗|份|个|颗|只|片|勺)", " ");
        cleaned = cleaned.replaceAll("[,，、+＋/和与\\s]", " ").trim();
        if (cleaned.isBlank()) {
            return List.of();
        }
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(part -> !part.isBlank())
                .distinct()
                .toList();
    }

    private String mealTip(MealEstimate estimate) {
        if (!estimate.unmatched().isEmpty()) {
            return "已按可识别食材汇总，未识别部分可补充克数或换成更具体的食材名称。";
        }
        if (estimate.healthScore() >= 80 && estimate.protein() >= 25) {
            return "蛋白质和主食搭配比较稳，继续补足蔬菜会更完整。";
        }
        if (estimate.calories() >= 900 || estimate.fat() >= 35) {
            return "这餐热量或脂肪偏高，建议减少油炸和酱料，增加蔬菜体积。";
        }
        return "估算结果来自本地营养规则库，可输入'鸡胸肉150g 米饭200g'得到更准结果。";
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private boolean isMassOrVolume(String unit) {
        return "g".equals(unit) || "ml".equals(unit);
    }

    private double servingMultiplier(String amount) {
        if (amount == null) return 1.0;
        String text = amount.toLowerCase(Locale.ROOT);
        ParsedQuantity explicit = parseFirstQuantity(text, mixedMealRule());
        if (explicit != null && !isMassOrVolume(explicit.unit())) {
            return Math.max(0.2, explicit.grams() / mixedMealRule().defaultQuantity());
        }
        if (text.contains("半") || text.contains("0.5")) return 0.5;
        if (text.contains("两份") || text.contains("2份") || text.contains("二份")) return 2.0;
        if (text.contains("大")) return 1.3;
        if (text.contains("小")) return 0.75;
        return 1.0;
    }

    private double numericValue(String text) {
        return switch (text) {
            case "半" -> 0.5;
            case "一" -> 1.0;
            case "两", "二" -> 2.0;
            case "三" -> 3.0;
            case "四" -> 4.0;
            case "五" -> 5.0;
            case "六" -> 6.0;
            case "七" -> 7.0;
            case "八" -> 8.0;
            case "九" -> 9.0;
            case "十" -> 10.0;
            default -> Double.parseDouble(text);
        };
    }

    private String formatQuantity(double amount, String unit) {
        double rounded = Math.round(amount * 10.0) / 10.0;
        if (Math.abs(rounded - Math.round(rounded)) < 0.01) {
            return Math.round(rounded) + unit;
        }
        return rounded + unit;
    }

    private FoodRule mixedMealRule() {
        return new FoodRule("混合餐", List.of("meal", "餐", "饭"), 165, 8, 20, 5, 60, 300, "g");
    }

    private List<FoodRule> foodRules() {
        return List.of(
                new FoodRule("鸡胸肉", List.of("鸡胸肉", "鸡胸", "chicken breast"), 165, 31, 0, 3.6, 86, 120, "g"),
                new FoodRule("鸡肉", List.of("鸡肉", "chicken"), 190, 26, 0, 8, 78, 120, "g"),
                new FoodRule("米饭", List.of("米饭", "白米饭", "rice"), 116, 2.6, 25.9, 0.3, 66, 150, "g"),
                new FoodRule("糙米饭", List.of("糙米", "糙米饭", "brown rice"), 111, 2.6, 23, 0.9, 75, 150, "g"),
                new FoodRule("面条", List.of("面条", "拉面", "挂面", "noodle", "noodles"), 137, 4.5, 25, 2, 60, 200, "g"),
                new FoodRule("全麦面包", List.of("全麦面包", "面包", "吐司", "toast"), 246, 9, 43, 4.2, 68, 70, "g"),
                new FoodRule("鸡蛋", List.of("鸡蛋", "水煮蛋", "蛋", "egg"), 144, 13.3, 2.8, 8.8, 82, 50, "g"),
                new FoodRule("牛奶", List.of("牛奶", "milk"), 54, 3.2, 5.1, 3.2, 78, 250, "ml"),
                new FoodRule("酸奶", List.of("酸奶", "yogurt"), 72, 3.5, 9.3, 2.7, 76, 180, "g"),
                new FoodRule("燕麦", List.of("燕麦", "燕麦片", "oat", "oats"), 377, 15, 67, 6.9, 82, 40, "g"),
                new FoodRule("牛肉", List.of("牛肉", "beef"), 250, 26, 0, 15, 76, 120, "g"),
                new FoodRule("鱼肉", List.of("鱼肉", "鱼", "fish"), 120, 20, 0, 4, 86, 120, "g"),
                new FoodRule("虾仁", List.of("虾仁", "虾", "shrimp"), 99, 24, 0.2, 0.3, 88, 120, "g"),
                new FoodRule("豆腐", List.of("豆腐", "tofu"), 84, 8.1, 3.8, 4.2, 82, 150, "g"),
                new FoodRule("蔬菜", List.of("蔬菜", "青菜", "西兰花", "生菜", "菜", "vegetable"), 35, 2.5, 6, 0.4, 92, 200, "g"),
                new FoodRule("沙拉", List.of("沙拉", "salad"), 80, 3, 10, 3, 84, 200, "g"),
                new FoodRule("土豆", List.of("土豆", "马铃薯", "potato"), 81, 2, 17, 0.1, 70, 150, "g"),
                new FoodRule("红薯", List.of("红薯", "地瓜", "sweet potato"), 86, 1.6, 20, 0.1, 76, 150, "g"),
                new FoodRule("香蕉", List.of("香蕉", "banana"), 93, 1.4, 22, 0.2, 72, 120, "g"),
                new FoodRule("苹果", List.of("苹果", "apple"), 53, 0.3, 14, 0.2, 78, 180, "g"),
                new FoodRule("坚果", List.of("坚果", "杏仁", "核桃", "nuts"), 580, 18, 22, 50, 64, 25, "g"),
                new FoodRule("油炸食品", List.of("炸鸡", "薯条", "油炸", "fried"), 520, 12, 42, 32, 38, 150, "g"),
                new FoodRule("奶茶", List.of("奶茶", "milk tea"), 65, 1.2, 12, 1.6, 42, 500, "ml")
        );
    }

    private ExerciseRule exerciseRule(String exerciseType) {
        String text = exerciseType.toLowerCase(Locale.ROOT);
        for (ExerciseRule rule : exerciseRules()) {
            if (rule.aliases().stream().anyMatch(alias -> text.contains(alias.toLowerCase(Locale.ROOT)))) {
                return rule;
            }
        }
        return new ExerciseRule("综合训练", List.of("训练", "运动"), 4.5, "保持可对话强度，先稳定完成时长。");
    }

    private List<ExerciseRule> exerciseRules() {
        return List.of(
                new ExerciseRule("跑步", List.of("run", "跑步", "慢跑", "跑"), 8.3, "跑前热身 5 分钟，结束后放松小腿和髂胫束。"),
                new ExerciseRule("快走", List.of("walk", "步行", "快走", "走路", "走"), 3.8, "保持微喘但可说话的节奏，适合做低压力燃脂。"),
                new ExerciseRule("骑行", List.of("bike", "cycling", "骑行", "骑车", "单车"), 6.8, "注意踏频稳定，膝盖不适时降低阻力。"),
                new ExerciseRule("游泳", List.of("swim", "游泳", "游"), 7.0, "游泳消耗较高，训练后注意补水和蛋白质。"),
                new ExerciseRule("瑜伽", List.of("yoga", "瑜伽"), 2.8, "瑜伽更偏恢复和灵活性，可搭配力量训练。"),
                new ExerciseRule("力量训练", List.of("strength", "力量", "器械", "抗阻", "撸铁"), 5.5, "优先保证动作质量，复合动作和核心训练交替安排。"),
                new ExerciseRule("跳绳", List.of("跳绳"), 10.0, "冲击较高，建议分组完成并留意踝膝反馈。"),
                new ExerciseRule("HIIT", List.of("hiit", "间歇"), 8.0, "强度较高，每周 1-2 次即可，避免连续高强度。")
        );
    }

    private String intensityLabel(String intensity) {
        return switch (intensity) {
            case "HIGH" -> "高强度";
            case "MEDIUM" -> "中等强度";
            default -> "低强度";
        };
    }

    private LocalDate monday(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    private String currentWeekCode() {
        LocalDate now = LocalDate.now();
        WeekFields fields = WeekFields.ISO;
        return now.get(fields.weekBasedYear()) + "-W" + String.format("%02d", now.get(fields.weekOfWeekBasedYear()));
    }

    private Map<String, Object> queryOne(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForMap(sql, args);
        } catch (EmptyResultDataAccessException ex) {
            return new LinkedHashMap<>();
        }
    }

    private Object queryScalar(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, Object.class, args);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Long queryLong(String sql, Object... args) {
        Object value = queryScalar(sql, args);
        return longValue(value);
    }

    private LocalDate queryDate(String sql, Object... args) {
        Object value = queryScalar(sql, args);
        return dateValue(value, null);
    }

    private Map<String, Object> normalize(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((key, value) -> result.put(toCamel(key), normalizeValue(value)));
        return result;
    }

    private List<Map<String, Object>> normalizeList(List<Map<String, Object>> rows) {
        return rows.stream().map(this::normalize).toList();
    }

    private Object normalizeValue(Object value) {
        if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime().toString();
        return value;
    }

    private String toCamel(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : lower.toCharArray()) {
            if (c == '_') {
                up = true;
            } else if (up) {
                sb.append(Character.toUpperCase(c));
                up = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Long longValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private BigDecimal decimalValue(Object value) {
        return decimalValue(value, BigDecimal.ZERO);
    }

    private BigDecimal decimalValue(Object value, BigDecimal fallback) {
        if (value == null) return fallback;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private LocalDate dateValue(Object value, LocalDate fallback) {
        if (value == null || String.valueOf(value).isBlank()) return fallback;
        if (value instanceof LocalDate d) return d;
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) return fallback;
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private String value(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private record FoodRule(String name, List<String> aliases, double caloriesPer100, double proteinPer100,
                            double carbsPer100, double fatPer100, int score, double defaultQuantity,
                            String defaultUnit) {
    }

    private record FoodHit(FoodRule rule, int index, String alias) {
    }

    private record ParsedQuantity(double grams, String unit, String display) {
    }

    private record MealEstimate(int calories, double protein, double carbs, double fat, int healthScore,
                                List<Map<String, Object>> items, List<String> unmatched, String confidence) {
    }

    private record ExerciseRule(String name, List<String> aliases, double met, String suggestion) {
    }

    private record MealTemplate(String mealType, String title, String description, int calories) {
    }

    private record WorkoutTemplate(String title, String description, int durationMin, String intensity) {
    }

    private record PlanDayTemplate(String focus, List<MealTemplate> meals, WorkoutTemplate workout) {
    }
}
