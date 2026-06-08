package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.dto.LoginRequest;
import com.hnust.health.dto.LoginResponse;
import com.hnust.health.dto.RegisterRequest;
import com.hnust.health.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口 - 注册与登录
 * 无需 JWT 鉴权 (在 AuthInterceptor 排除路径中)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.ok();
    }

    /**
     * 用户登录 - 返回 JWT 令牌
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.ok(response);
    }
}
