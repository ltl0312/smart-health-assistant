package com.hnust.health.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeatureSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        for (String sql : statements()) {
            jdbcTemplate.execute(sql);
        }
        for (String sql : migrations()) {
            safeExecute(sql);
        }
        seedArticleCategories();
        seedArticles();
    }

    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Additive startup DDL may be re-run on existing Docker volumes.
        }
    }

    private void seedArticleCategories() {
        String[] categories = {"FAT_LOSS", "MUSCLE_GAIN", "DIET", "EXERCISE", "SLEEP", "GLUCOSE"};
        String[] names = {"减脂", "增肌", "饮食", "运动", "睡眠", "控糖"};
        for (int i = 0; i < categories.length; i++) {
            jdbcTemplate.update("""
                    INSERT INTO article_category(code, name)
                    SELECT ?, ? FROM DUAL
                    WHERE NOT EXISTS (SELECT 1 FROM article_category WHERE code = ?)
                    """, categories[i], names[i], categories[i]);
        }
    }

    private void seedArticles() {
        Object[][] seeds = {
                {"FAT_LOSS", "减脂期如何设置一周热量缺口", "用温和热量缺口换取稳定下降，避免靠极端节食硬撑。", "FAT_LOSS"},
                {"FAT_LOSS", "减脂不是少吃一切，而是先稳住三餐", "三餐稳定能减少夜间加餐和情绪性进食。", "FAT_LOSS"},
                {"FAT_LOSS", "平台期的判断和处理方式", "真正的平台期需要至少两周数据，而不是某一天体重没变。", "FAT_LOSS"},
                {"FAT_LOSS", "外卖减脂的点餐公式", "不会做饭也可以通过点餐结构控制热量。", "FAT_LOSS"},
                {"FAT_LOSS", "减脂期的力量训练为什么重要", "力量训练帮助保留肌肉，让下降的体重更有质量。", "FAT_LOSS"},
                {"MUSCLE_GAIN", "增肌期如何避免只增脂肪", "小幅热量盈余和持续训练进步，是干净增肌的核心。", "MUSCLE_GAIN"},
                {"MUSCLE_GAIN", "蛋白质怎么吃更有效", "总量、分配和食物质量，比单次吃很多更重要。", "MUSCLE_GAIN"},
                {"MUSCLE_GAIN", "新手力量训练的三条底线", "动作标准、渐进加量、恢复充足，优先级高于动作数量。", "MUSCLE_GAIN"},
                {"MUSCLE_GAIN", "增肌期碳水不是敌人", "足够碳水能让训练更有质量，也能保护蛋白质用于修复。", "MUSCLE_GAIN"},
                {"MUSCLE_GAIN", "围度和体重应该怎么一起看", "增肌评估不能只看体重，还要结合围度、照片和训练表现。", "MUSCLE_GAIN"},
                {"DIET", "一餐盘法：不用称重也能吃得均衡", "用蛋白、主食、蔬菜的比例快速搭建一餐。", null},
                {"DIET", "早餐应该优先解决什么问题", "早餐的重点是稳定上午能量，而不是追求复杂。", null},
                {"DIET", "高分饮食打卡的判断标准", "健康饮食不只看低热量，还看结构和可持续性。", null},
                {"DIET", "饮品热量为什么容易被忽略", "奶茶、果汁和酒精经常悄悄抬高总热量。", null},
                {"DIET", "如何安排加餐才不失控", "加餐应该服务于饱腹、训练和营养，而不是随机零食。", null},
                {"EXERCISE", "每周 150 分钟运动怎么拆", "把目标拆成几次小任务，比一次完成更容易坚持。", null},
                {"EXERCISE", "快走为什么适合大多数人", "快走门槛低、恢复快，是建立运动习惯的好入口。", null},
                {"EXERCISE", "力量训练和有氧怎么搭配", "两者不是竞争关系，搭配后效果更完整。", null},
                {"EXERCISE", "运动后酸痛应该怎么处理", "酸痛常见，但尖锐疼痛和关节痛需要区别对待。", null},
                {"EXERCISE", "久坐人群的微运动策略", "把活动插进一天，比等一个完整运动时间更现实。", null},
                {"SLEEP", "睡眠如何影响体重管理", "睡不好会改变饥饿感、恢复和第二天的选择。", null},
                {"SLEEP", "睡前仪式的四个步骤", "稳定的睡前流程能让身体更快进入休息状态。", null},
                {"SLEEP", "午睡应该多长合适", "短午睡能恢复精力，过长可能影响夜间睡眠。", null},
                {"SLEEP", "咖啡因和睡眠的边界", "咖啡因影响时间比很多人想象得更长。", null},
                {"SLEEP", "睡眠评分低时怎么调整训练", "恢复不足时，训练目标应从突破改为维持。", null},
                {"GLUCOSE", "控糖饮食先看主食质量", "控糖不是完全不吃主食，而是选择更稳的主食。", null},
                {"GLUCOSE", "餐后散步的实际价值", "饭后轻活动能帮助餐后血糖更平稳。", null},
                {"GLUCOSE", "隐藏糖常出现在哪里", "看起来不甜的食品，也可能含有不少糖。", null},
                {"GLUCOSE", "控糖也需要足够蛋白质", "蛋白质能帮助稳定饱腹感和餐后反应。", null},
                {"GLUCOSE", "控糖用户的运动优先级", "规律运动能改善胰岛素敏感性，力量训练也很重要。", null}
        };
        for (Object[] seed : seeds) {
            String category = String.valueOf(seed[0]);
            String title = String.valueOf(seed[1]);
            String summary = String.valueOf(seed[2]);
            String goal = seed[3] == null ? null : String.valueOf(seed[3]);
            jdbcTemplate.update("""
                    INSERT INTO health_article(category_code, title, summary, content, target_goal, status)
                    SELECT ?, ?, ?, ?, ?, 'PUBLISHED' FROM DUAL
                    WHERE NOT EXISTS (SELECT 1 FROM health_article WHERE title = ?)
                    """, category, title, summary, articleContent(category, title, summary), goal, title);
        }
    }

    private String articleContent(String category, String title, String summary) {
        String categoryName = switch (category) {
            case "FAT_LOSS" -> "减脂";
            case "MUSCLE_GAIN" -> "增肌";
            case "DIET" -> "饮食";
            case "EXERCISE" -> "运动";
            case "SLEEP" -> "睡眠";
            case "GLUCOSE" -> "控糖";
            default -> "健康管理";
        };
        return """
                【核心结论】
                %s

                【为什么重要】
                %s 管理最怕只看一天的结果。体重、食欲、训练表现、睡眠和压力都会互相影响，所以这条建议要放进一周周期里观察。只要大多数天的选择稳定，偶尔一餐或一天的波动不会破坏长期趋势。

                【具体做法】
                1. 先记录当前状态：体重、饮水、运动、饮食和睡眠至少连续记录 7 天。
                2. 找到最容易改变的一件事，例如减少含糖饮品、增加餐后步行、补足蛋白质或固定睡前时间。
                3. 每周只调整一个变量，观察 7 到 14 天，再决定是否继续加码。
                4. 把目标拆小：一次正餐、一杯水、一次 20 分钟运动，都会让系统数据更可靠。

                【执行提醒】
                不建议用极端方式追求快速结果。若出现明显不适、持续疼痛、异常体重波动或血糖相关风险，应优先咨询专业人士。SmartHealth 的价值是帮你把日常行为变成可回顾的数据，而不是替代医疗判断。

                【本周行动】
                选择一个最小动作执行三天：记录一餐、饭后散步 10 分钟、睡前提前 20 分钟放下屏幕，或把每日饮水目标拆成上午、下午、晚上三段完成。
                """.formatted(summary, categoryName);
    }

    private String[] migrations() {
        return new String[]{
                "ALTER TABLE ai_plan ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'APPROVED'",
                "ALTER TABLE user_reminder ADD COLUMN reminder_key VARCHAR(128) DEFAULT NULL",
                "ALTER TABLE user_reminder ADD COLUMN group_type VARCHAR(32) NOT NULL DEFAULT 'TODAY'",
                "ALTER TABLE user_reminder ADD COLUMN action_view VARCHAR(32) DEFAULT NULL",
                "UPDATE ai_plan SET cycle_start_date = DATE_SUB(cycle_start_date, INTERVAL WEEKDAY(cycle_start_date) DAY) WHERE cycle_start_date IS NOT NULL",
                "CREATE UNIQUE INDEX uk_user_reminder_key ON user_reminder(user_id, reminder_key)"
        };
    }

    private String[] statements() {
        return new String[]{
                """
                CREATE TABLE IF NOT EXISTS health_goal (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  goal_type VARCHAR(32) NOT NULL DEFAULT 'MAINTENANCE',
                  target_weight DECIMAL(5,2) DEFAULT NULL,
                  daily_water_ml INT NOT NULL DEFAULT 2000,
                  weekly_exercise_times INT NOT NULL DEFAULT 3,
                  weekly_exercise_minutes INT NOT NULL DEFAULT 150,
                  target_date DATE DEFAULT NULL,
                  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  KEY idx_goal_user_status(user_id, status),
                  CONSTRAINT fk_goal_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS nutrition_estimate (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  food_name VARCHAR(128) NOT NULL,
                  amount VARCHAR(64) DEFAULT NULL,
                  calories INT NOT NULL DEFAULT 0,
                  protein_g DECIMAL(6,2) NOT NULL DEFAULT 0,
                  carbs_g DECIMAL(6,2) NOT NULL DEFAULT 0,
                  fat_g DECIMAL(6,2) NOT NULL DEFAULT 0,
                  health_score INT NOT NULL DEFAULT 60,
                  source VARCHAR(32) NOT NULL DEFAULT 'LOCAL_RULE',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  KEY idx_nutrition_user_created(user_id, created_at DESC),
                  CONSTRAINT fk_nutrition_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS exercise_estimate (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  exercise_type VARCHAR(64) NOT NULL,
                  duration_min INT NOT NULL,
                  weight_kg DECIMAL(5,2) DEFAULT NULL,
                  calories INT NOT NULL DEFAULT 0,
                  intensity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
                  source VARCHAR(32) NOT NULL DEFAULT 'LOCAL_RULE',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  KEY idx_exercise_user_created(user_id, created_at DESC),
                  CONSTRAINT fk_exercise_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS health_alert (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  alert_type VARCHAR(32) NOT NULL,
                  title VARCHAR(128) NOT NULL,
                  message VARCHAR(512) NOT NULL,
                  severity VARCHAR(16) NOT NULL DEFAULT 'INFO',
                  alert_key VARCHAR(128) NOT NULL,
                  is_read TINYINT(1) NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  read_at DATETIME DEFAULT NULL,
                  PRIMARY KEY(id),
                  UNIQUE KEY uk_alert_key(user_id, alert_key),
                  KEY idx_alert_user_read(user_id, is_read, created_at DESC),
                  CONSTRAINT fk_alert_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS user_reminder (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  reminder_type VARCHAR(32) NOT NULL,
                  reminder_key VARCHAR(128) DEFAULT NULL,
                  title VARCHAR(128) NOT NULL,
                  message VARCHAR(512) NOT NULL,
                  group_type VARCHAR(32) NOT NULL DEFAULT 'TODAY',
                  action_view VARCHAR(32) DEFAULT NULL,
                  due_at DATETIME DEFAULT NULL,
                  is_done TINYINT(1) NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  UNIQUE KEY uk_user_reminder_key(user_id, reminder_key),
                  KEY idx_reminder_user_done(user_id, is_done, due_at),
                  CONSTRAINT fk_reminder_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS plan_day (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  plan_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  plan_date DATE NOT NULL,
                  weekday TINYINT NOT NULL,
                  focus VARCHAR(128) DEFAULT NULL,
                  PRIMARY KEY(id),
                  UNIQUE KEY uk_plan_day(plan_id, plan_date),
                  KEY idx_plan_day_user(user_id, plan_date),
                  CONSTRAINT fk_plan_day_plan FOREIGN KEY(plan_id) REFERENCES ai_plan(id) ON DELETE CASCADE,
                  CONSTRAINT fk_plan_day_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS plan_item (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  plan_day_id BIGINT NOT NULL,
                  plan_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  item_type VARCHAR(16) NOT NULL,
                  meal_type VARCHAR(16) DEFAULT NULL,
                  title VARCHAR(128) NOT NULL,
                  description VARCHAR(512) DEFAULT NULL,
                  calories INT DEFAULT NULL,
                  duration_min INT DEFAULT NULL,
                  intensity VARCHAR(16) DEFAULT NULL,
                  sort_order INT NOT NULL DEFAULT 0,
                  PRIMARY KEY(id),
                  KEY idx_plan_item_plan(plan_id, sort_order),
                  CONSTRAINT fk_plan_item_day FOREIGN KEY(plan_day_id) REFERENCES plan_day(id) ON DELETE CASCADE,
                  CONSTRAINT fk_plan_item_plan FOREIGN KEY(plan_id) REFERENCES ai_plan(id) ON DELETE CASCADE,
                  CONSTRAINT fk_plan_item_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS plan_execution (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  plan_item_id BIGINT NOT NULL,
                  plan_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                  note VARCHAR(255) DEFAULT NULL,
                  checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  UNIQUE KEY uk_plan_execution(user_id, plan_item_id),
                  KEY idx_plan_execution_plan(user_id, plan_id, status),
                  CONSTRAINT fk_execution_item FOREIGN KEY(plan_item_id) REFERENCES plan_item(id) ON DELETE CASCADE,
                  CONSTRAINT fk_execution_plan FOREIGN KEY(plan_id) REFERENCES ai_plan(id) ON DELETE CASCADE,
                  CONSTRAINT fk_execution_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS weekly_review (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  user_id BIGINT NOT NULL,
                  week_code VARCHAR(8) NOT NULL,
                  summary TEXT NOT NULL,
                  good_points TEXT DEFAULT NULL,
                  risks TEXT DEFAULT NULL,
                  next_suggestions TEXT DEFAULT NULL,
                  source VARCHAR(32) NOT NULL DEFAULT 'LOCAL_RULE',
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  UNIQUE KEY uk_weekly_review(user_id, week_code),
                  CONSTRAINT fk_review_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS article_category (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  code VARCHAR(32) NOT NULL,
                  name VARCHAR(64) NOT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  UNIQUE KEY uk_article_category_code(code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS health_article (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  category_code VARCHAR(32) NOT NULL,
                  title VARCHAR(160) NOT NULL,
                  summary VARCHAR(512) DEFAULT NULL,
                  content TEXT NOT NULL,
                  cover_url VARCHAR(512) DEFAULT NULL,
                  target_goal VARCHAR(32) DEFAULT NULL,
                  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
                  author_id BIGINT DEFAULT NULL,
                  view_count INT NOT NULL DEFAULT 0,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  KEY idx_article_status_category(status, category_code),
                  CONSTRAINT fk_article_author FOREIGN KEY(author_id) REFERENCES sys_user(id) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS article_view_log (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  article_id BIGINT NOT NULL,
                  user_id BIGINT DEFAULT NULL,
                  viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  KEY idx_article_view(article_id, viewed_at DESC),
                  CONSTRAINT fk_article_view_article FOREIGN KEY(article_id) REFERENCES health_article(id) ON DELETE CASCADE,
                  CONSTRAINT fk_article_view_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                """
                CREATE TABLE IF NOT EXISTS ai_call_log (
                  id BIGINT NOT NULL AUTO_INCREMENT,
                  user_id BIGINT DEFAULT NULL,
                  feature VARCHAR(64) NOT NULL,
                  status VARCHAR(16) NOT NULL,
                  message VARCHAR(512) DEFAULT NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY(id),
                  KEY idx_ai_call_created(feature, created_at DESC),
                  CONSTRAINT fk_ai_call_user FOREIGN KEY(user_id) REFERENCES sys_user(id) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """
        };
    }
}
