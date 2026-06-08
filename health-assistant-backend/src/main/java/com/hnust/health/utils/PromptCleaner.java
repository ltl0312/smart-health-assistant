package com.hnust.health.utils;

public class PromptCleaner {

    public static String cleanJsonResponse(String raw) {
        if (raw == null || raw.isBlank()) return raw != null ? raw.trim() : null;

        String trimmed = raw.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        trimmed = trimmed.trim();

        if (trimmed.startsWith("﻿")) {
            trimmed = trimmed.substring(1);
        }

        return trimmed;
    }
}
