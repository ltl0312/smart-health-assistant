package com.hnust.health.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 健康生理档案表 - 存储静态或低频更新的生理指标与偏好，作为大模型的基线上下文
 */
@Data
@TableName("health_profile")
public class HealthProfile {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private Integer age;

    /** 生理性别: 1男, 2女, 0其他 */
    private Integer gender;

    /** 身高(厘米), 用于BMI基准计算 */
    private BigDecimal heightCm;

    /** 建档初始体重(公斤) */
    private BigDecimal baselineWeight;

    /** 日常活动强度: LOW, MODERATE, HIGH */
    private String activityLevel;

    /** 饮食倾向: KETO, VEGAN, BALANCED */
    private String dietPreference;

    /** 干预目标: FAT_LOSS, MUSCLE_GAIN, MAINTENANCE */
    private String healthGoal;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 本周身高修改次数 */
    private Integer heightUpdateCount;

    /** 上次修改身高的周标识(如 2026-W23) */
    private String lastHeightUpdateWeek;
}
