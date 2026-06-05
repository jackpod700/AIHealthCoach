package com.aihealthcoach.exercise.service;

import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseActivityOptionResponse;
import com.aihealthcoach.exercise.entity.ExerciseActivityOption;
import com.aihealthcoach.exercise.entity.ExerciseRecord;
import com.aihealthcoach.exercise.exception.ExerciseException;
import com.aihealthcoach.exercise.mapper.ExerciseMapper;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseMapper exerciseDao;
    private final UserMapper userDao;

    @Override
    public List<ExerciseActivityOptionResponse> findExerciseActivityOptions(String keyword) {
        return exerciseDao.findExerciseActivityOptions(keyword).stream()
                .map(ExerciseActivityOptionResponse::fromEntity).toList();
    }

    @Override
    public ExerciseRecordResponse insertExerciseRecord(Long userId, ExerciseRecordRequest request) {
        ExerciseActivityOption activityOption = exerciseDao.findExerciseActivityOptionById(
                request.exerciseActivityOptionId());

        if (activityOption == null) {
            throw ExerciseException.exerciseActivityOptionNotFound();
        }

        UserProfile userProfile = userDao.findUserProfileByUserId(userId);

        if (userProfile == null) {
            throw UserException.profileNotFound();
        }

        if (userProfile.getCurrentWeightKg() == null) {
            throw ExerciseException.userWeightNotFound();
        }

        BigDecimal metValue = resolveMetValue(activityOption, request.intensityLevel());
        Integer caloriesBurned = calculateCaloriesBurned(metValue, userProfile.getCurrentWeightKg(),
                request.durationMinutes());

        ExerciseRecord savedRecord = exerciseDao.insertExerciseRecord(
                ExerciseRecord.builder().userId(userId).exerciseActivityOptionId(request.exerciseActivityOptionId())
                        .intensityLevel(request.intensityLevel())
                        .exerciseDate(request.exerciseDate()).durationMinutes(request.durationMinutes())
                        .caloriesBurned(caloriesBurned).memo(request.memo()).build());

        return ExerciseRecordResponse.fromEntity(savedRecord);
    }

    @Override
    public List<ExerciseRecordResponse> findExerciseRecordsByDate(Long userId, LocalDate exerciseDate) {
        return exerciseDao.findExerciseRecordsByDate(userId, exerciseDate).stream()
                .map(ExerciseRecordResponse::fromEntity).toList();
    }

    @Override
    public List<LocalDate> findExerciseDatesInMonth(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);

        return exerciseDao.findExerciseDatesInMonth(userId, startDate, endDate);
    }

    private BigDecimal resolveMetValue(ExerciseActivityOption activityOption, String intensityLevel) {
        return switch (intensityLevel) {
            case "LOW" -> activityOption.getLowMetValue();
            case "MEDIUM" -> activityOption.getMediumMetValue();
            case "HIGH" -> activityOption.getHighMetValue();
            default -> throw ExerciseException.invalidIntensityLevel();
        };
    }

    private Integer calculateCaloriesBurned(BigDecimal metValue, BigDecimal weightKg, Integer durationMinutes) {
        return metValue.multiply(weightKg).multiply(BigDecimal.valueOf(durationMinutes))
                .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP).intValue();
    }
}
