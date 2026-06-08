package com.hnust.health.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * DeepSeek 大模型客户端配置
 * 从 application.yml 中 llm.deepseek 节点读取配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm.deepseek")
public class DeepSeekConfig {

    private String baseUrl;
    private String apiKey;
    private String model;
    private int timeoutMs;
    private double temperature;
    private int maxTokens;

    @Bean
    public RestTemplate deepSeekRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
