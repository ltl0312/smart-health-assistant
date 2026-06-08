package com.hnust.health.service;

import java.util.Map;

public interface ChatService {
    Map<String, Object> chat(Long userId, String message);
}
