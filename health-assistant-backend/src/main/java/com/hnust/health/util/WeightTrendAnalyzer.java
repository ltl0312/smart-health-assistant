package com.hnust.health.util;

import com.hnust.health.model.WeightRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 体重趋势分析工具类
 * 计算历史体重数据的变化率（一阶导数/斜率）
 * 用于构建大模型的"记忆"上下文
 */
public class WeightTrendAnalyzer {

    /**
     * 体重趋势分析结果
     */
    public record TrendResult(
            BigDecimal startWeight,
            BigDecimal endWeight,
            BigDecimal totalChange,
            BigDecimal weeklyChangeRate,
            int recordCount,
            String trendDescription
    ) {}

    /**
     * 分析体重变化趋势
     *
     * @param records 按日期升序排列的体重记录列表
     * @return 趋势分析结果
     */
    public static TrendResult analyze(List<WeightRecord> records) {
        if (records == null || records.isEmpty()) {
            return new TrendResult(null, null, BigDecimal.ZERO, BigDecimal.ZERO, 0, "暂无体重数据");
        }

        // 按日期升序排列，确保第一条为最早记录
        records.sort((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()));

        if (records.size() == 1) {
            WeightRecord single = records.get(0);
            return new TrendResult(
                    single.getCurrentWeight(),
                    single.getCurrentWeight(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    1,
                    "仅有单次记录，无法计算趋势"
            );
        }

        WeightRecord first = records.get(0);
        WeightRecord last = records.get(records.size() - 1);

        BigDecimal totalChange = last.getCurrentWeight().subtract(first.getCurrentWeight());

        // 计算周均变化率：（总变化量 / 天数）* 7
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(first.getRecordDate(), last.getRecordDate());
        if (daysBetween <= 0) {
            daysBetween = 1;
        }

        BigDecimal weeklyChangeRate = totalChange
                .multiply(new BigDecimal("7"))
                .divide(new BigDecimal(daysBetween), 4, RoundingMode.HALF_UP);

        // 生成趋势描述
        String description = buildTrendDescription(totalChange, weeklyChangeRate, records.size());

        return new TrendResult(
                first.getCurrentWeight(),
                last.getCurrentWeight(),
                totalChange,
                weeklyChangeRate,
                records.size(),
                description
        );
    }

    private static String buildTrendDescription(BigDecimal totalChange, BigDecimal weeklyRate, int count) {
        StringBuilder sb = new StringBuilder();

        String direction;
        if (totalChange.compareTo(BigDecimal.ZERO) < 0) {
            direction = "下降";
        } else if (totalChange.compareTo(BigDecimal.ZERO) > 0) {
            direction = "上升";
        } else {
            direction = "持平";
        }

        sb.append("基于近").append(count).append("条体重记录分析：体重总体呈").append(direction).append("趋势");
        sb.append("，总变化量为").append(totalChange.abs()).append("kg");
        sb.append("，周均变化率约为").append(weeklyRate.abs()).append("kg/周。");

        // 解读
        BigDecimal weeklyAbs = weeklyRate.abs();
        if (weeklyAbs.compareTo(new BigDecimal("0.1")) < 0) {
            sb.append("当前体重变化极为缓慢，可能处于平台期，需调整干预方案。");
        } else if (weeklyAbs.compareTo(new BigDecimal("0.5")) < 0) {
            sb.append("体重变化处于温和区间。");
        } else {
            sb.append("体重变化较快，请关注是否在健康范围内。");
        }

        return sb.toString();
    }
}
