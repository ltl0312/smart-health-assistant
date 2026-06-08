package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.dto.PlanGenerateRequest;
import com.hnust.health.dto.PlanResponse;
import com.hnust.health.service.AiPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

/**
 * AI 计划生成接口 - 系统核心引擎端点
 * 需要 JWT 鉴权
 */
@RestController
@RequestMapping("/plan")
@RequiredArgsConstructor
public class PlanController {

    private final AiPlanService aiPlanService;

    /**
     * 触发 DeepSeek V4 Pro 生成个性化干预计划
     * POST /api/plan/generate
     */
    @PostMapping("/generate")
    public Result<PlanResponse> generatePlan(@Valid @RequestBody PlanGenerateRequest request,
                                              @RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        PlanResponse response = aiPlanService.generatePlan(userId, request);
        return Result.ok(response);
    }
}
