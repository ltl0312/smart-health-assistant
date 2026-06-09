package com.hnust.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.health.model.DailyCheckin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyCheckinMapper extends BaseMapper<DailyCheckin> {

    @Select("SELECT * FROM daily_checkin WHERE user_id = #{userId} AND record_date = #{date} ORDER BY created_at ASC")
    List<DailyCheckin> selectByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT * FROM daily_checkin WHERE user_id = #{userId} AND record_date = #{date} AND checkin_type = #{type} ORDER BY created_at ASC")
    List<DailyCheckin> selectByUserIdAndDateAndType(@Param("userId") Long userId, @Param("date") LocalDate date, @Param("type") String type);

    @Select("SELECT * FROM daily_checkin WHERE user_id = #{userId} AND record_date >= #{startDate} AND record_date <= #{endDate} ORDER BY record_date ASC, created_at ASC")
    List<DailyCheckin> selectByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT COALESCE(SUM(water_cups), 0) FROM daily_checkin WHERE user_id = #{userId} AND record_date = #{date} AND checkin_type = 'WATER'")
    int sumWaterCupsByDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
