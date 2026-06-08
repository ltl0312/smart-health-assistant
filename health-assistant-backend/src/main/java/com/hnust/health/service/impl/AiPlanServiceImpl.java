package com.hnust.health.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.health.dto.PlanGenerateRequest;
import com.hnust.health.dto.PlanResponse;
import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.AiPlanMapper;
import com.hnust.health.mapper.WeightRecordMapper;
import com.hnust.health.model.AiPlan;
import com.hnust.health.model.HealthProfile;
import com.hnust.health.model.WeightRecord;
import com.hnust.health.service.AiPlanService;
import com.hnust.health.service.DeepSeekService;
import com.hnust.health.service.HealthProfileService;
import com.hnust.health.util.PromptBuilder;
import com.hnust.health.util.WeightTrendAnalyzer;
import com.hnust.health.util.WeightTrendAnalyzer.TrendResult;
import com.hnust.health.utils.PromptCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 计划生成服务实现 - 系统核心引擎
 * 检索用户档案与历史体重 → 组装提示词 → 调用 DeepSeek → 解析响应 → 落库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPlanServiceImpl implements AiPlanService {

    private final HealthProfileService healthProfileService;
    private final WeightRecordMapper weightRecordMapper;
    private final DeepSeekService deepSeekService;
    private final AiPlanMapper aiPlanMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PlanResponse generatePlan(Long userId, PlanGenerateRequest request) {
        // 阶段一：检索用户生理档案
        HealthProfile profile = healthProfileService.getProfile(userId);

        // 阶段二：拉取最近体重数据（最多4周，4条记录用于记忆上下文）
        List<WeightRecord> recentRecords = weightRecordMapper.selectRecentByUserId(userId, 4);

        // 阶段三：计算体重趋势（一阶导数/斜率）
        TrendResult trend = WeightTrendAnalyzer.analyze(recentRecords);

        // 阶段四：组装大模型提示词
        String systemPrompt = PromptBuilder.buildSystemPrompt(profile, trend);
        String userMessage = PromptBuilder.buildUserMessage();
        log.info("提示词组装完成，系统提示词长度: {} 字符", systemPrompt.length());

        // 阶段五：调用 DeepSeek V4 Pro
        log.info("开始调用 DeepSeek V4 Pro...");
        String llmResponse = deepSeekService.chat(systemPrompt, userMessage);
        log.info("DeepSeek 响应获取成功");

        // 阶段六：解析 JSON 响应
        String dietPlanJson = null;
        String workoutPlanJson = null;
        String reasoningChain = null;

        try {
            String jsonContent = PromptCleaner.cleanJsonResponse(llmResponse);
            JsonNode root = objectMapper.readTree(jsonContent);

            if (root.has("diet_plan")) {
                dietPlanJson = objectMapper.writeValueAsString(root.get("diet_plan"));
            }
            if (root.has("workout_plan")) {
                workoutPlanJson = objectMapper.writeValueAsString(root.get("workout_plan"));
            }
            if (root.has("analysis")) {
                reasoningChain = root.get("analysis").asText();
            }
        } catch (Exception e) {
            log.warn("JSON 解析失败，将原始响应作为 reasoning_chain 存储: {}", e.getMessage());
            reasoningChain = llmResponse;
            // 如果无法解析结构化JSON，将整个响应作为推理链存储
        }

        // 阶段七：落库保存
        AiPlan aiPlan = new AiPlan();
        aiPlan.setUserId(userId);
        aiPlan.setCycleStartDate(request.getCycleStartDate());
        // 用 Jackson 构建 JSON 快照（自动转义特殊字符）
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("description", trend.trendDescription());
        snapshot.put("startWeight", trend.startWeight());
        snapshot.put("endWeight", trend.endWeight());
        snapshot.put("totalChange", trend.totalChange());
        snapshot.put("weeklyRate", trend.weeklyChangeRate());
        try {
            aiPlan.setMemoryContextSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            aiPlan.setMemoryContextSnapshot("{}");
        }
        aiPlan.setDietPlanJson(dietPlanJson);
        aiPlan.setWorkoutPlanJson(workoutPlanJson);
        aiPlan.setLlmReasoningChain(reasoningChain);
        aiPlanMapper.insert(aiPlan);

        // 构建响应
        PlanResponse response = new PlanResponse();
        response.setPlanId(aiPlan.getId());
        response.setCycleStartDate(aiPlan.getCycleStartDate());
        response.setMemoryContextSnapshot(aiPlan.getMemoryContextSnapshot());
        response.setDietPlanJson(aiPlan.getDietPlanJson());
        response.setWorkoutPlanJson(aiPlan.getWorkoutPlanJson());
        response.setLlmReasoningChain(aiPlan.getLlmReasoningChain());

        log.info("AI 计划生成并保存成功，planId={}", aiPlan.getId());
        return response;
    }

}
