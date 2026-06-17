-- 智能健康助手 (Smart Health Assistant) 数据库初始化脚本 v4
-- 引擎: InnoDB | 字符集: utf8mb4 | 排序: utf8mb4_unicode_ci

CREATE DATABASE IF NOT EXISTS `smart_health_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `smart_health_db`;

-- 第一部分：核心表 (5 张)

-- 1. 系统用户基表
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

-- 2. 健康生理档案表 (1:1 关联 sys_user)
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

-- 3. 时序体重追踪记录表 (1:N 关联 sys_user)
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

-- 4. AI 干预计划生成表 (1:N 关联 sys_user)
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

-- 5. 每日打卡记录表 (1:N 关联 sys_user)
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


-- 第二部分：功能表 (13 张 — 从 FeatureSchemaInitializer 迁移)
-- 原通过 CommandLineRunner 动态创建，现改为 SQL 脚本预创建

-- 6. 健康目标表
CREATE TABLE IF NOT EXISTS `health_goal` (
  `id`                      BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`                 BIGINT       NOT NULL,
  `goal_type`               VARCHAR(32)  NOT NULL DEFAULT 'MAINTENANCE' COMMENT 'FAT_LOSS/MUSCLE_GAIN/MAINTENANCE',
  `target_weight`           DECIMAL(5,2) DEFAULT NULL COMMENT '目标体重(kg)',
  `daily_water_ml`          INT          NOT NULL DEFAULT 2000 COMMENT '每日饮水目标(ml)',
  `weekly_exercise_times`   INT          NOT NULL DEFAULT 3 COMMENT '每周运动次数',
  `weekly_exercise_minutes` INT          NOT NULL DEFAULT 150 COMMENT '每周运动分钟数',
  `target_date`             DATE         DEFAULT NULL COMMENT '目标达成日期',
  `status`                  VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
  `created_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_goal_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_goal_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户健康目标表';

-- 7. 食物营养估算表
CREATE TABLE IF NOT EXISTS `nutrition_estimate` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT        NOT NULL,
  `food_name`    VARCHAR(128)  NOT NULL COMMENT '食物描述',
  `amount`       VARCHAR(64)   DEFAULT NULL COMMENT '份量说明',
  `calories`     INT           NOT NULL DEFAULT 0 COMMENT '估算热量(千卡)',
  `protein_g`    DECIMAL(6,2)  NOT NULL DEFAULT 0 COMMENT '蛋白质(g)',
  `carbs_g`      DECIMAL(6,2)  NOT NULL DEFAULT 0 COMMENT '碳水化合物(g)',
  `fat_g`        DECIMAL(6,2)  NOT NULL DEFAULT 0 COMMENT '脂肪(g)',
  `health_score` INT           NOT NULL DEFAULT 60 COMMENT '健康评分(0~100)',
  `source`       VARCHAR(32)   NOT NULL DEFAULT 'LOCAL_RULE' COMMENT '数据来源: LOCAL_RULE/AI',
  `created_at`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_nutrition_user_created` (`user_id`, `created_at` DESC),
  CONSTRAINT `fk_nutrition_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食物营养估算记录表';

-- 8. 运动消耗估算表
CREATE TABLE IF NOT EXISTS `exercise_estimate` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT       NOT NULL,
  `exercise_type` VARCHAR(64)  NOT NULL COMMENT '运动类型',
  `duration_min`  INT          NOT NULL COMMENT '运动时长(分钟)',
  `weight_kg`     DECIMAL(5,2) DEFAULT NULL COMMENT '估算所用体重(kg)',
  `calories`      INT          NOT NULL DEFAULT 0 COMMENT '估算消耗热量(千卡)',
  `intensity`     VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM' COMMENT '强度: LOW/MEDIUM/HIGH',
  `source`        VARCHAR(32)  NOT NULL DEFAULT 'LOCAL_RULE' COMMENT '数据来源: LOCAL_RULE/AI',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_exercise_user_created` (`user_id`, `created_at` DESC),
  CONSTRAINT `fk_exercise_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运动消耗估算记录表';

-- 9. 健康预警表
CREATE TABLE IF NOT EXISTS `health_alert` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT        NOT NULL,
  `alert_type` VARCHAR(32)   NOT NULL COMMENT '预警类型',
  `title`      VARCHAR(128)  NOT NULL COMMENT '预警标题',
  `message`    VARCHAR(512)  NOT NULL COMMENT '预警详情',
  `severity`   VARCHAR(16)   NOT NULL DEFAULT 'INFO' COMMENT '严重程度: INFO/WARN/ERROR',
  `alert_key`  VARCHAR(128)  NOT NULL COMMENT '去重键',
  `is_read`    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否已读',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at`    DATETIME      DEFAULT NULL COMMENT '已读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alert_key` (`user_id`, `alert_key`),
  KEY `idx_alert_user_read` (`user_id`, `is_read`, `created_at` DESC),
  CONSTRAINT `fk_alert_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康预警表';

-- 10. 用户提醒表
CREATE TABLE IF NOT EXISTS `user_reminder` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT        NOT NULL,
  `reminder_type` VARCHAR(32)   NOT NULL COMMENT '提醒类型: WATER/CHECKIN/WEIGHT/BACKFILL/EXERCISE/PLAN_ITEM/PLAN_REVIEW/RISK',
  `reminder_key`  VARCHAR(128)  DEFAULT NULL COMMENT '去重键(与user_id联合唯一)',
  `title`         VARCHAR(128)  NOT NULL COMMENT '提醒标题',
  `message`       VARCHAR(512)  NOT NULL COMMENT '提醒内容',
  `group_type`    VARCHAR(32)   NOT NULL DEFAULT 'TODAY' COMMENT '分组: TODAY/PLAN/RISK',
  `action_view`   VARCHAR(32)   DEFAULT NULL COMMENT '点击跳转视图名',
  `due_at`        DATETIME      DEFAULT NULL COMMENT '到期时间',
  `is_done`       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否已完成',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_reminder_key` (`user_id`, `reminder_key`),
  KEY `idx_reminder_user_done` (`user_id`, `is_done`, `due_at`),
  CONSTRAINT `fk_reminder_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户提醒事项表';

-- 11. 计划日表
CREATE TABLE IF NOT EXISTS `plan_day` (
  `id`        BIGINT       NOT NULL AUTO_INCREMENT,
  `plan_id`   BIGINT       NOT NULL COMMENT '关联 ai_plan.id',
  `user_id`   BIGINT       NOT NULL COMMENT '冗余用户ID便于查询',
  `plan_date` DATE         NOT NULL COMMENT '计划日期',
  `weekday`   TINYINT      NOT NULL COMMENT '星期几(1-7)',
  `focus`     VARCHAR(128) DEFAULT NULL COMMENT '当日重点',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_day` (`plan_id`, `plan_date`),
  KEY `idx_plan_day_user` (`user_id`, `plan_date`),
  CONSTRAINT `fk_plan_day_plan` FOREIGN KEY (`plan_id`) REFERENCES `ai_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_day_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI计划每日分解表';

-- 12. 计划条目表
CREATE TABLE IF NOT EXISTS `plan_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `plan_day_id` BIGINT        NOT NULL COMMENT '关联 plan_day.id',
  `plan_id`     BIGINT        NOT NULL COMMENT '冗余计划ID便于查询',
  `user_id`     BIGINT        NOT NULL COMMENT '冗余用户ID便于查询',
  `item_type`   VARCHAR(16)   NOT NULL COMMENT '条目类型: MEAL/WORKOUT',
  `meal_type`   VARCHAR(16)   DEFAULT NULL COMMENT '餐次: BREAKFAST/LUNCH/DINNER/SNACK',
  `title`       VARCHAR(128)  NOT NULL COMMENT '条目名称',
  `description` VARCHAR(512)  DEFAULT NULL COMMENT '条目描述',
  `calories`    INT           DEFAULT NULL COMMENT '热量(千卡)',
  `duration_min` INT          DEFAULT NULL COMMENT '时长(分钟)',
  `intensity`   VARCHAR(16)   DEFAULT NULL COMMENT '强度: LOW/MEDIUM/HIGH',
  `sort_order`  INT           NOT NULL DEFAULT 0 COMMENT '排序序号',
  PRIMARY KEY (`id`),
  KEY `idx_plan_item_plan` (`plan_id`, `sort_order`),
  CONSTRAINT `fk_plan_item_day`  FOREIGN KEY (`plan_day_id`) REFERENCES `plan_day` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_item_plan` FOREIGN KEY (`plan_id`)     REFERENCES `ai_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_item_user` FOREIGN KEY (`user_id`)     REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI计划条目明细表';

-- 13. 计划执行表
CREATE TABLE IF NOT EXISTS `plan_execution` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `plan_item_id` BIGINT       NOT NULL COMMENT '关联 plan_item.id',
  `plan_id`      BIGINT       NOT NULL COMMENT '冗余计划ID便于查询',
  `user_id`      BIGINT       NOT NULL COMMENT '冗余用户ID便于查询',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '执行状态: PENDING/DONE/SKIPPED',
  `note`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `checked_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_execution` (`user_id`, `plan_item_id`),
  KEY `idx_plan_execution_plan` (`user_id`, `plan_id`, `status`),
  CONSTRAINT `fk_execution_item` FOREIGN KEY (`plan_item_id`) REFERENCES `plan_item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_execution_plan` FOREIGN KEY (`plan_id`)      REFERENCES `ai_plan` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_execution_user` FOREIGN KEY (`user_id`)      REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计划条目执行打卡表';

-- 14. 每周回顾表
CREATE TABLE IF NOT EXISTS `weekly_review` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`         BIGINT       NOT NULL,
  `week_code`       VARCHAR(8)   NOT NULL COMMENT '周标识(如2026-W24)',
  `summary`         TEXT         NOT NULL COMMENT '周总结',
  `good_points`     TEXT         DEFAULT NULL COMMENT '亮点',
  `risks`           TEXT         DEFAULT NULL COMMENT '风险点',
  `next_suggestions` TEXT        DEFAULT NULL COMMENT '下周建议',
  `source`          VARCHAR(32)  NOT NULL DEFAULT 'LOCAL_RULE' COMMENT '生成来源: LOCAL_RULE/AI',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_weekly_review` (`user_id`, `week_code`),
  CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每周健康回顾表';

-- 15. 文章分类表
CREATE TABLE IF NOT EXISTS `article_category` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `code`       VARCHAR(32)  NOT NULL COMMENT '分类编码',
  `name`       VARCHAR(64)  NOT NULL COMMENT '分类名称',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_category_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康文章分类表';

-- 16. 健康文章表
CREATE TABLE IF NOT EXISTS `health_article` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `category_code` VARCHAR(32)   NOT NULL COMMENT '关联分类编码',
  `title`         VARCHAR(160)  NOT NULL COMMENT '文章标题',
  `summary`       VARCHAR(512)  DEFAULT NULL COMMENT '文章摘要',
  `content`       TEXT          NOT NULL COMMENT '文章正文',
  `cover_url`     VARCHAR(512)  DEFAULT NULL COMMENT '封面图片URL',
  `target_goal`   VARCHAR(32)   DEFAULT NULL COMMENT '关联目标类型(个性化推荐)',
  `status`        VARCHAR(16)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/OFFLINE',
  `author_id`     BIGINT        DEFAULT NULL COMMENT '作者ID(管理员)',
  `view_count`    INT           NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_article_status_category` (`status`, `category_code`),
  CONSTRAINT `fk_article_author` FOREIGN KEY (`author_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康知识文章表';

-- 17. 文章浏览日志表
CREATE TABLE IF NOT EXISTS `article_view_log` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `article_id` BIGINT   NOT NULL COMMENT '关联 health_article.id',
  `user_id`    BIGINT   DEFAULT NULL COMMENT '浏览用户ID',
  `viewed_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_view` (`article_id`, `viewed_at` DESC),
  CONSTRAINT `fk_article_view_article` FOREIGN KEY (`article_id`) REFERENCES `health_article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_article_view_user`    FOREIGN KEY (`user_id`)    REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章浏览日志表';

-- 18. AI 调用日志表
CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       DEFAULT NULL COMMENT '调用用户ID',
  `feature`    VARCHAR(64)  NOT NULL COMMENT '调用功能模块',
  `status`     VARCHAR(16)  NOT NULL COMMENT '调用状态: SUCCESS/FAIL',
  `message`    VARCHAR(512) DEFAULT NULL COMMENT '错误信息或耗时',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_call_created` (`feature`, `created_at` DESC),
  CONSTRAINT `fk_ai_call_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI接口调用日志表';


-- 第三部分：种子数据 (文章分类)

INSERT IGNORE INTO `article_category` (`code`, `name`) VALUES
  ('FAT_LOSS',    '减脂'),
  ('MUSCLE_GAIN', '增肌'),
  ('DIET',        '饮食'),
  ('EXERCISE',    '运动'),
  ('SLEEP',       '睡眠'),
  ('GLUCOSE',     '控糖');