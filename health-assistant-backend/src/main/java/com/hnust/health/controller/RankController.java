package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.dto.HealthRankResponse;
import com.hnust.health.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/health")
    public Result<List<HealthRankResponse>> getHealthRanking() {
        return Result.ok(rankService.getHealthRanking());
    }
}
