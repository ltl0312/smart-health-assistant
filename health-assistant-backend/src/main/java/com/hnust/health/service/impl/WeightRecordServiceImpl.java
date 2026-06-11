package com.hnust.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnust.health.dto.WeightHistoryResponse;
import com.hnust.health.dto.WeightRecordRequest;
import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.HealthProfileMapper;
import com.hnust.health.mapper.WeightRecordMapper;
import com.hnust.health.model.HealthProfile;
import com.hnust.health.model.WeightRecord;
import com.hnust.health.service.WeightRecordService;
import com.hnust.health.util.BmiCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeightRecordServiceImpl implements WeightRecordService {

    private final WeightRecordMapper weightRecordMapper;
    private final HealthProfileMapper healthProfileMapper;

    @Override
    public void recordWeight(Long userId, WeightRecordRequest request) {
        HealthProfile profile = healthProfileMapper.selectById(userId);
        if (profile == null) throw new BusinessException(400, "请先创建健康档案");

        validateRecordWeek(userId, request.getRecordDate());

        BigDecimal bmi = BmiCalculator.calculate(profile.getHeightCm(), request.getCurrentWeight());

        WeightRecord record = new WeightRecord();
        record.setUserId(userId);
        record.setRecordDate(request.getRecordDate());
        record.setCurrentWeight(request.getCurrentWeight());
        record.setCalculatedBmi(bmi);
        record.setUpdateCount(0);

        try {
            weightRecordMapper.insert(record);
        } catch (Exception e) {
            throw new BusinessException(400, "该日期体重记录已存在");
        }
    }

    @Override
    public void updateWeight(Long userId, WeightRecordRequest request) {
        // 查找今日记录
        LocalDate today = request.getRecordDate();
        validateAllowedWeek(today);
        List<WeightRecord> list = weightRecordMapper.selectList(
                new LambdaQueryWrapper<WeightRecord>()
                        .eq(WeightRecord::getUserId, userId)
                        .eq(WeightRecord::getRecordDate, today));

        if (list.isEmpty()) {
            throw new BusinessException(404, "该日期无体重记录，请先记录");
        }

        WeightRecord record = list.get(0);
        int count = (record.getUpdateCount() != null) ? record.getUpdateCount() : 0;
        if (count >= 2) {
            throw new BusinessException(400, "今日修改次数已用完（2次），请明天再试");
        }

        HealthProfile profile = healthProfileMapper.selectById(userId);
        BigDecimal bmi = BmiCalculator.calculate(profile.getHeightCm(), request.getCurrentWeight());

        record.setCurrentWeight(request.getCurrentWeight());
        record.setCalculatedBmi(bmi);
        record.setUpdateCount(count + 1);
        weightRecordMapper.updateById(record);
    }

    @Override
    public List<WeightHistoryResponse> getWeightHistory(Long userId, int days) {
        if (days <= 0) days = 30;
        List<WeightRecord> records = weightRecordMapper.selectByUserIdAndDays(userId, days);
        return records.stream()
                .map(r -> new WeightHistoryResponse(r.getRecordDate(), r.getCurrentWeight(), r.getCalculatedBmi(), r.getUpdateCount()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getWeeklyStatus(Long userId) {
        LocalDate currentStart = weekStart(LocalDate.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentWeekStart", currentStart.toString());
        result.put("currentWeekEnd", currentStart.plusDays(6).toString());
        List<WeightRecord> currentRecords = recordsForWeek(userId, currentStart);
        result.put("currentWeekRecorded", !currentRecords.isEmpty());
        if (!currentRecords.isEmpty()) {
            WeightRecord record = currentRecords.get(0);
            result.put("currentWeekRecordDate", record.getRecordDate().toString());
            result.put("currentWeekWeight", record.getCurrentWeight());
        }

        List<Map<String, Object>> backfillableWeeks = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            LocalDate start = currentStart.minusWeeks(i);
            List<WeightRecord> records = recordsForWeek(userId, start);
            Map<String, Object> week = new LinkedHashMap<>();
            week.put("weekStart", start.toString());
            week.put("weekEnd", start.plusDays(6).toString());
            week.put("recorded", !records.isEmpty());
            week.put("label", start.getMonthValue() + "." + start.getDayOfMonth() + " - "
                    + start.plusDays(6).getMonthValue() + "." + start.plusDays(6).getDayOfMonth());
            if (!records.isEmpty()) {
                week.put("recordDate", records.get(0).getRecordDate().toString());
                week.put("weight", records.get(0).getCurrentWeight());
            }
            backfillableWeeks.add(week);
        }
        result.put("backfillableWeeks", backfillableWeeks);
        return result;
    }

    private void validateRecordWeek(Long userId, LocalDate recordDate) {
        validateAllowedWeek(recordDate);
        LocalDate start = weekStart(recordDate);
        List<WeightRecord> records = recordsForWeek(userId, start);
        if (!records.isEmpty()) {
            LocalDate currentStart = weekStart(LocalDate.now());
            if (start.equals(currentStart)) {
                throw new BusinessException(400, "本周体重已记录");
            }
            throw new BusinessException(400, "该周体重已记录");
        }
    }

    private void validateAllowedWeek(LocalDate recordDate) {
        if (recordDate == null) {
            throw new BusinessException(400, "记录日期不能为空");
        }
        LocalDate today = LocalDate.now();
        if (recordDate.isAfter(today)) {
            throw new BusinessException(400, "不能记录未来日期");
        }
        LocalDate currentStart = weekStart(today);
        LocalDate recordStart = weekStart(recordDate);
        if (recordStart.isBefore(currentStart.minusWeeks(2)) || recordStart.isAfter(currentStart)) {
            throw new BusinessException(400, "只能记录本周或补录前两周体重");
        }
    }

    private List<WeightRecord> recordsForWeek(Long userId, LocalDate weekStart) {
        return weightRecordMapper.selectList(new LambdaQueryWrapper<WeightRecord>()
                .eq(WeightRecord::getUserId, userId)
                .ge(WeightRecord::getRecordDate, weekStart)
                .le(WeightRecord::getRecordDate, weekStart.plusDays(6))
                .orderByAsc(WeightRecord::getRecordDate));
    }

    private LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }
}
