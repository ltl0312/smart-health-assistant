package com.hnust.health.constant;

/**
 * 系统静态常量统一定义
 */
public class Constants {

    /** JWT Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 请求属性中存放 userId 的键 */
    public static final String REQUEST_ATTR_USER_ID = "userId";

    /** 默认拉取历史体重记录的天数 */
    public static final int DEFAULT_WEIGHT_HISTORY_DAYS = 30;

    /** 请求属性中存放 role 的键 */
    public static final String REQUEST_ATTR_ROLE = "role";

    /** 健康标准 BMI 值 */
    public static final double STANDARD_BMI = 22.0;

    /** 为大模型构建记忆上下文时提取的最大记录条数 */
    public static final int MEMORY_CONTEXT_MAX_RECORDS = 4;

    /** 活动水平枚举 */
    public static class ActivityLevel {
        public static final String LOW = "LOW";
        public static final String MODERATE = "MODERATE";
        public static final String HIGH = "HIGH";
    }

    /** 饮食偏好枚举 */
    public static class DietPreference {
        public static final String KETO = "KETO";
        public static final String VEGAN = "VEGAN";
        public static final String BALANCED = "BALANCED";
    }

    /** 健康目标枚举 */
    public static class HealthGoal {
        public static final String FAT_LOSS = "FAT_LOSS";
        public static final String MUSCLE_GAIN = "MUSCLE_GAIN";
        public static final String MAINTENANCE = "MAINTENANCE";
    }
}
