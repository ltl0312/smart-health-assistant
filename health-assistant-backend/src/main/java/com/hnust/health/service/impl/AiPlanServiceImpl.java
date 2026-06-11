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

import java.time.DayOfWeek;
import java.time.LocalDate;
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

        // 阶段六：解析 JSON 响应
        String dietPlanJson = null;
        String workoutPlanJson = null;
        String reasoningChain = null;

        try {
            // 阶段五：调用 DeepSeek V4 Pro
            log.info("开始调用 DeepSeek V4 Pro...");
            String llmResponse = deepSeekService.chat(systemPrompt, userMessage);
            log.info("DeepSeek 响应获取成功");

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
            log.warn("AI 计划生成失败，使用本地模板待审核: {}", e.getMessage());
            dietPlanJson = localDietPlanJson();
            workoutPlanJson = localWorkoutPlanJson();
            reasoningChain = "AI 服务暂时不可用，系统已根据通用健康规则生成本地模板，请审核后再应用。";
        }

        // 阶段七：落库保存
        if (dietPlanJson == null && workoutPlanJson == null) {
            dietPlanJson = localDietPlanJson();
            workoutPlanJson = localWorkoutPlanJson();
            reasoningChain = "AI 返回内容不完整，系统已生成本地模板待审核。";
        }
        if (dietPlanJson == null) {
            dietPlanJson = "{}";
        }
        if (workoutPlanJson == null) {
            workoutPlanJson = "{}";
        }

        AiPlan aiPlan = new AiPlan();
        aiPlan.setUserId(userId);
        LocalDate requestedStart = request.getCycleStartDate() == null ? LocalDate.now() : request.getCycleStartDate();
        aiPlan.setCycleStartDate(requestedStart.with(DayOfWeek.MONDAY));
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
        aiPlan.setStatus("PENDING_REVIEW");
        aiPlanMapper.insert(aiPlan);

        // 构建响应
        PlanResponse response = new PlanResponse();
        response.setPlanId(aiPlan.getId());
        response.setCycleStartDate(aiPlan.getCycleStartDate());
        response.setMemoryContextSnapshot(aiPlan.getMemoryContextSnapshot());
        response.setDietPlanJson(aiPlan.getDietPlanJson());
        response.setWorkoutPlanJson(aiPlan.getWorkoutPlanJson());
        response.setLlmReasoningChain(aiPlan.getLlmReasoningChain());
        response.setStatus(aiPlan.getStatus());

        log.info("AI 计划生成并保存为待审核，planId={}", aiPlan.getId());
        return response;
    }

    private String localDietPlanJson() {
        return """
                {
                  "source": "LOCAL_TEMPLATE",
                  "principle": "完整 7 天本地兜底模板，保证每天餐食不同，用户审核后才应用",
                  "days": [
                    {"day": 1, "focus": "轻盈启动", "meals": [
                      {"meal": "早餐", "foods": ["燕麦40g", "鸡蛋1个", "牛奶250ml"], "calories": 410},
                      {"meal": "午餐", "foods": ["鸡胸肉140g", "糙米饭120g", "西兰花250g"], "calories": 520},
                      {"meal": "晚餐", "foods": ["鱼肉120g", "豆腐100g", "青菜250g"], "calories": 450},
                      {"meal": "加餐", "foods": ["苹果1个", "杏仁10g"], "calories": 160}
                    ]},
                    {"day": 2, "focus": "力量维护", "meals": [
                      {"meal": "早餐", "foods": ["全麦吐司1片", "鸡蛋2个", "番茄100g"], "calories": 380},
                      {"meal": "午餐", "foods": ["瘦牛肉120g", "杂粮饭150g", "凉拌蔬菜250g"], "calories": 590},
                      {"meal": "晚餐", "foods": ["虾仁140g", "菌菇150g", "绿叶菜250g"], "calories": 390},
                      {"meal": "加餐", "foods": ["无糖酸奶150g"], "calories": 110}
                    ]},
                    {"day": 3, "focus": "心肺提升", "meals": [
                      {"meal": "早餐", "foods": ["酸奶180g", "麦片35g", "蓝莓50g"], "calories": 360},
                      {"meal": "午餐", "foods": ["清蒸鱼150g", "红薯180g", "蔬菜250g"], "calories": 500},
                      {"meal": "晚餐", "foods": ["鸡肉120g", "生菜200g", "彩椒100g"], "calories": 380},
                      {"meal": "加餐", "foods": ["香蕉半根"], "calories": 60}
                    ]},
                    {"day": 4, "focus": "恢复调整", "meals": [
                      {"meal": "早餐", "foods": ["玉米半根", "鸡蛋1个", "豆浆250ml"], "calories": 340},
                      {"meal": "午餐", "foods": ["鸡肉120g", "荞麦面150g", "青菜250g"], "calories": 510},
                      {"meal": "晚餐", "foods": ["豆腐150g", "番茄150g", "冬瓜200g"], "calories": 350},
                      {"meal": "加餐", "foods": ["小番茄200g"], "calories": 60}
                    ]},
                    {"day": 5, "focus": "高蛋白日", "meals": [
                      {"meal": "早餐", "foods": ["全麦面包2片", "低脂奶酪1片", "鸡蛋1个"], "calories": 420},
                      {"meal": "午餐", "foods": ["虾仁150g", "米饭130g", "蔬菜250g"], "calories": 520},
                      {"meal": "晚餐", "foods": ["瘦牛肉100g", "豆腐100g", "菌菇200g"], "calories": 430},
                      {"meal": "加餐", "foods": ["牛奶250ml"], "calories": 135}
                    ]},
                    {"day": 6, "focus": "户外活动", "meals": [
                      {"meal": "早餐", "foods": ["香蕉半根", "燕麦35g", "酸奶150g"], "calories": 340},
                      {"meal": "午餐", "foods": ["鱼肉140g", "米饭120g", "蔬菜300g"], "calories": 500},
                      {"meal": "晚餐", "foods": ["鸡蛋1个", "豆腐150g", "青菜300g"], "calories": 360},
                      {"meal": "加餐", "foods": ["坚果15g"], "calories": 90}
                    ]},
                    {"day": 7, "focus": "复盘微调", "meals": [
                      {"meal": "早餐", "foods": ["鸡蛋1个", "吐司1片", "牛奶200ml"], "calories": 360},
                      {"meal": "午餐", "foods": ["鸡腿肉150g", "糙米饭150g", "蔬菜250g"], "calories": 610},
                      {"meal": "晚餐", "foods": ["沙拉菜250g", "鸡胸肉120g", "红薯120g"], "calories": 420},
                      {"meal": "加餐", "foods": ["猕猴桃1个"], "calories": 70}
                    ]}
                  ]
                }
                """;
    }

    private String localWorkoutPlanJson() {
        return """
                {
                  "source": "LOCAL_TEMPLATE",
                  "principle": "周一到周日完整运动安排，含训练日和恢复日",
                  "weekly_schedule": [
                    {"day": "周一", "type": "快走", "exercises": ["快走35分钟", "小腿拉伸5分钟"], "duration_min": 40, "intensity": "MEDIUM"},
                    {"day": "周二", "type": "力量训练", "exercises": ["深蹲3组×10次", "俯卧撑3组×8次", "划船动作3组×12次"], "duration_min": 45, "intensity": "MEDIUM"},
                    {"day": "周三", "type": "慢跑间歇", "exercises": ["慢跑5分钟×4轮", "快走3分钟×4轮"], "duration_min": 35, "intensity": "MEDIUM"},
                    {"day": "周四", "type": "瑜伽拉伸", "exercises": ["髋部拉伸10分钟", "肩颈放松10分钟", "腹式呼吸5分钟"], "duration_min": 25, "intensity": "LOW"},
                    {"day": "周五", "type": "下肢力量", "exercises": ["臀桥4组×12次", "箭步蹲3组×10次", "平板支撑3组×30秒"], "duration_min": 45, "intensity": "MEDIUM"},
                    {"day": "周六", "type": "骑行", "exercises": ["轻中等骑行40分钟", "下肢拉伸5分钟"], "duration_min": 45, "intensity": "MEDIUM"},
                    {"day": "周日", "type": "散步恢复", "exercises": ["饭后散步15分钟×2次", "睡前拉伸10分钟"], "duration_min": 40, "intensity": "LOW"}
                  ]
                }
                """;
    }

}
