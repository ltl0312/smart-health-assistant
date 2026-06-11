package com.hnust.health.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 大模型干预计划生成表 - 存储DeepSeek V4 Pro生成的复杂结构化数据及推理思维链记录
 */
@Data
@TableName("ai_plan")
public class AiPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 本干预周期的起始日期 */
    private LocalDate cycleStartDate;

    /** 生成时注入大模型的历史体重波动特征快照(JSON) */
    private String memoryContextSnapshot;

    /** DeepSeek生成的严格结构化饮食处方(JSON) */
    private String dietPlanJson;

    /** DeepSeek生成的严格结构化运动处方(JSON) */
    private String workoutPlanJson;

    /** DeepSeek的高阶思维链推理过程记录 */
    private String llmReasoningChain;

    /** PENDING_REVIEW / APPROVED / REJECTED */
    private String status;

    private LocalDateTime createdAt;
}
