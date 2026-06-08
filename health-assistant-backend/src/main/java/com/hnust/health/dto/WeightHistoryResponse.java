package com.hnust.health.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 体重历史记录响应
 */
@Data
@AllArgsConstructor
public class WeightHistoryResponse {
    private LocalDate recordDate;
    private BigDecimal currentWeight;
    private BigDecimal calculatedBmi;
}
