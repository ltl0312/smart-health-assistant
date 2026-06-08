package com.hnust.health.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String phone;
    private String email;
    private String bio;
}
