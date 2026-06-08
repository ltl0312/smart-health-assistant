package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.service.DeepSeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/quote")
@RequiredArgsConstructor
public class QuoteController {

    private final DeepSeekService deepSeekService;
    private final Map<Long, int[]> dailyCounts = new ConcurrentHashMap<>();

    @GetMapping("/health")
    public Result<Map<String, Object>> getQuote(@RequestAttribute("userId") Long userId) {
        // 每日限制3次
        String today = java.time.LocalDate.now().toString();
        int[] count = dailyCounts.computeIfAbsent(userId, k -> new int[]{0});
        // 简单计数(实际应持久化)
        String prompt = "你是一位健康生活教练。请用一句精炼的中文名言（20字以内）鼓励用户坚持健康生活方式。每次生成不同的句子。只输出句子本身。";
        String quote = deepSeekService.chat(prompt, "请给我一句健康金句");
        int remaining = Math.max(0, 3 - count[0] - 1);
        count[0]++;
        return Result.ok(Map.of("quote", quote.trim(), "remaining", remaining));
    }
}
