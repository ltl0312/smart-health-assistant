package com.hnust.health.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.health.config.DeepSeekConfig;
import com.hnust.health.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final DeepSeekConfig config;
    private final RestTemplate deepSeekRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Retryable(retryFor = {RestClientException.class, BusinessException.class},
               maxAttempts = 3,
               backoff = @Backoff(delay = 2000),
               listeners = {"retryListener"})
    public String chat(String systemPrompt, String userMessage) {
        String url = config.getBaseUrl() + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getMaxTokens());

        // 关闭深度推理以加快响应（关闭 thinking + 降低 reasoning 强度）
        // thinking 和 reasoning_effort 已移除，使用默认模式

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            log.info("调用 DeepSeek API: model={}", config.getModel());

            ResponseEntity<String> response = deepSeekRestTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getBody() == null) {
                throw new BusinessException("DeepSeek API 返回空响应");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                log.error("DeepSeek 响应无 choices: {}", response.getBody());
                throw new BusinessException("DeepSeek 未返回有效结果");
            }

            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText();

            String reasoningContent = message.path("reasoning_content").asText();
            if (!reasoningContent.isEmpty()) {
                log.info("DeepSeek 思维链: {}", reasoningContent.substring(0, Math.min(200, reasoningContent.length())));
            }

            log.info("DeepSeek 响应长度: {} 字符", content.length());
            return content;

        } catch (RestClientException e) {
            log.warn("DeepSeek API 调用失败 (将自动重试): {}", e.getMessage());
            throw new BusinessException("AI 服务暂时不可用: " + e.getMessage());
        } catch (Exception e) {
            log.error("DeepSeek 响应解析失败: {}", e.getMessage());
            throw new BusinessException("AI 响应解析失败: " + e.getMessage());
        }
    }

    @Recover
    public String recover(RestClientException e, String systemPrompt, String userMessage) {
        log.error("DeepSeek 重试3次后仍失败: {}", e.getMessage());
        return "{\"error\":\"AI_SERVICE_UNAVAILABLE\",\"message\":\"AI服务暂时不可用，请稍后重试\"}";
    }

    @Recover
    public String recover(BusinessException e, String systemPrompt, String userMessage) {
        log.error("DeepSeek 重试3次后仍失败 (业务异常): {}", e.getMessage());
        return "{\"error\":\"AI_SERVICE_UNAVAILABLE\",\"message\":\"AI服务暂时不可用，请稍后重试\"}";
    }
}
