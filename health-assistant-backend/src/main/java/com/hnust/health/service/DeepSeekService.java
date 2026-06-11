package com.hnust.health.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.health.config.DeepSeekConfig;
import com.hnust.health.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        requestBody.put("messages", messages);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            log.info("Calling DeepSeek API: model={}", config.getModel());

            ResponseEntity<String> response = deepSeekRestTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getBody() == null) {
                throw new BusinessException("DeepSeek API returned an empty response");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                log.error("DeepSeek response has no choices: {}", response.getBody());
                throw new BusinessException("DeepSeek did not return a valid result");
            }

            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText();

            String reasoningContent = message.path("reasoning_content").asText();
            if (!reasoningContent.isEmpty()) {
                log.info("DeepSeek reasoning preview: {}", reasoningContent.substring(0, Math.min(200, reasoningContent.length())));
            }

            log.info("DeepSeek response length: {} chars", content.length());
            return content;
        } catch (RestClientException e) {
            log.warn("DeepSeek API call failed and will be retried: {}", e.getMessage());
            throw new BusinessException("AI service unavailable: " + e.getMessage());
        } catch (Exception e) {
            log.error("DeepSeek response parsing failed: {}", e.getMessage());
            throw new BusinessException("AI response parsing failed: " + e.getMessage());
        }
    }

    @Recover
    public String recover(RestClientException e, String systemPrompt, String userMessage) {
        log.error("DeepSeek failed after retries: {}", e.getMessage());
        throw new BusinessException(503, "AI服务暂时不可用，请检查 DeepSeek API Key 或稍后重试");
    }

    @Recover
    public String recover(BusinessException e, String systemPrompt, String userMessage) {
        log.error("DeepSeek failed after retries: {}", e.getMessage());
        throw new BusinessException(503, "AI服务暂时不可用，请检查 DeepSeek API Key 或稍后重试");
    }
}
