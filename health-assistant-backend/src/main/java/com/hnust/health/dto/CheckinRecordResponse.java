package com.hnust.health.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CheckinRecordResponse {
    private Long id;
    private LocalDate recordDate;
    private String checkinType;
    private String mealType;
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
