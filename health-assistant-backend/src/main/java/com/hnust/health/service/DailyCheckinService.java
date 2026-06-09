package com.hnust.health.service;

import com.hnust.health.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface DailyCheckinService {
    CheckinRecordResponse createCheckin(Long userId, CheckinRequest request);
    void deleteCheckin(Long userId, Long checkinId);
    List<CheckinRecordResponse> getTodayCheckins(Long userId, String typeFilter);
    DailySummaryResponse getDailySummary(Long userId, LocalDate date);
    WaterSummaryResponse getWaterSummary(Long userId, LocalDate date);
    CheckinRecordResponse addWaterCup(Long userId, LocalDate date);
    List<CheckinRecordResponse> getCheckinsByDateRange(Long userId, LocalDate start, LocalDate end);
}
