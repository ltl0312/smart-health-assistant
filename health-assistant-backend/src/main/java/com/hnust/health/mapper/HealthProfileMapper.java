package com.hnust.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.health.model.HealthProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthProfileMapper extends BaseMapper<HealthProfile> {
}
