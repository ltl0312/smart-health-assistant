package com.hnust.health.controller;

import com.hnust.health.config.Result;
import com.hnust.health.dto.ChangePasswordRequest;
import com.hnust.health.dto.UpdateProfileRequest;
import com.hnust.health.model.SysUser;
import com.hnust.health.service.UserProfileService;
import com.hnust.health.util.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.hnust.health.constant.Constants.REQUEST_ATTR_USER_ID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/profile")
    public Result<SysUser> getProfile(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        return Result.ok(userProfileService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                       @RequestBody UpdateProfileRequest request) {
        userProfileService.updateProfile(userId, request);
        return Result.ok();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                        @Valid @RequestBody ChangePasswordRequest request) {
        passwordResetService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.ok();
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId,
                                        @RequestParam("file") MultipartFile file) {
        return Result.ok(userProfileService.uploadAvatar(userId, file));
    }

    @DeleteMapping("/account")
    public Result<Void> deleteAccount(@RequestAttribute(REQUEST_ATTR_USER_ID) Long userId) {
        userProfileService.deleteAccount(userId);
        return Result.ok();
    }
}
