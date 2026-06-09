package com.hnust.health.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.health.mapper.*;
import com.hnust.health.model.*;
import com.hnust.health.service.ChatService;
import com.hnust.health.service.DeepSeekService;
import com.hnust.health.util.WeightTrendAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final DeepSeekService deepSeekService;
    private final SysUserMapper sysUserMapper;
    private final HealthProfileMapper profileMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final AiPlanMapper aiPlanMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public Map<String, Object> chat(Long userId, String message) {
        SysUser user = sysUserMapper.selectById(userId);
        HealthProfile profile = profileMapper.selectById(userId);
        List<WeightRecord> weights = weightRecordMapper.selectByUserIdAndDays(userId, 90);
        var trend = WeightTrendAnalyzer.analyze(weights);

        StringBuilder ctx = new StringBuilder();
        ctx.append("你是用户的专属健康助手。你可以访问用户的以下数据（但绝不能提及邮箱和电话）：\n");
        ctx.append("- 昵称：").append(user.getNickname() != null ? user.getNickname() : user.getUsername()).append("\n");
        if (profile != null) {
            ctx.append("- 年龄：").append(profile.getAge()).append("岁，性别：").append(profile.getGender()==1?"男":profile.getGender()==2?"女":"其他").append("\n");
            ctx.append("- 身高：").append(profile.getHeightCm()).append("cm，建档体重：").append(profile.getBaselineWeight()).append("kg\n");
            ctx.append("- 活动水平：").append(profile.getActivityLevel()).append("，饮食偏好：").append(profile.getDietPreference()).append("，目标：").append(profile.getHealthGoal()).append("\n");
        }
        ctx.append("- 体重趋势：").append(trend.trendDescription()).append("\n");
        if (trend.startWeight() != null) ctx.append("  起始").append(trend.startWeight()).append("kg → 当前").append(trend.endWeight()).append("kg，变化").append(trend.totalChange()).append("kg\n");

        // 近7天打卡记录
        String checkinCtx = buildCheckinContext(userId);
        if (!checkinCtx.isBlank()) ctx.append("- 近7天打卡：\n").append(checkinCtx).append("\n");

        ctx.append("\n用户消息：").append(message).append("\n\n");
        ctx.append("请友好简洁地回复。如果用户要求生成计划，请生成包含diet_plan（7天详细饮食）和workout_plan（7天运动）的JSON，并在回复中说明已生成。");

        String reply = deepSeekService.chat(ctx.toString(), message);

        Map<String, Object> result = new LinkedHashMap<>();
        // 清除markdown和JSON，只保留自然语言
        String clean = reply.replaceAll("```[\\s\\S]*?```", "").replaceAll("\\{[^}]*\\}", "").replaceAll("[#*>_`]", "").trim();
        if (clean.length() < 10) clean = reply.replaceAll("[#*>`_]", "").trim();
        result.put("reply", clean);

        // 尝试从回复中提取计划
        if (message.contains("计划") || message.contains("方案") || message.contains("生成")) {
            try {
                String json = extractJson(reply);
                if (json != null && (json.contains("diet_plan") || json.contains("workout_plan"))) {
                    var root = om.readTree(json);
                    AiPlan plan = new AiPlan();
                    plan.setUserId(userId);
                    plan.setCycleStartDate(java.time.LocalDate.now());
                    plan.setMemoryContextSnapshot(om.writeValueAsString(Map.of("description", trend.trendDescription(), "startWeight", trend.startWeight(), "endWeight", trend.endWeight())));
                    plan.setDietPlanJson(root.has("diet_plan") ? om.writeValueAsString(root.get("diet_plan")) : null);
                    plan.setWorkoutPlanJson(root.has("workout_plan") ? om.writeValueAsString(root.get("workout_plan")) : null);
                    plan.setLlmReasoningChain(reply.replaceAll("```[\\s\\S]*?```", "").replaceAll("[\\[\\{][\\s\\S]*[\\]\\}]", "").trim());
                    aiPlanMapper.insert(plan);
                    result.put("planId", plan.getId());
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    /**
     * 构建近7天打卡记录的文字摘要，供 AI 上下文使用
     */
    private String buildCheckinContext(Long userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<DailyCheckin> checkins = dailyCheckinMapper.selectByUserIdAndDateRange(userId, start, end);
        if (checkins.isEmpty()) return "";

        Map<LocalDate, List<DailyCheckin>> byDate = new LinkedHashMap<>();
        for (DailyCheckin c : checkins) {
            byDate.computeIfAbsent(c.getRecordDate(), k -> new ArrayList<>()).add(c);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<LocalDate, List<DailyCheckin>> entry : byDate.entrySet()) {
            sb.append("  ").append(entry.getKey().format(fmt)).append(": ");
            List<String> parts = new ArrayList<>();
            Set<String> mealTypes = new HashSet<>();
            int waterCups = 0;
            boolean hasExercise = false;
            String exerciseDesc = null;

            for (DailyCheckin c : entry.getValue()) {
                switch (c.getCheckinType()) {
                    case "MEAL" -> {
                        if (c.getMealType() != null) mealTypes.add(c.getMealType());
                        if (c.getFoodDesc() != null) parts.add(c.getMealType() + "-" + c.getFoodDesc());
                    }
                    case "EXERCISE" -> {
                        hasExercise = true;
                        exerciseDesc = c.getExerciseType() + (c.getDurationMin() != null ? " " + c.getDurationMin() + "分钟" : "");
                    }
                    case "WATER" -> waterCups += c.getWaterCups() != null ? c.getWaterCups() : 0;
                }
            }
            if (!mealTypes.isEmpty()) parts.add(0, "饮食(" + String.join("/", mealTypes) + ")");
            if (hasExercise) parts.add("运动(" + exerciseDesc + ")");
            if (waterCups > 0) parts.add("饮水" + waterCups + "杯");
            sb.append(String.join("; ", parts)).append("\n");
        }
        return sb.toString();
    }

    private String extractJson(String text) {
        // 尝试找到json代码块
        int codeStart = text.indexOf("```json");
        if (codeStart >= 0) {
            int codeEnd = text.indexOf("```", codeStart + 7);
            if (codeEnd > codeStart) return text.substring(codeStart + 7, codeEnd).trim();
        }
        // 尝试数组格式 [{...}]
        int arrStart = text.indexOf("[{");
        int arrEnd = text.lastIndexOf("}]");
        if (arrStart >= 0 && arrEnd > arrStart) return text.substring(arrStart, arrEnd + 2);
        // 尝试对象格式 {...}
        int objStart = text.indexOf("{");
        int objEnd = text.lastIndexOf("}");
        if (objStart >= 0 && objEnd > objStart) return text.substring(objStart, objEnd + 1);
        return null;
    }
}
