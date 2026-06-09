package com.hnust.health.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PromptCleanerTest {

    @Test
    void shouldStripJsonCodeBlock() {
        String input = "```json\n{\"diet_plan\": \"ok\"}\n```";
        assertEquals("{\"diet_plan\": \"ok\"}", PromptCleaner.cleanJsonResponse(input));
    }

    @Test
    void shouldStripPlainCodeBlock() {
        String input = "```\n{\"hello\": \"world\"}\n```";
        assertEquals("{\"hello\": \"world\"}", PromptCleaner.cleanJsonResponse(input));
    }

    @Test
    void shouldHandlePlainJson() {
        assertEquals("{\"a\":1}", PromptCleaner.cleanJsonResponse("{\"a\":1}"));
    }

    @Test
    void shouldHandleNull() {
        assertNull(PromptCleaner.cleanJsonResponse(null));
    }

    @Test
    void shouldHandleBlank() {
        assertEquals("", PromptCleaner.cleanJsonResponse("   "));
    }

    @Test
    void shouldHandleBom() {
        assertEquals("{\"a\":1}", PromptCleaner.cleanJsonResponse("﻿{\"a\":1}"));
    }

    @Test
    void shouldStripConversationalTextBeforeJson() {
        String input = "好的，已为你生成计划。\\n以下是你的专属计划JSON：\\n{\"diet_plan\":{\"day1\":{}},\"analysis\":\"test\"}";
        assertEquals("{\"diet_plan\":{\"day1\":{}},\"analysis\":\"test\"}", PromptCleaner.cleanJsonResponse(input));
    }

    @Test
    void shouldStripTextAfterJson() {
        String input = "{\"a\":1}\\n以上是生成结果，请参考。";
        assertEquals("{\"a\":1}", PromptCleaner.cleanJsonResponse(input));
    }

    @Test
    void shouldHandleConversationAndCodeBlock() {
        String input = "好的！\\n```json\\n{\"diet_plan\":{\"day1\":{}}}\\n```\\n希望对你有帮助！";
        assertEquals("{\"diet_plan\":{\"day1\":{}}}", PromptCleaner.cleanJsonResponse(input));
    }
}
