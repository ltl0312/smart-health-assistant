package com.hnust.health.service.impl;

import com.hnust.health.dto.UpdateProfileRequest;
import com.hnust.health.exception.BusinessException;
import com.hnust.health.mapper.SysUserMapper;
import com.hnust.health.model.SysUser;
import com.hnust.health.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final SysUserMapper sysUserMapper;

    @Override
    public SysUser getProfile(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setPasswordHash(null); // 脱敏
        return user;
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getBio() != null) user.setBio(request.getBio());
        sysUserMapper.updateById(user);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException(400, "文件为空");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new BusinessException(400, "仅支持图片文件");

        try {
            Path uploadDir = Paths.get("./uploads/avatars/");
            Files.createDirectories(uploadDir);
            String ext = ".png";
            if (contentType.contains("jpeg") || contentType.contains("jpg")) ext = ".jpg";
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path filePath = uploadDir.resolve(filename);
            file.transferTo(filePath.toFile());
            String avatarUrl = "/uploads/avatars/" + filename;

            SysUser user = sysUserMapper.selectById(userId);
            user.setAvatarUrl(avatarUrl);
            sysUserMapper.updateById(user);
            return avatarUrl;
        } catch (IOException e) {
            throw new BusinessException(500, "头像上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteAccount(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setStatus(0);
        sysUserMapper.updateById(user);
    }
}
