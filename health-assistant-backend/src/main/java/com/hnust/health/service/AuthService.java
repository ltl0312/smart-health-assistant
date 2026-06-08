package com.hnust.health.service;

import com.hnust.health.dto.LoginRequest;
import com.hnust.health.dto.LoginResponse;
import com.hnust.health.dto.RegisterRequest;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册 - 密码 BCrypt 加密落库
     */
    void register(RegisterRequest request);

    /**
     * 用户登录 - 校验凭据，返回 JWT 令牌
     */
    LoginResponse login(LoginRequest request);
}
