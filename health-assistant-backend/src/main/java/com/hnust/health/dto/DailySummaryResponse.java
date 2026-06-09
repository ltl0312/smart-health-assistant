package com.hnust.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class DailySummaryResponse {
    private List<CheckinRecordResponse> meals;
    private List<CheckinRecordResponse> drinks;
    private List<CheckinRecordResponse> exercises;
    private int waterCups;
    private int waterMl;
    private boolean hasCompleteMeals;    // 早+午+晚全部记录
    private boolean hasDietAndExercise;  // 同时有饮食和运动
}
