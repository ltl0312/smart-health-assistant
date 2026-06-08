package com.hnust.health.service.impl;

import com.hnust.health.dto.LoginRequest;
import com.hnust.health.dto.RegisterRequest;
import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.model.SysUser;
import com.hnust.health.security.JwtUtil;
import com.hnust.health.security.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock SysUserMapper sysUserMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthServiceImpl authService;

    private RegisterRequest registerReq;
    private LoginRequest loginReq;

    @BeforeEach
    void setUp() {
        registerReq = new RegisterRequest();
        registerReq.setUsername("testuser");
        registerReq.setPassword("123456");
        registerReq.setEmail("test@test.com");

        loginReq = new LoginRequest();
        loginReq.setUsername("testuser");
        loginReq.setPassword("123456");
    }

    @Test
    void register_shouldSuccess() {
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        when(sysUserMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> authService.register(registerReq));
        verify(sysUserMapper).insert(any(SysUser.class));
    }

    @Test
    void register_shouldThrowWhenUsernameExists() {
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> authService.register(registerReq));
    }

    @Test
    void login_shouldReturnToken() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hashed");
        user.setStatus(1);
        user.setRole("USER");

        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testuser", "USER")).thenReturn("jwt.token.here");

        var result = authService.login(loginReq);
        assertNotNull(result);
        assertEquals("jwt.token.here", result.getToken());
        assertEquals(1L, result.getUserId());
        assertEquals("USER", result.getRole());
    }

    @Test
    void login_shouldThrowWithWrongPassword() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setPasswordHash("hashed");
        user.setStatus(1);

        when(sysUserMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.login(loginReq));
    }
}
