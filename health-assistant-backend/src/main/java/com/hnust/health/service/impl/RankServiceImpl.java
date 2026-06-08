package com.hnust.health.service.impl;

import com.hnust.health.dto.HealthRankResponse;
import com.hnust.health.mapper.WeightRecordMapper;
import com.hnust.health.model.WeightRecord;
import com.hnust.health.service.RankService;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.mapper.HealthProfileMapper;
import com.hnust.health.model.SysUser;
import com.hnust.health.model.HealthProfile;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.hnust.health.constant.Constants.STANDARD_BMI;

@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final SysUserMapper sysUserMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final WeightRecordMapper weightRecordMapper;

    @Override
    @Cacheable(value = "healthRank", key = "'all'")
    public List<HealthRankResponse> getHealthRanking() {
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        List<HealthRankResponse> rankings = new ArrayList<>();

        int rank = 1;
        for (SysUser user : users) {
            HealthProfile profile = healthProfileMapper.selectById(user.getId());
            if (profile == null) continue;

            List<WeightRecord> records = weightRecordMapper.selectByUserIdAndDays(user.getId(), 90);
            int consecutiveWeeks = calcConsecutiveWeeks(records);
            BigDecimal latestBmi = records.isEmpty() ? null : records.get(records.size() - 1).getCalculatedBmi();

            int baseScore = 100;
            int weekBonus = consecutiveWeeks * 2;
            int bmiPenalty = 0;
            if (latestBmi != null) {
                bmiPenalty = (int) (Math.abs(latestBmi.doubleValue() - STANDARD_BMI) * 5);
            }
            int goalBonus = calcGoalBonus(profile.getHealthGoal(), records);

            int score = Math.max(0, Math.min(200, baseScore + weekBonus - bmiPenalty + goalBonus));
            String nickname = user.getNickname() != null ? user.getNickname() : user.getUsername();

            rankings.add(new HealthRankResponse(0, user.getId(), nickname, user.getAvatarUrl(),
                    score, latestBmi, consecutiveWeeks));
        }

        rankings.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }
        return rankings;
    }

    private int calcConsecutiveWeeks(List<WeightRecord> records) {
        if (records.isEmpty()) return 0;
        Set<String> weeks = new HashSet<>();
        for (WeightRecord r : records) {
            LocalDate d = r.getRecordDate();
            int year = d.getYear();
            int week = d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
            weeks.add(year + "-" + week);
        }
        return weeks.size();
    }

    private int calcGoalBonus(String goal, List<WeightRecord> records) {
        if (records.size() < 2) return 0;
        WeightRecord first = records.get(0);
        WeightRecord last = records.get(records.size() - 1);
        BigDecimal change = last.getCurrentWeight().subtract(first.getCurrentWeight());

        if ("FAT_LOSS".equals(goal) && change.compareTo(BigDecimal.ZERO) < 0) return 10;
        if ("MUSCLE_GAIN".equals(goal) && change.compareTo(BigDecimal.ZERO) > 0) return 10;
        if ("MAINTENANCE".equals(goal) && change.abs().compareTo(new BigDecimal("0.5")) <= 0) return 10;
        return 0;
    }
}
