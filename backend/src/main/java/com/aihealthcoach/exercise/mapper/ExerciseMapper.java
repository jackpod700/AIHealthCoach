package com.aihealthcoach.exercise.mapper;

import com.aihealthcoach.exercise.entity.ExerciseRecord;
import com.aihealthcoach.exercise.entity.PhysicalActivity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExerciseMapper {
    List<PhysicalActivity> findPhysicalActivities(@Param("keyword") String keyword);

    PhysicalActivity findPhysicalActivityById(Long id);

    void upsertPhysicalActivity(PhysicalActivity physicalActivity);

    ExerciseRecord insertExerciseRecord(ExerciseRecord exerciseRecord);

    List<ExerciseRecord> findExerciseRecordsByDate(
        @Param("userId") Long userId,
        @Param("exerciseDate") LocalDate exerciseDate);

    List<LocalDate> findExerciseDatesInMonth(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
