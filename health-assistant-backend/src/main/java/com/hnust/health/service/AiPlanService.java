package com.hnust.health.service;

import com.hnust.health.dto.PlanGenerateRequest;
import com.hnust.health.dto.PlanResponse;

/**
 * AI 计划生成服务接口
 */
public interface AiPlanService {

    /**
     * 触发 DeepSeek V4 Pro 生成个性化干预计划
     */
    PlanResponse generatePlan(Long userId, PlanGenerateRequest request);
}
