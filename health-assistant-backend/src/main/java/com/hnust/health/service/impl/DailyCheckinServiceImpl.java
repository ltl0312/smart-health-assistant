package com.hnust.health.service.impl;

import com.hnust.health.dto.*;
import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.DailyCheckinMapper;
import com.hnust.health.model.DailyCheckin;
import com.hnust.health.service.DailyCheckinService;
import com.hnust.health.util.MealHealthScorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyCheckinServiceImpl implements DailyCheckinService {

    private final DailyCheckinMapper dailyCheckinMapper;

    @Override
    @Transactional
    public CheckinRecordResponse createCheckin(Long userId, CheckinRequest request) {
        validateRequest(request);

        DailyCheckin record = new DailyCheckin();
        record.setUserId(userId);
        record.setRecordDate(request.getRecordDate());
        record.setCheckinType(request.getCheckinType().toUpperCase());
        record.setMealType(request.getMealType() != null ? request.getMealType().toUpperCase() : null);
        record.setFoodDesc(request.getFoodDesc());
        record.setFoodAmount(request.getFoodAmount());
        record.setDrinkName(request.getDrinkName());
        record.setDrinkVolumeMl(request.getDrinkVolumeMl());
        record.setExerciseType(request.getExerciseType());
        record.setDurationMin(request.getDurationMin());
        record.setWaterCups(request.getWaterCups());

        // 早/午/晚餐每日限一次，加餐不限
        if ("MEAL".equals(record.getCheckinType()) && record.getMealType() != null
                && !"SNACK".equals(record.getMealType())) {
            List<DailyCheckin> sameMealType = dailyCheckinMapper.selectByUserIdAndDateAndType(userId, record.getRecordDate(), "MEAL");
            boolean exists = sameMealType.stream()
                    .anyMatch(m -> record.getMealType().equals(m.getMealType()));
            if (exists) {
                throw new BusinessException(400, "今日" + mealLabel(record.getMealType()) + "已记录，每天限一次（加餐不限）");
            }
        }

        // 饮食健康评分
        if ("MEAL".equals(record.getCheckinType())) {
            boolean isSkip = record.getFoodDesc() != null && record.getFoodDesc().contains("未进食");
            int score = MealHealthScorer.score(record.getFoodDesc(), record.getMealType(), isSkip);
            record.setHealthScore(score);
            log.info("饮食健康评分: food={}, mealType={}, score={}", record.getFoodDesc(), record.getMealType(), score);
        }

        dailyCheckinMapper.insert(record);
        log.info("打卡记录创建成功: userId={}, type={}, date={}", userId, record.getCheckinType(), record.getRecordDate());
        return toResponse(record);
    }

    @Override
    @Transactional
    public void deleteCheckin(Long userId, Long checkinId) {
        DailyCheckin record = dailyCheckinMapper.selectById(checkinId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "打卡记录不存在");
        }
        dailyCheckinMapper.deleteById(checkinId);
    }

    @Override
    public List<CheckinRecordResponse> getTodayCheckins(Long userId, String typeFilter) {
        List<DailyCheckin> records;
        LocalDate today = LocalDate.now();
        if (typeFilter != null && !typeFilter.isBlank()) {
            records = dailyCheckinMapper.selectByUserIdAndDateAndType(userId, today, typeFilter.toUpperCase());
        } else {
            records = dailyCheckinMapper.selectByUserIdAndDate(userId, today);
        }
        return records.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public DailySummaryResponse getDailySummary(Long userId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        List<DailyCheckin> records = dailyCheckinMapper.selectByUserIdAndDate(userId, date);

        List<CheckinRecordResponse> meals = new ArrayList<>();
        List<CheckinRecordResponse> drinks = new ArrayList<>();
        List<CheckinRecordResponse> exercises = new ArrayList<>();
        int waterCups = 0;

        for (DailyCheckin r : records) {
            switch (r.getCheckinType()) {
                case "MEAL" -> meals.add(toResponse(r));
                case "DRINK" -> drinks.add(toResponse(r));
                case "EXERCISE" -> exercises.add(toResponse(r));
                case "WATER" -> waterCups += r.getWaterCups() != null ? r.getWaterCups() : 0;
            }
        }

        // Check meal completeness: has BREAKFAST, LUNCH, DINNER all present
        Set<String> mealTypes = meals.stream()
                .map(CheckinRecordResponse::getMealType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        boolean hasCompleteMeals = mealTypes.containsAll(Set.of("BREAKFAST", "LUNCH", "DINNER"));
        boolean hasDietAndExercise = !meals.isEmpty() && !exercises.isEmpty();

        return DailySummaryResponse.builder()
                .meals(meals)
                .drinks(drinks)
                .exercises(exercises)
                .waterCups(waterCups)
                .waterMl(waterCups * 250)
                .hasCompleteMeals(hasCompleteMeals)
                .hasDietAndExercise(hasDietAndExercise)
                .build();
    }

    @Override
    public WaterSummaryResponse getWaterSummary(Long userId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        int cups = dailyCheckinMapper.sumWaterCupsByDate(userId, date);
        return new WaterSummaryResponse(cups, cups * 250);
    }

    @Override
    @Transactional
    public CheckinRecordResponse addWaterCup(Long userId, LocalDate date) {
        if (date == null) date = LocalDate.now();

        // 防止疯狂点击：单日上限20杯
        int currentCups = dailyCheckinMapper.sumWaterCupsByDate(userId, date);
        if (currentCups >= 20) {
            throw new BusinessException(400, "今日饮水已达上限(20杯)，请适量饮水");
        }

        DailyCheckin record = new DailyCheckin();
        record.setUserId(userId);
        record.setRecordDate(date);
        record.setCheckinType("WATER");
        record.setWaterCups(1);

        dailyCheckinMapper.insert(record);
        return toResponse(record);
    }

    @Override
    public List<CheckinRecordResponse> getCheckinsByDateRange(Long userId, LocalDate start, LocalDate end) {
        List<DailyCheckin> records = dailyCheckinMapper.selectByUserIdAndDateRange(userId, start, end);
        return records.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ===== Private helpers =====

    private void validateRequest(CheckinRequest request) {
        String type = request.getCheckinType() != null ? request.getCheckinType().toUpperCase() : "";
        switch (type) {
            case "MEAL" -> {
                if (request.getMealType() == null || request.getMealType().isBlank())
                    throw new BusinessException(400, "饮食打卡需选择餐次(早餐/午餐/晚餐/加餐)");
                if (request.getFoodDesc() == null || request.getFoodDesc().isBlank())
                    throw new BusinessException(400, "请填写食物描述");
            }
            case "EXERCISE" -> {
                if (request.getExerciseType() == null || request.getExerciseType().isBlank())
                    throw new BusinessException(400, "请选择运动类型");
                if (request.getDurationMin() == null || request.getDurationMin() <= 0)
                    throw new BusinessException(400, "请填写运动时长(分钟)");
            }
            case "DRINK" -> {
                if (request.getDrinkName() == null || request.getDrinkName().isBlank())
                    throw new BusinessException(400, "请填写饮品名称");
            }
            case "WATER" -> {
                // WATER type uses waterCups field; if missing, default to 1
                if (request.getWaterCups() == null || request.getWaterCups() <= 0)
                    request.setWaterCups(1);
            }
            default -> throw new BusinessException(400, "不支持的打卡类型: " + type + " (支持: MEAL/DRINK/EXERCISE/WATER)");
        }
    }

    private String mealLabel(String type) {
        return switch (type) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            case "DINNER" -> "晚餐";
            case "SNACK" -> "加餐";
            default -> type;
        };
    }

    private CheckinRecordResponse toResponse(DailyCheckin record) {
        return new CheckinRecordResponse(
                record.getId(),
                record.getRecordDate(),
                record.getCheckinType(),
                record.getMealType(),
                record.getFoodDesc(),
                record.getFoodAmount(),
                record.getDrinkName(),
                record.getDrinkVolumeMl(),
                record.getExerciseType(),
                record.getDurationMin(),
                record.getWaterCups(),
                record.getHealthScore(),
                record.getCreatedAt()
        );
    }
}
