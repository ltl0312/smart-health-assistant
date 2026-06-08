package com.hnust.health.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 健康档案创建/更新请求
 */
@Data
public class ProfileRequest {

    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄必须大于0")
    @Max(value = 150, message = "年龄必须小于150")
    private Integer age;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @NotNull(message = "身高不能为空")
    @DecimalMin(value = "50.0", message = "身高范围不合理")
    @DecimalMax(value = "250.0", message = "身高范围不合理")
    private BigDecimal heightCm;

    @NotNull(message = "初始体重不能为空")
    @DecimalMin(value = "20.0", message = "体重范围不合理")
    @DecimalMax(value = "300.0", message = "体重范围不合理")
    private BigDecimal baselineWeight;

    @NotBlank(message = "活动水平不能为空")
    private String activityLevel;

    @NotBlank(message = "饮食偏好不能为空")
    private String dietPreference;

    @NotBlank(message = "健康目标不能为空")
    private String healthGoal;
}
