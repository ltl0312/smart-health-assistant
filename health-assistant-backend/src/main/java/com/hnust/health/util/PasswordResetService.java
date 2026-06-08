package com.hnust.health.util;

import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.model.SysUser;
import com.hnust.health.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordResetService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(400, "旧密码不正确");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }
}
