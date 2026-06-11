package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.dto.WeightHistoryResponse;
import com.hnust.health.dto.WeightRecordRequest;
import com.hnust.health.service.WeightRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

@RestController
@RequestMapping("/weight")
@RequiredArgsConstructor
public class WeightController {

    private final WeightRecordService weightRecordService;

    @PostMapping("/record")
    public Result<Void> recordWeight(@Valid @RequestBody WeightRecordRequest request,
                                      @RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        weightRecordService.recordWeight(userId, request);
        return Result.ok();
    }

    @PutMapping("/record")
    public Result<Map<String, Object>> updateWeight(@Valid @RequestBody WeightRecordRequest request,
                                                     @RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        weightRecordService.updateWeight(userId, request);
        return Result.ok(Map.of("msg", "修改成功"));
    }

    @GetMapping("/history")
    public Result<List<WeightHistoryResponse>> getWeightHistory(
            @RequestParam(defaultValue = "30") int days,
            @RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(weightRecordService.getWeightHistory(userId, days));
    }

    @GetMapping("/weekly-status")
    public Result<Map<String, Object>> getWeeklyStatus(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(weightRecordService.getWeeklyStatus(userId));
    }
}
