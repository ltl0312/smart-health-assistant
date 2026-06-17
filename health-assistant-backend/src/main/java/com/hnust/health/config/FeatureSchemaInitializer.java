package com.hnust.health.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库后初始化组件。
 *
 * <p>所有建表语句已迁移至 init.sql（Docker MySQL 容器启动时自动执行），
 * 本类仅负责两件事：
 * <ol>
 *   <li><b>数据库升级迁移</b> — 对旧版 Docker 卷执行 additive DDL 和索引修复</li>
 *   <li><b>文章种子数据</b> — 写入 30 篇健康文章（内容由 Java 模板生成）</li>
 * </ol>
 *
 * <p>所有语句均通过 {@code INSERT ... WHERE NOT EXISTS} 或
 * {@code safeExecute} 实现幂等，重复执行安全。
 */
@Component
@RequiredArgsConstructor
public class FeatureSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        // 1. 对已有 Docker 卷执行增量升级（新数据库自动跳过）
        for (String sql : migrations()) {
            safeExecute(sql);
        }

        // 2. 写入文章种子数据（内容由 Java 模板生成）
        //    文章分类种子已移入 init.sql（INSERT IGNORE），此处仅负责文章正文
        seedArticles();
    }

    /**
     * 安全执行 DDL，重复执行不报错。
     */
    private void safeExecute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Additive startup DDL may be re-run on existing Docker volumes.
        }
    }

    // ================================================================
    // 数据库增量升级迁移（对 init.sql 未覆盖的旧版卷修复）
    // ================================================================

    private String[] migrations() {
        return new String[]{
                // 为旧版 ai_plan 表（无 status 列）补齐字段
                "ALTER TABLE ai_plan ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'APPROVED'",

                // 为旧版 user_reminder 表补齐 migration 新增字段
                "ALTER TABLE user_reminder ADD COLUMN reminder_key VARCHAR(128) DEFAULT NULL",
                "ALTER TABLE user_reminder ADD COLUMN group_type VARCHAR(32) NOT NULL DEFAULT 'TODAY'",
                "ALTER TABLE user_reminder ADD COLUMN action_view VARCHAR(32) DEFAULT NULL",

                // 修复旧计划中 cycle_start_date 非周一的数据
                "UPDATE ai_plan SET cycle_start_date = DATE_SUB(cycle_start_date, INTERVAL WEEKDAY(cycle_start_date) DAY) WHERE cycle_start_date IS NOT NULL",

                // 为旧版 user_reminder 表补齐唯一索引
                "CREATE UNIQUE INDEX uk_user_reminder_key ON user_reminder(user_id, reminder_key)"
        };
    }

    // ================================================================
    // 文章种子数据
    // ================================================================

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

    /**
     * 生成文章正文 HTML 模板。
     */
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
                【核心结论】\n%s\n\n\
                【为什么重要】\n%s 管理最怕只看一天的结果。体重、食欲、训练表现、睡眠和压力都会互相影响，\
                所以这条建议要放进一周周期里观察。只要大多数天的选择稳定，偶尔一餐或一天的波动不会破坏长期趋势。\n\n\
                【具体做法】\n1. 先记录当前状态：体重、饮水、运动、饮食和睡眠至少连续记录 7 天。\n\
                2. 找到最容易改变的一件事，例如减少含糖饮品、增加餐后步行、补足蛋白质或固定睡前时间。\n\
                3. 每周只调整一个变量，观察 7 到 14 天，再决定是否继续加码。\n\
                4. 把目标拆小：一次正餐、一杯水、一次 20 分钟运动，都会让系统数据更可靠。\n\n\
                【执行提醒】\n不建议用极端方式追求快速结果。若出现明显不适、持续疼痛、异常体重波动或血糖相关风险，\
                应优先咨询专业人士。SmartHealth 的价值是帮你把日常行为变成可回顾的数据，而不是替代医疗判断。\n\n\
                【本周行动】\n选择一个最小动作执行三天：记录一餐、饭后散步 10 分钟、睡前提前 20 分钟放下屏幕，\
                或把每日饮水目标拆成上午、下午、晚上三段完成。\
                """.formatted(summary, categoryName);
    }
}
