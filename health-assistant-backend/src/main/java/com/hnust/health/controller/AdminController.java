package com.hnust.health.controller;

import com.hnust.health.annotation.RequireRole;
import com.hnust.health.config.Result;
import com.hnust.health.dto.AdminStatsResponse;
import com.hnust.health.mapper.AiPlanMapper;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.mapper.WeightRecordMapper;
import com.hnust.health.model.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SysUserMapper sysUserMapper;
    private final AiPlanMapper aiPlanMapper;
    private final WeightRecordMapper weightRecordMapper;

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
    public Result<List<SysUser>> getAllUsers() {
        List<SysUser> users = sysUserMapper.selectList(null);
        users.forEach(u -> u.setPasswordHash(null));
        return Result.ok(users);
    }
}
