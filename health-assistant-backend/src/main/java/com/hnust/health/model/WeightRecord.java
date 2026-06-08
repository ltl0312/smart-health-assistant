package com.hnust.health.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时序体重追踪记录表 - 记录高频波动的体重数据
 * 系统的核心"记忆"数据源，用于计算周际变化率，驱动AI计划的自适应调整
 */
@Data
@TableName("weight_record")
public class WeightRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 记录归属日期 */
    private LocalDate recordDate;

    /** 当日测量体重(公斤) */
    private BigDecimal currentWeight;

    /** 系统后台自动计算的当日BMI指数 */
    private BigDecimal calculatedBmi;

    private LocalDateTime createdAt;

    /** 当日修改次数（限制2次） */
    private Integer updateCount;
}
