package com.hnust.health.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BMI 计算工具类
 * BMI = 体重(kg) / 身高(m)²
 */
public class BmiCalculator {

    /**
     * 根据身高(厘米)和体重(公斤)计算 BMI 值
     */
    public static BigDecimal calculate(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null || heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 将身高从厘米转换为米
        BigDecimal heightM = heightCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        // BMI = 体重 / 身高²
        BigDecimal heightSquared = heightM.multiply(heightM);
        return weightKg.divide(heightSquared, 2, RoundingMode.HALF_UP);
    }
}
