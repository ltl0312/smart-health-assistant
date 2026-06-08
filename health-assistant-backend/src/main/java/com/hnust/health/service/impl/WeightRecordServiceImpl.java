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
import java.time.LocalDate;
import java.util.List;
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
            throw new BusinessException(400, "该日期已有体重记录，可使用编辑功能修改");
        }
    }

    @Override
    public void updateWeight(Long userId, WeightRecordRequest request) {
        // 查找今日记录
        LocalDate today = request.getRecordDate();
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
                .map(r -> new WeightHistoryResponse(r.getRecordDate(), r.getCurrentWeight(), r.getCalculatedBmi()))
                .collect(Collectors.toList());
    }
}
