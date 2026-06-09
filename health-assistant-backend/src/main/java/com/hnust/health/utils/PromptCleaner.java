package com.hnust.health.utils;

public class PromptCleaner {

    /**
     * 清理 LLM 响应：去除 markdown 代码块、BOM、以及 JSON 前后的对话文本。
     * DeepSeek 有时会在 JSON 前后附加"好的，以下是..."等寒暄语，
     * 此方法定位第一个 { 和最后一个 } 来提取纯 JSON。
     */
    public static String cleanJsonResponse(String raw) {
        if (raw == null || raw.isBlank()) return raw != null ? raw.trim() : null;

        String trimmed = raw.trim();

        // 1. 去除 markdown 代码块标记
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // 2. 去除 BOM
        if (trimmed.startsWith("﻿")) {
            trimmed = trimmed.substring(1);
        }

        // 3. 提取 JSON 对象：找到第一个 { 和最后一个 }，去掉前后对话文本
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }
}
