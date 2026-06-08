package com.hnust.health.service;

import com.hnust.health.dto.HealthRankResponse;
import java.util.List;

public interface RankService {
    List<HealthRankResponse> getHealthRanking(String period);
}
