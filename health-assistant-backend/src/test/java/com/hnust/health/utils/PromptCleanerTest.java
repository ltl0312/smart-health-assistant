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
}
