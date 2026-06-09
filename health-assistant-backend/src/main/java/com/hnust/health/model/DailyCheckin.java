package com.hnust.health.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_checkin")
public class DailyCheckin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private String checkinType;      // MEAL, DRINK, EXERCISE, WATER
    private String mealType;         // BREAKFAST, LUNCH, DINNER, SNACK
    private String foodDesc;
    private String foodAmount;
    private String drinkName;
    private Integer drinkVolumeMl;
    private String exerciseType;
    private Integer durationMin;
    private Integer waterCups;
    private Integer healthScore;
    private LocalDateTime createdAt;
}
