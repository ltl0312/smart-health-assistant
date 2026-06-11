-- ============================================================
-- 智能健康助手 (Smart Health Assistant) 数据库初始化脚本 v3
-- 引擎: InnoDB | 字符集: utf8mb4 | 排序: utf8mb4_unicode_ci
-- ============================================================

CREATE DATABASE IF NOT EXISTS `smart_health_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `smart_health_db`;

-- -----------------------------------------------------------
-- 1. 系统用户基表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分布式主键ID',
  `username`      VARCHAR(64)  NOT NULL                COMMENT '登录账户名',
  `password_hash` VARCHAR(255) NOT NULL                COMMENT 'BCrypt加密密码',
  `email`         VARCHAR(128) DEFAULT NULL            COMMENT '联络邮箱',
  `status`        TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '状态: 1正常, 0封禁',
  `avatar_url`    VARCHAR(512) DEFAULT NULL            COMMENT '头像链接',
  `phone`         VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `nickname`      VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  `bio`           VARCHAR(255) DEFAULT NULL            COMMENT '个人简介',
  `role`          VARCHAR(32)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核心用户鉴权表';

-- -----------------------------------------------------------
-- 2. 健康生理档案表 (1:1 关联 sys_user)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `health_profile` (
  `user_id`                BIGINT        NOT NULL COMMENT '关联 sys_user.id',
  `age`                    INT           NOT NULL COMMENT '年龄',
  `gender`                 TINYINT(1)    NOT NULL COMMENT '性别: 1男, 2女, 0其他',
  `height_cm`              DECIMAL(5,2)  NOT NULL COMMENT '身高(厘米), BMI计算基准',
  `baseline_weight`        DECIMAL(5,2)  NOT NULL COMMENT '建档初始体重(公斤)',
  `activity_level`         VARCHAR(32)   NOT NULL COMMENT '活动强度: LOW/MODERATE/HIGH',
  `diet_preference`        VARCHAR(64)   NOT NULL COMMENT '饮食倾向: KETO/VEGAN/BALANCED',
  `health_goal`            VARCHAR(64)   NOT NULL COMMENT '目标: FAT_LOSS/MUSCLE_GAIN/MAINTENANCE',
  `height_update_count`    INT           NOT NULL DEFAULT 0 COMMENT '本周身高修改次数(周限3次)',
  `last_height_update_week` VARCHAR(8)   DEFAULT NULL COMMENT '上次修改身高的周标识(如2026-W23)',
  `created_at`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_profile_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户健康档案表';

-- -----------------------------------------------------------
-- 3. 时序体重追踪记录表 (1:N 关联 sys_user)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `weight_record` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录流水号',
  `user_id`        BIGINT        NOT NULL COMMENT '所属用户ID',
  `record_date`    DATE          NOT NULL COMMENT '记录归属日期',
  `current_weight` DECIMAL(5,2)  NOT NULL COMMENT '当日测量体重(公斤)',
  `calculated_bmi` DECIMAL(5,2)  DEFAULT NULL COMMENT '系统自动计算BMI',
  `update_count`   INT           NOT NULL DEFAULT 0 COMMENT '当日修改次数(日限2次)',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
  KEY `idx_user_weight_trend` (`user_id`, `record_date` DESC),
  CONSTRAINT `fk_weight_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='时序体重测量记录表';

-- -----------------------------------------------------------
-- 4. AI 干预计划生成表 (1:N 关联 sys_user)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ai_plan` (
  `id`                       BIGINT   NOT NULL AUTO_INCREMENT COMMENT '计划流水号',
  `user_id`                  BIGINT   NOT NULL COMMENT '所属用户ID',
  `cycle_start_date`         DATE     NOT NULL COMMENT '干预周期起始日期',
  `memory_context_snapshot`  JSON     DEFAULT NULL COMMENT '注入大模型的历史体重波动特征快照',
  `diet_plan_json`           JSON     NOT NULL COMMENT 'DeepSeek生成的饮食处方',
  `workout_plan_json`        JSON     NOT NULL COMMENT 'DeepSeek生成的运动处方',
  `llm_reasoning_chain`      TEXT     COMMENT 'AI思维链推理过程记录',
  `status`                   VARCHAR(24) NOT NULL DEFAULT 'APPROVED' COMMENT '计划状态: PENDING_REVIEW/APPROVED/REJECTED',
  `created_at`               DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_cycle` (`user_id`, `cycle_start_date` DESC),
  CONSTRAINT `fk_plan_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI干预计划表';

-- -----------------------------------------------------------
-- 5. 每日打卡记录表 (1:N 关联 sys_user)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `daily_checkin` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '打卡流水号',
  `user_id`         BIGINT       NOT NULL                COMMENT '所属用户ID',
  `record_date`     DATE         NOT NULL                COMMENT '打卡归属日期',
  `checkin_type`    VARCHAR(16)  NOT NULL                COMMENT '类型: MEAL/DRINK/EXERCISE/WATER',
  `meal_type`       VARCHAR(16)  DEFAULT NULL            COMMENT '餐次: BREAKFAST/LUNCH/DINNER/SNACK',
  `food_desc`       VARCHAR(255) DEFAULT NULL            COMMENT '食物描述',
  `food_amount`     VARCHAR(64)  DEFAULT NULL            COMMENT '份量说明',
  `drink_name`      VARCHAR(64)  DEFAULT NULL            COMMENT '饮品名称',
  `drink_volume_ml` INT          DEFAULT NULL            COMMENT '饮用量(毫升)',
  `exercise_type`   VARCHAR(64)  DEFAULT NULL            COMMENT '运动类型',
  `duration_min`    INT          DEFAULT NULL            COMMENT '运动时长(分钟)',
  `water_cups`      INT          DEFAULT NULL            COMMENT '杯数(每杯250ml)',
  `health_score`    INT          NOT NULL DEFAULT 0      COMMENT '饮食健康评分(-2~2)，不吃=-1',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`, `record_date` DESC),
  KEY `idx_user_type_date` (`user_id`, `checkin_type`, `record_date` DESC),
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日打卡记录表';
