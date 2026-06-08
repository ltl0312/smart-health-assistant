package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.dto.ProfileRequest;
import com.hnust.health.mapper.AiPlanMapper;
import com.hnust.health.mapper.WeightRecordMapper;
import com.hnust.health.model.AiPlan;
import com.hnust.health.model.HealthProfile;
import com.hnust.health.model.WeightRecord;
import com.hnust.health.service.HealthProfileService;
import com.hnust.health.util.MarkdownGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final HealthProfileService healthProfileService;
    private final WeightRecordMapper weightRecordMapper;
    private final AiPlanMapper aiPlanMapper;

    @PostMapping("/setup")
    public Result<Void> setupProfile(@Valid @RequestBody ProfileRequest request,
                                      @RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        healthProfileService.setupProfile(userId, request);
        return Result.ok();
    }

    @GetMapping
    public Result<HealthProfile> getProfile(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(healthProfileService.getProfile(userId));
    }

    @PutMapping("/height")
    public Result<Void> updateHeight(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                      @RequestBody Map<String, BigDecimal> body) {
        BigDecimal heightCm = body.get("heightCm");
        if (heightCm == null) throw new RuntimeException("身高不能为空");
        healthProfileService.updateHeight(userId, heightCm);
        return Result.ok();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProfile(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        HealthProfile profile = healthProfileService.getProfile(userId);
        List<WeightRecord> weights = weightRecordMapper.selectList(
                new LambdaQueryWrapper<WeightRecord>().eq(WeightRecord::getUserId, userId).orderByAsc(WeightRecord::getRecordDate));
        List<AiPlan> plans = aiPlanMapper.selectList(
                new LambdaQueryWrapper<AiPlan>().eq(AiPlan::getUserId, userId).orderByDesc(AiPlan::getCreatedAt));

        String md = MarkdownGenerator.generateHealthExport(profile, weights, plans);
        byte[] bytes = md.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("health-export.md", StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
