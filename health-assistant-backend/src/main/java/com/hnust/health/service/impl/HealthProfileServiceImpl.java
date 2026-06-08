package com.hnust.health.service.impl;

import com.hnust.health.dto.ProfileRequest;
import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.HealthProfileMapper;
import com.hnust.health.model.HealthProfile;
import com.hnust.health.service.HealthProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class HealthProfileServiceImpl implements HealthProfileService {

    private final HealthProfileMapper healthProfileMapper;

    @Override
    public void setupProfile(Long userId, ProfileRequest request) {
        HealthProfile profile = new HealthProfile();
        profile.setUserId(userId);
        profile.setAge(request.getAge());
        profile.setGender(request.getGender());
        profile.setHeightCm(request.getHeightCm());
        profile.setBaselineWeight(request.getBaselineWeight());
        profile.setActivityLevel(request.getActivityLevel());
        profile.setDietPreference(request.getDietPreference());
        profile.setHealthGoal(request.getHealthGoal());

        HealthProfile existing = healthProfileMapper.selectById(userId);
        if (existing != null) {
            healthProfileMapper.updateById(profile);
        } else {
            healthProfileMapper.insert(profile);
        }
    }

    @Override
    public HealthProfile getProfile(Long userId) {
        HealthProfile profile = healthProfileMapper.selectById(userId);
        if (profile == null) throw new BusinessException(404, "健康档案尚未创建");
        return profile;
    }

    @Override
    public void updateHeight(Long userId, BigDecimal newHeightCm) {
        HealthProfile profile = getProfile(userId);

        // ISO week: Year-W## (e.g. "2026-W23")
        WeekFields wf = WeekFields.of(Locale.getDefault());
        LocalDate today = LocalDate.now();
        int weekOfYear = today.get(wf.weekOfWeekBasedYear());
        int weekYear = today.get(wf.weekBasedYear());
        String currentWeek = weekYear + "-W" + String.format("%02d", weekOfYear);

        String lastWeek = profile.getLastHeightUpdateWeek();
        int count = (profile.getHeightUpdateCount() != null) ? profile.getHeightUpdateCount() : 0;

        if (!currentWeek.equals(lastWeek)) {
            count = 1; // 新一周重置
        } else {
            count++;
        }

        if (count > 3) {
            throw new BusinessException(400, "本周身高修改次数已用完（3次），请下周一再试");
        }

        profile.setHeightCm(newHeightCm);
        profile.setHeightUpdateCount(count);
        profile.setLastHeightUpdateWeek(currentWeek);
        healthProfileMapper.updateById(profile);
    }
}
