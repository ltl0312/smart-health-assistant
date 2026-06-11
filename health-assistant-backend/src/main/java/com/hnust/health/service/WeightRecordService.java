package com.hnust.health.service;

import com.hnust.health.dto.WeightHistoryResponse;
import com.hnust.health.dto.WeightRecordRequest;
import java.util.List;
import java.util.Map;

public interface WeightRecordService {
    void recordWeight(Long userId, WeightRecordRequest request);
    void updateWeight(Long userId, WeightRecordRequest request);
    List<WeightHistoryResponse> getWeightHistory(Long userId, int days);
    Map<String, Object> getWeeklyStatus(Long userId);
}
