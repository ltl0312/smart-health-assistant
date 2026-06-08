package com.hnust.health.service;

import com.hnust.health.dto.WeightHistoryResponse;
import com.hnust.health.dto.WeightRecordRequest;
import java.util.List;

public interface WeightRecordService {
    void recordWeight(Long userId, WeightRecordRequest request);
    void updateWeight(Long userId, WeightRecordRequest request);
    List<WeightHistoryResponse> getWeightHistory(Long userId, int days);
}
