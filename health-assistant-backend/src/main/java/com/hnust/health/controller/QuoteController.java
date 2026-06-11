package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.service.DeepSeekService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/quote")
@RequiredArgsConstructor
public class QuoteController {

    private static final List<String> FALLBACK_QUOTES = List.of(
            "健康不是某一天的冲刺，而是每天温柔地照顾自己。",
            "今天多走一步，明天的身体会替你记得。",
            "规律饮食、稳定睡眠和适量运动，是最可靠的健康复利。",
            "不用追求完美，坚持记录和微调，就是改变的开始。",
            "身体的反馈很诚实，慢慢来，也是在向前。"
    );

    private final DeepSeekService deepSeekService;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, String> localCache = new ConcurrentHashMap<>();

    @GetMapping("/health")
    public Result<Map<String, Object>> getQuote(@RequestAttribute("userId") Long userId) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        String key = "dailyQuote:" + userId + ":" + today;
        String cached = getCachedQuote(key);
        if (cached != null && !cached.isBlank()) {
            return Result.ok(Map.of("quote", cached, "date", today.toString(), "cached", true));
        }

        String prompt = "你是一位健康生活教练。请用一句精炼的中文短句鼓励用户坚持健康生活方式。只输出句子本身。";
        String quote;
        try {
            quote = deepSeekService.chat(prompt, "请给我一句健康金句").trim();
        } catch (Exception e) {
            quote = fallbackQuote(userId);
        }

        if (quote.isBlank()) {
            quote = fallbackQuote(userId);
        }
        cacheQuote(key, quote, today);
        return Result.ok(Map.of("quote", quote, "date", today.toString(), "cached", false));
    }

    private String fallbackQuote(Long userId) {
        int index = Math.floorMod((int) (LocalDate.now(ZoneId.of("Asia/Shanghai")).toEpochDay() + userId), FALLBACK_QUOTES.size());
        return FALLBACK_QUOTES.get(index);
    }

    private String getCachedQuote(String key) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        } catch (Exception ignored) {
            // Fall back to process-local cache when Redis is unavailable.
        }
        return localCache.get(key);
    }

    private void cacheQuote(String key, String quote, LocalDate today) {
        localCache.put(key, quote);
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                LocalDateTime midnight = LocalDateTime.of(today.plusDays(1), LocalTime.MIDNIGHT);
                Duration ttl = Duration.between(LocalDateTime.now(ZoneId.of("Asia/Shanghai")), midnight);
                redisTemplate.opsForValue().set(key, quote, ttl);
            }
        } catch (Exception ignored) {
            // Local cache already holds today's quote.
        }
    }
}
