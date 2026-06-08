package com.hnust.health.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 体重记录请求
 */
@Data
public class WeightRecordRequest {

    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @NotNull(message = "体重不能为空")
    @DecimalMin(value = "20.0", message = "体重范围不合理")
    @DecimalMax(value = "300.0", message = "体重范围不合理")
    private BigDecimal currentWeight;
}
