package com.hnust.health.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class WeightHistoryResponse {
    private LocalDate recordDate;
    private BigDecimal currentWeight;
    private BigDecimal calculatedBmi;
    private Integer updateCount;
}
