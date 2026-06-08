package com.hnust.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnust.health.dto.LoginRequest;
import com.hnust.health.dto.LoginResponse;
import com.hnust.health.dto.RegisterRequest;
import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.HealthProfileMapper;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.model.SysUser;
import com.hnust.health.security.JwtUtil;
import com.hnust.health.security.PasswordEncoder;
import com.hnust.health.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现 - 注册与登录逻辑
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final HealthProfileMapper healthProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        if (sysUserMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(400, "用户名已被注册");
        }

        // 创建用户，密码 BCrypt 加密
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setStatus(1);
        user.setRole("USER");
        sysUserMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账户已被封禁");
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        boolean hasProfile = healthProfileMapper.selectById(user.getId()) != null;
        return new LoginResponse(token, user.getUsername(), user.getId(), user.getRole(), hasProfile);
    }
}
