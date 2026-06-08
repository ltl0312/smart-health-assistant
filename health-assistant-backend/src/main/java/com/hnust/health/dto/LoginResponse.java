package com.hnust.health.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户登录响应 - 返回 JWT Token
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private Long userId;
    private String role;
}
