package com.aihealthcoach.exercise.service;

import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordUpdateRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseActivityOptionResponse;
import com.aihealthcoach.exercise.entity.ExerciseActivityOption;
import com.aihealthcoach.exercise.entity.ExerciseRecord;
import com.aihealthcoach.exercise.exception.ExerciseException;
import com.aihealthcoach.exercise.mapper.ExerciseMapper;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.mapper.UserMapper;
import com.aihealthcoach.weight.mapper.WeightRecordMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.summary.service.DailyChatSummaryStateService;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseMapper exerciseDao;
    private final UserMapper userDao;
    private final WeightRecordMapper weightRecordDao;
    private final DailyChatSummaryStateService dailyChatSummaryStateService;

    @Override
    public List<ExerciseActivityOptionResponse> findExerciseActivityOptions(String keyword) {
        return exerciseDao.findExerciseActivityOptions(keyword).stream()
                .map(ExerciseActivityOptionResponse::fromEntity).toList();
    }

    @Override
    @Transactional
    public ExerciseRecordResponse insertExerciseRecord(Long userId, ExerciseRecordRequest request) {
        ExerciseRecord newRecord = buildExerciseRecord(userId, null, request);
        ExerciseRecord savedRecord = exerciseDao.insertExerciseRecord(newRecord);
        dailyChatSummaryStateService.markChanged(userId, savedRecord.getExerciseDate());

        return ExerciseRecordResponse.fromEntity(savedRecord);
    }

    @Override
    @Transactional
    public ExerciseRecordResponse updateExerciseRecord(Long userId, Long recordId, ExerciseRecordUpdateRequest request) {
        LocalDate existingExerciseDate = exerciseDao.findExerciseDateById(userId, recordId);
        if (existingExerciseDate == null) {
            throw ExerciseException.exerciseRecordNotFound();
        }

        ExerciseRecord updateRecord = buildExerciseRecord(
                userId,
                recordId,
                existingExerciseDate,
                request.exerciseActivityOptionId(),
                request.intensityLevel(),
                request.durationMinutes(),
                request.memo());
        ExerciseRecord savedRecord = exerciseDao.updateExerciseRecord(updateRecord);

        if (savedRecord == null) {
            throw ExerciseException.exerciseRecordNotFound();
        }

        dailyChatSummaryStateService.markChanged(userId, existingExerciseDate);

        return ExerciseRecordResponse.fromEntity(savedRecord);
    }

    @Override
    @Transactional
    public void deleteExerciseRecord(Long userId, Long recordId) {
        LocalDate existingExerciseDate = exerciseDao.findExerciseDateById(userId, recordId);
        if (existingExerciseDate == null) {
            throw ExerciseException.exerciseRecordNotFound();
        }

        int deletedRows = exerciseDao.deleteExerciseRecord(userId, recordId);

        if (deletedRows == 0) {
            throw ExerciseException.exerciseRecordNotFound();
        }

        dailyChatSummaryStateService.markChanged(userId, existingExerciseDate);
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

    private ExerciseRecord buildExerciseRecord(Long userId, Long recordId, ExerciseRecordRequest request) {
        return buildExerciseRecord(
                userId,
                recordId,
                request.exerciseDate(),
                request.exerciseActivityOptionId(),
                request.intensityLevel(),
                request.durationMinutes(),
                request.memo());
    }

    private ExerciseRecord buildExerciseRecord(
            Long userId,
            Long recordId,
            LocalDate exerciseDate,
            Long exerciseActivityOptionId,
            String intensityLevel,
            Integer durationMinutes,
            String memo) {
        ExerciseActivityOption activityOption = exerciseDao.findExerciseActivityOptionById(
                exerciseActivityOptionId);

        if (activityOption == null) {
            throw ExerciseException.exerciseActivityOptionNotFound();
        }

        UserProfile userProfile = userDao.findUserProfileByUserId(userId);

        if (userProfile == null) {
            throw UserException.profileNotFound();
        }

        BigDecimal metValue = resolveMetValue(activityOption, intensityLevel);
        BigDecimal weightKg = resolveWeightForExerciseDate(userId, exerciseDate, userProfile);
        Integer caloriesBurned = calculateCaloriesBurned(metValue, weightKg, durationMinutes);

        return ExerciseRecord.builder()
                .id(recordId)
                .userId(userId)
                .exerciseActivityOptionId(exerciseActivityOptionId)
                .intensityLevel(intensityLevel)
                .exerciseDate(exerciseDate)
                .durationMinutes(durationMinutes)
                .caloriesBurned(caloriesBurned)
                .memo(memo)
                .build();
    }

    private BigDecimal resolveWeightForExerciseDate(Long userId, LocalDate exerciseDate, UserProfile userProfile) {
        BigDecimal weightKg = weightRecordDao.findLatestWeightOnOrBefore(userId, exerciseDate);

        if (weightKg != null) {
            return weightKg;
        }

        if (userProfile.getCurrentWeightKg() != null) {
            return userProfile.getCurrentWeightKg();
        }

        throw ExerciseException.userWeightNotFound();
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
