package com.hnust.health.service.impl;

import com.hnust.health.dto.HealthRankResponse;
import com.hnust.health.mapper.DailyCheckinMapper;
import com.hnust.health.mapper.WeightRecordMapper;
import com.hnust.health.model.DailyCheckin;
import com.hnust.health.model.WeightRecord;
import com.hnust.health.service.RankService;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.mapper.HealthProfileMapper;
import com.hnust.health.model.SysUser;
import com.hnust.health.model.HealthProfile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.hnust.health.constant.Constants.STANDARD_BMI;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final SysUserMapper sysUserMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final DailyCheckinMapper dailyCheckinMapper;

    @Override
    @Cacheable(value = "healthRank", key = "#period")
    public List<HealthRankResponse> getHealthRanking(String period) {
        int days = "monthly".equals(period) ? 30 : 7;
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        List<HealthRankResponse> rankings = new ArrayList<>();

        for (SysUser user : users) {
            HealthProfile profile = healthProfileMapper.selectById(user.getId());
            if (profile == null) continue;

            List<WeightRecord> records = weightRecordMapper.selectByUserIdAndDays(user.getId(), days);
            int consecutiveWeeks = calcConsecutiveWeeks(records);
            BigDecimal latestBmi = records.isEmpty() ? null : records.get(records.size() - 1).getCalculatedBmi();

            int baseScore = 100;
            int weekBonus = consecutiveWeeks * 2;
            int bmiPenalty = 0;
            if (latestBmi != null) bmiPenalty = (int) (Math.abs(latestBmi.doubleValue() - STANDARD_BMI) * 5);
            int goalBonus = calcGoalBonus(profile.getHealthGoal(), records);
            int checkinBonus = calcCheckinBonus(user.getId(), days);
            int score = Math.max(0, Math.min(200, baseScore + weekBonus - bmiPenalty + goalBonus + checkinBonus));

            BigDecimal weightChange = records.size() >= 2
                    ? records.get(records.size() - 1).getCurrentWeight().subtract(records.get(0).getCurrentWeight())
                    : BigDecimal.ZERO;

            String nickname = user.getNickname() != null ? user.getNickname() : user.getUsername();
            rankings.add(new HealthRankResponse(0, user.getId(), nickname, user.getAvatarUrl(),
                    score, latestBmi, weightChange, consecutiveWeeks,
                    baseScore, weekBonus, bmiPenalty, goalBonus, checkinBonus));
        }

        rankings.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        for (int i = 0; i < rankings.size(); i++) rankings.get(i).setRank(i + 1);
        return rankings;
    }

    private int calcConsecutiveWeeks(List<WeightRecord> records) {
        if (records.isEmpty()) return 0;
        Set<String> weeks = new HashSet<>();
        for (WeightRecord r : records) {
            LocalDate d = r.getRecordDate();
            weeks.add(d.getYear() + "-" + d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
        }
        return weeks.size();
    }

    private int calcGoalBonus(String goal, List<WeightRecord> records) {
        if (records.size() < 2) return 0;
        BigDecimal change = records.get(records.size() - 1).getCurrentWeight().subtract(records.get(0).getCurrentWeight());
        if ("FAT_LOSS".equals(goal) && change.compareTo(BigDecimal.ZERO) < 0) return 10;
        if ("MUSCLE_GAIN".equals(goal) && change.compareTo(BigDecimal.ZERO) > 0) return 10;
        if ("MAINTENANCE".equals(goal) && change.abs().compareTo(new BigDecimal("0.5")) <= 0) return 10;
        return 0;
    }

    /**
     * 计算打卡质量加分
     * 每日得分 = 餐次种类数(最多3) + 运动(2分) + 饮水≥5杯(1分)
     * 任意一天同时有饮食+运动额外+3分
     */
    private int calcCheckinBonus(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        List<DailyCheckin> checkins = dailyCheckinMapper.selectByUserIdAndDateRange(userId, startDate, endDate);
        if (checkins.isEmpty()) return 0;

        Map<LocalDate, List<DailyCheckin>> byDate = checkins.stream()
                .collect(Collectors.groupingBy(DailyCheckin::getRecordDate));

        int totalBonus = 0;
        boolean hasCombinedDay = false;

        for (Map.Entry<LocalDate, List<DailyCheckin>> entry : byDate.entrySet()) {
            List<DailyCheckin> dayCheckins = entry.getValue();
            Set<String> mealTypes = new HashSet<>();
            int mealHealthTotal = 0;
            boolean hasExercise = false;
            int waterCups = 0;

            for (DailyCheckin c : dayCheckins) {
                switch (c.getCheckinType()) {
                    case "MEAL" -> {
                        if (c.getMealType() != null) mealTypes.add(c.getMealType());
                        mealHealthTotal += c.getHealthScore() != null ? c.getHealthScore() : 0;
                    }
                    case "EXERCISE" -> hasExercise = true;
                    case "WATER" -> waterCups += c.getWaterCups() != null ? c.getWaterCups() : 0;
                }
            }

            // 饮食分 = 健康评分总和 (可为负), 保底 -3
            totalBonus += Math.max(-3, mealHealthTotal);
            totalBonus += hasExercise ? 2 : 0;             // 运动加分
            totalBonus += (waterCups >= 5) ? 1 : 0;        // 饮水达标

            if (!mealTypes.isEmpty() && hasExercise) hasCombinedDay = true;
        }

        if (hasCombinedDay) totalBonus += 3; // 饮食+运动组合加成
        log.debug("用户{}打卡加分: {} ({}天有记录)", userId, totalBonus, byDate.size());
        return totalBonus;
    }
}
