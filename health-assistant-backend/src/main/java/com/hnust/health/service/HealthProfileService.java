package com.hnust.health.service;

import com.hnust.health.dto.ProfileRequest;
import com.hnust.health.model.HealthProfile;
import java.math.BigDecimal;

public interface HealthProfileService {
    void setupProfile(Long userId, ProfileRequest request);
    HealthProfile getProfile(Long userId);
    void updateHeight(Long userId, BigDecimal newHeightCm);
}
