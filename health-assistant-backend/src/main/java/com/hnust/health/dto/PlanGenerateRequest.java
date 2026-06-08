package com.hnust.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * AI 计划生成请求
 */
@Data
public class PlanGenerateRequest {

    @NotNull(message = "周期起始日期不能为空")
    private LocalDate cycleStartDate;
}
