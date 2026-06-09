package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.dto.*;
import com.hnust.health.service.DailyCheckinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class DailyCheckinController {

    private final DailyCheckinService dailyCheckinService;

    @PostMapping
    public Result<CheckinRecordResponse> createCheckin(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                        @Valid @RequestBody CheckinRequest request) {
        return Result.ok(dailyCheckinService.createCheckin(userId, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCheckin(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                       @PathVariable Long id) {
        dailyCheckinService.deleteCheckin(userId, id);
        return Result.ok();
    }

    @GetMapping("/today")
    public Result<List<CheckinRecordResponse>> getTodayCheckins(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                                  @RequestParam(required = false) String type) {
        return Result.ok(dailyCheckinService.getTodayCheckins(userId, type));
    }

    @GetMapping("/summary")
    public Result<DailySummaryResponse> getDailySummary(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dailyCheckinService.getDailySummary(userId, date));
    }

    @GetMapping("/water/today")
    public Result<WaterSummaryResponse> getWaterToday(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(dailyCheckinService.getWaterSummary(userId, LocalDate.now()));
    }

    @PostMapping("/water")
    public Result<CheckinRecordResponse> addWaterCup(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(dailyCheckinService.addWaterCup(userId, LocalDate.now()));
    }

    @GetMapping("/range")
    public Result<List<CheckinRecordResponse>> getCheckinRange(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return Result.ok(dailyCheckinService.getCheckinsByDateRange(userId, start, end));
    }
}
