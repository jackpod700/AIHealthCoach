package com.aihealthcoach.dailygoal.mapper;

import com.aihealthcoach.dailygoal.entity.DailyGoal;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyGoalMapper {
    DailyGoal findByUserId(Long userId);

    DailyGoal upsert(DailyGoal dailyGoal);
}
