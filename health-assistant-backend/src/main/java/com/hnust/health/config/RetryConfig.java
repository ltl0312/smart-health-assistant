package com.hnust.health.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.listener.RetryListenerSupport;

@Slf4j
@Configuration
@EnableRetry
public class RetryConfig {

    @Bean
    public RetryListenerSupport retryListener() {
        return new RetryListenerSupport() {
            @Override
            public <T, E extends Throwable> void onError(
                    org.springframework.retry.RetryContext context,
                    org.springframework.retry.RetryCallback<T, E> callback,
                    Throwable throwable) {
                log.warn("DeepSeek 重试第 {} 次, 错误: {}",
                        context.getRetryCount(), throwable.getMessage());
            }
        };
    }
}
