package com.aihealthcoach.exercise.service;

import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.PhysicalActivityResponse;
import java.time.LocalDate;
import java.util.List;

public interface ExerciseService {
    public List<PhysicalActivityResponse> findPhysicalActivities(String keyword);

    public ExerciseRecordResponse insertExerciseRecord(Long userId, ExerciseRecordRequest request);

    public List<ExerciseRecordResponse> findExerciseRecordsByDate(Long userId, LocalDate exerciseDate);

    public List<LocalDate> findExerciseDatesInMonth(Long userId, int year, int month);
}
