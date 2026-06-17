package com.aihealthcoach.exercise.mapper;

import com.aihealthcoach.exercise.entity.ExerciseRecord;
import com.aihealthcoach.exercise.entity.ExerciseActivityOption;
import com.aihealthcoach.exercise.entity.PhysicalActivity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExerciseMapper {
    List<ExerciseActivityOption> findExerciseActivityOptions(@Param("keyword") String keyword);

    ExerciseActivityOption findExerciseActivityOptionById(Long id);

    void upsertPhysicalActivity(PhysicalActivity physicalActivity);

    ExerciseRecord insertExerciseRecord(ExerciseRecord exerciseRecord);

    ExerciseRecord updateExerciseRecord(ExerciseRecord exerciseRecord);

    int deleteExerciseRecord(
            @Param("userId") Long userId,
            @Param("recordId") Long recordId);

    List<ExerciseRecord> findExerciseRecordsByDate(
        @Param("userId") Long userId,
        @Param("exerciseDate") LocalDate exerciseDate);

    Integer sumDailyCaloriesBurned(
        @Param("userId") Long userId,
        @Param("exerciseDate") LocalDate exerciseDate);

    List<LocalDate> findExerciseDatesInMonth(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
