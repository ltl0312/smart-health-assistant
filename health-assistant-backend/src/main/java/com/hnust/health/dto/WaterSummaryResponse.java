package com.hnust.health.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WaterSummaryResponse {
    private int totalCups;
    private int totalMl;
}
