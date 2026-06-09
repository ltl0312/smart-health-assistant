package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.mapper.AiPlanMapper;
import com.hnust.health.mapper.DailyCheckinMapper;
import com.hnust.health.model.AiPlan;
import com.hnust.health.model.DailyCheckin;
import com.hnust.health.util.MarkdownGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final AiPlanMapper aiPlanMapper;
    private final DailyCheckinMapper dailyCheckinMapper;

    @GetMapping
    public Result<List<AiPlan>> listRecords(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        // 自动删除3个月前的记录
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        aiPlanMapper.delete(new LambdaQueryWrapper<AiPlan>()
                .lt(AiPlan::getCreatedAt, threeMonthsAgo));

        // 查询当前用户3个月内的记录
        List<AiPlan> plans = aiPlanMapper.selectList(
                new LambdaQueryWrapper<AiPlan>()
                        .eq(AiPlan::getUserId, userId)
                        .ge(AiPlan::getCreatedAt, threeMonthsAgo)
                        .orderByDesc(AiPlan::getCycleStartDate)
                        .orderByDesc(AiPlan::getCreatedAt));
        return Result.ok(plans);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadPlan(@PathVariable Long id,
                                                @RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        AiPlan plan = aiPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }

        // 查询该计划周期内的打卡记录
        LocalDate cycleStart = plan.getCycleStartDate();
        LocalDate cycleEnd = cycleStart.plusDays(6);
        List<DailyCheckin> checkins = dailyCheckinMapper.selectByUserIdAndDateRange(userId, cycleStart, cycleEnd);

        String markdown = MarkdownGenerator.generatePlanReport(plan, checkins);
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"));
        String datePart = plan.getCycleStartDate().toString();
        String timePart = plan.getCreatedAt() != null
                ? plan.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                : "00:00";
        String fname = "健康协议 · " + datePart + "——" + timePart + ".md";
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(fname, StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(@PathVariable Long id,
                                    @RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        AiPlan plan = aiPlanMapper.selectById(id);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return Result.fail(404, "记录不存在");
        }
        aiPlanMapper.deleteById(id);
        return Result.ok();
    }
}
