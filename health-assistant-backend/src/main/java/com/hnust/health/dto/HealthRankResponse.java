package com.hnust.health.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class HealthRankResponse {
    private int rank;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private int score;
    private BigDecimal bmi;
    private int consecutiveWeeks;
}
