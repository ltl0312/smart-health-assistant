package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.service.WellnessFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

@RestController
@RequiredArgsConstructor
public class WellnessFeatureController {

    private final WellnessFeatureService wellnessFeatureService;

    @GetMapping("/goals")
    public Result<Map<String, Object>> getGoal(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(wellnessFeatureService.getGoal(userId));
    }

    @PostMapping("/goals")
    public Result<Map<String, Object>> createGoal(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                  @RequestBody Map<String, Object> request) {
        return Result.ok(wellnessFeatureService.saveGoal(userId, request));
    }

    @PutMapping("/goals/{id}")
    public Result<Map<String, Object>> updateGoal(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                  @PathVariable Long id,
                                                  @RequestBody Map<String, Object> request) {
        request.put("id", id);
        return Result.ok(wellnessFeatureService.saveGoal(userId, request));
    }

    @GetMapping("/report/summary")
    public Result<Map<String, Object>> reportSummary(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                     @RequestParam(defaultValue = "30") int days) {
        return Result.ok(wellnessFeatureService.reportSummary(userId, days));
    }

    @GetMapping(value = "/report/pdf", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> printableReport(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                  @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(wellnessFeatureService.printableReportHtml(userId, days));
    }

    @PostMapping("/estimate/meal")
    public Result<Map<String, Object>> estimateMeal(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                    @RequestBody Map<String, Object> request) {
        return Result.ok(wellnessFeatureService.estimateMeal(userId, request));
    }

    @PostMapping("/estimate/exercise")
    public Result<Map<String, Object>> estimateExercise(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                        @RequestBody Map<String, Object> request) {
        return Result.ok(wellnessFeatureService.estimateExercise(userId, request));
    }

    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> alerts(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(wellnessFeatureService.alerts(userId));
    }

    @PostMapping("/alerts/{id}/read")
    public Result<Void> markAlertRead(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                      @PathVariable Long id) {
        wellnessFeatureService.markAlertRead(userId, id);
        return Result.ok();
    }

    @GetMapping("/reminders")
    public Result<List<Map<String, Object>>> reminders(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(wellnessFeatureService.reminders(userId));
    }

    @PostMapping("/reminders/{id}/done")
    public Result<Void> markReminderDone(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                         @PathVariable Long id) {
        wellnessFeatureService.markReminderDone(userId, id);
        return Result.ok();
    }

    @GetMapping("/plans/latest")
    public Result<Map<String, Object>> latestPlan(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(wellnessFeatureService.latestPlan(userId));
    }

    @GetMapping("/plans/pending")
    public Result<Map<String, Object>> pendingPlan(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(wellnessFeatureService.pendingPlan(userId));
    }

    @GetMapping("/plans/history")
    public Result<List<Map<String, Object>>> planHistory(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(wellnessFeatureService.planHistory(userId));
    }

    @GetMapping("/plans/{id}")
    public Result<Map<String, Object>> plan(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                            @PathVariable Long id) {
        return Result.ok(wellnessFeatureService.plan(userId, id));
    }

    @PostMapping("/plans/{id}/approve")
    public Result<Map<String, Object>> approvePlan(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                   @PathVariable Long id) {
        return Result.ok(wellnessFeatureService.approvePlan(userId, id));
    }

    @PostMapping("/plans/{id}/reject")
    public Result<Void> rejectPlan(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                   @PathVariable Long id) {
        wellnessFeatureService.rejectPlan(userId, id);
        return Result.ok();
    }

    @GetMapping(value = "/plans/{id}/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> exportPlan(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                             @PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smarthealth-week-plan.md\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(wellnessFeatureService.exportPlanMarkdown(userId, id));
    }

    @GetMapping("/plans/{id}/calendar")
    public Result<List<Map<String, Object>>> planCalendar(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                          @PathVariable Long id) {
        return Result.ok(wellnessFeatureService.planCalendar(userId, id));
    }

    @PostMapping("/plans/{id}/items/{itemId}/checkin")
    public Result<Map<String, Object>> checkinPlanItem(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                       @PathVariable Long id,
                                                       @PathVariable Long itemId,
                                                       @RequestBody Map<String, Object> request) {
        return Result.ok(wellnessFeatureService.checkinPlanItem(userId, id, itemId, request));
    }

    @GetMapping("/plans/{id}/progress")
    public Result<Map<String, Object>> planProgress(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                    @PathVariable Long id) {
        return Result.ok(wellnessFeatureService.planProgress(userId, id));
    }

    @GetMapping("/reviews/weekly")
    public Result<Map<String, Object>> weeklyReview(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                    @RequestParam(required = false) String week) {
        return Result.ok(wellnessFeatureService.weeklyReview(userId, week));
    }

    @PostMapping("/reviews/weekly/generate")
    public Result<Map<String, Object>> generateWeeklyReview(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                            @RequestParam(required = false) String week) {
        return Result.ok(wellnessFeatureService.generateWeeklyReview(userId, week));
    }

    @GetMapping("/articles")
    public Result<List<Map<String, Object>>> articles(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                                      @RequestParam(required = false) String category) {
        return Result.ok(wellnessFeatureService.articles(userId, category));
    }

    @GetMapping("/articles/{id}")
    public Result<Map<String, Object>> article(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                               @PathVariable Long id) {
        return Result.ok(wellnessFeatureService.article(userId, id));
    }
}
