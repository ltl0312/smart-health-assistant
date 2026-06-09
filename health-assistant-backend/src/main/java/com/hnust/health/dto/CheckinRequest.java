package com.hnust.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckinRequest {
    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;

    @NotBlank(message = "打卡类型不能为空")
    private String checkinType;          // MEAL, DRINK, EXERCISE, WATER

    private String mealType;             // BREAKFAST/LUNCH/DINNER/SNACK
    private String foodDesc;
    private String foodAmount;
    private String drinkName;
    private Integer drinkVolumeMl;
    private String exerciseType;
    private Integer durationMin;
    private Integer waterCups;
}
