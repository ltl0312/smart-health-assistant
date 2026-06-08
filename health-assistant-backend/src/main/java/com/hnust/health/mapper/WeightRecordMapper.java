package com.hnust.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.health.model.WeightRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WeightRecordMapper extends BaseMapper<WeightRecord> {

    /**
     * 按时间倒序查询用户近N条体重记录
     * 利用 idx_user_weight_trend 复合索引优化查询
     */
    @Select("SELECT * FROM weight_record WHERE user_id = #{userId} ORDER BY record_date DESC LIMIT #{limit}")
    List<WeightRecord> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询用户指定天数内的体重记录
     */
    @Select("SELECT * FROM weight_record WHERE user_id = #{userId} AND record_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) ORDER BY record_date ASC")
    List<WeightRecord> selectByUserIdAndDays(@Param("userId") Long userId, @Param("days") int days);
}
