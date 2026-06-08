package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.dto.ChatRequest;
import com.hnust.health.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    public Result<Map<String, Object>> sendMessage(@RequestBody ChatRequest request,
                                                    @RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(chatService.chat(userId, request.getMessage()));
    }
}
