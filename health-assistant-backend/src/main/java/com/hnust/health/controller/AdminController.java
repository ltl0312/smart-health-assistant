package com.hnust.health.controller;

import com.hnust.health.annotation.RequireRole;
import com.hnust.health.config.Result;
import com.hnust.health.dto.AdminStatsResponse;
import com.hnust.health.mapper.AiPlanMapper;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.mapper.WeightRecordMapper;
import com.hnust.health.model.SysUser;
import com.hnust.health.service.WellnessFeatureService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SysUserMapper sysUserMapper;
    private final AiPlanMapper aiPlanMapper;
    private final WeightRecordMapper weightRecordMapper;
    private final WellnessFeatureService wellnessFeatureService;

    @GetMapping("/stats")
    @RequireRole("ADMIN")
    public Result<AdminStatsResponse> getStats() {
        long totalUsers = sysUserMapper.selectCount(null);
        long activeUsers = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        long totalPlans = aiPlanMapper.selectCount(null);
        long totalRecords = weightRecordMapper.selectCount(null);
        return Result.ok(new AdminStatsResponse(totalUsers, activeUsers, totalPlans, totalRecords));
    }

    @GetMapping("/users")
    @RequireRole("ADMIN")
    public Result<List<Map<String, Object>>> getAllUsers(@RequestParam(required = false) String keyword) {
        return Result.ok(wellnessFeatureService.adminUsers(keyword));
    }

    @GetMapping("/dashboard")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> getDashboard() {
        return Result.ok(wellnessFeatureService.adminDashboard());
    }

    @PutMapping("/users/{id}/status")
    @RequireRole("ADMIN")
    public Result<Void> updateUserStatus(@RequestAttribute("userId") Long adminId,
                                         @PathVariable Long id,
                                         @RequestBody Map<String, Object> request) {
        wellnessFeatureService.updateUserStatus(adminId, id, request);
        return Result.ok();
    }

    @GetMapping("/ai/status")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> getAiStatus() {
        return Result.ok(wellnessFeatureService.aiStatus());
    }

    @GetMapping("/articles")
    @RequireRole("ADMIN")
    public Result<List<Map<String, Object>>> getArticles(@RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String status) {
        return Result.ok(wellnessFeatureService.adminArticles(keyword, status));
    }

    @GetMapping("/articles/{id}")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> getArticle(@PathVariable Long id) {
        return Result.ok(wellnessFeatureService.adminArticle(id));
    }

    @PostMapping("/articles")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> createArticle(@RequestAttribute("userId") Long adminId,
                                                     @RequestBody Map<String, Object> request) {
        return Result.ok(wellnessFeatureService.saveArticle(adminId, null, request));
    }

    @PutMapping("/articles/{id}")
    @RequireRole("ADMIN")
    public Result<Map<String, Object>> updateArticle(@RequestAttribute("userId") Long adminId,
                                                     @PathVariable Long id,
                                                     @RequestBody Map<String, Object> request) {
        return Result.ok(wellnessFeatureService.saveArticle(adminId, id, request));
    }

    @DeleteMapping("/articles/{id}")
    @RequireRole("ADMIN")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        wellnessFeatureService.deleteArticle(id);
        return Result.ok();
    }
}
