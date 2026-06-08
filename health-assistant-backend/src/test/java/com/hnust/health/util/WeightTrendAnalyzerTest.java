package com.hnust.health.util;

import com.hnust.health.model.WeightRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeightTrendAnalyzerTest {

    @Test
    void analyze_shouldHandleEmpty() {
        var r = WeightTrendAnalyzer.analyze(Collections.emptyList());
        assertEquals(0, r.recordCount());
        assertNull(r.startWeight());
    }

    @Test
    void analyze_shouldHandleSingleRecord() {
        var rec = new WeightRecord();
        rec.setCurrentWeight(new BigDecimal("80.0"));
        rec.setRecordDate(LocalDate.now());
        var r = WeightTrendAnalyzer.analyze(List.of(rec));
        assertEquals(1, r.recordCount());
        assertEquals(new BigDecimal("80.0"), r.startWeight());
    }

    @Test
    void analyze_shouldCalculateTrend() {
        var r1 = new WeightRecord(); r1.setCurrentWeight(new BigDecimal("80.0")); r1.setRecordDate(LocalDate.of(2026, 6, 1));
        var r2 = new WeightRecord(); r2.setCurrentWeight(new BigDecimal("78.0")); r2.setRecordDate(LocalDate.of(2026, 6, 8));
        var r = WeightTrendAnalyzer.analyze(List.of(r1, r2));

        assertEquals(2, r.recordCount());
        assertEquals(new BigDecimal("-2.0"), r.totalChange());
        assertEquals(new BigDecimal("-2.0000"), r.weeklyChangeRate()); // (-2/7)*7 = -2
    }
}
