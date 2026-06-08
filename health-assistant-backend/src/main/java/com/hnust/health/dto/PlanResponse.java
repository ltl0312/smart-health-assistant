package com.hnust.health.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * AI 计划响应
 */
@Data
public class PlanResponse {
    private Long planId;
    private LocalDate cycleStartDate;
    private String memoryContextSnapshot;
    private String dietPlanJson;
    private String workoutPlanJson;
    private String llmReasoningChain;
}
