package com.hnust.health.service;

import com.hnust.health.dto.UpdateProfileRequest;
import com.hnust.health.model.SysUser;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {
    SysUser getProfile(Long userId);
    void updateProfile(Long userId, UpdateProfileRequest request);
    String uploadAvatar(Long userId, MultipartFile file);
    void deleteAccount(Long userId);
}
