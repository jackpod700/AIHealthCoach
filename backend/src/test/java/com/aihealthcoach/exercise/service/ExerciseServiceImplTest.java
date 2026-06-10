package com.aihealthcoach.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.entity.ExerciseActivityOption;
import com.aihealthcoach.exercise.entity.ExerciseRecord;
import com.aihealthcoach.exercise.exception.ExerciseException;
import com.aihealthcoach.exercise.mapper.ExerciseMapper;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceImplTest {

    @Mock
    private ExerciseMapper exerciseDao;

    @Mock
    private UserMapper userDao;

    @InjectMocks
    private ExerciseServiceImpl exerciseService;

    @Test
    void insertExerciseRecordCalculatesCaloriesBurned() {
        ExerciseActivityOption activityOption = ExerciseActivityOption.builder()
                .id(1L)
                .activityNameKo("걷기")
                .mediumPhysicalActivityId(11L)
                .mediumMetValue(BigDecimal.valueOf(3.8))
                .mediumMetSource("COMPENDIUM")
                .mediumSourceDescription("Walking, 2.8 to 3.4 mph, level, moderate pace, firm surface")
                .build();

        UserProfile userProfile = UserProfile.builder().userId(1L).currentWeightKg(BigDecimal.valueOf(70)).build();

        when(exerciseDao.findExerciseActivityOptionById(1L)).thenReturn(activityOption);
        when(userDao.findUserProfileByUserId(1L)).thenReturn(userProfile);

        ArgumentCaptor<ExerciseRecord> recordCaptor = ArgumentCaptor.forClass(ExerciseRecord.class);
        when(exerciseDao.insertExerciseRecord(recordCaptor.capture())).thenAnswer(invocation -> {
            ExerciseRecord record = invocation.getArgument(0);
            record.setId(10L);
            record.setExerciseActivityOption(activityOption);
            return record;
        });

        ExerciseRecordResponse response = exerciseService.insertExerciseRecord(1L,
                ExerciseRecordRequest.builder().exerciseActivityOptionId(1L).intensityLevel("MEDIUM")
                        .exerciseDate(LocalDate.of(2026, 6, 1))
                        .durationMinutes(30).memo("퇴근 후 걷기").build());

        ExerciseRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getExerciseActivityOptionId()).isEqualTo(1L);
        assertThat(savedRecord.getIntensityLevel()).isEqualTo("MEDIUM");
        assertThat(savedRecord.getCaloriesBurned()).isEqualTo(133);
        assertThat(response.caloriesBurned()).isEqualTo(133);
        assertThat(response.activityNameKo()).isEqualTo("걷기");
        assertThat(response.metValue()).isEqualByComparingTo("3.8");
    }

    @Test
    void updateExerciseRecordRecalculatesCaloriesBurned() {
        ExerciseActivityOption activityOption = ExerciseActivityOption.builder()
                .id(1L)
                .activityNameKo("걷기")
                .highPhysicalActivityId(12L)
                .highMetValue(BigDecimal.valueOf(6.0))
                .highMetSource("COMPENDIUM")
                .highSourceDescription("Walking, steep uphill")
                .build();

        UserProfile userProfile = UserProfile.builder().userId(1L).currentWeightKg(BigDecimal.valueOf(70)).build();

        when(exerciseDao.findExerciseActivityOptionById(1L)).thenReturn(activityOption);
        when(userDao.findUserProfileByUserId(1L)).thenReturn(userProfile);

        ArgumentCaptor<ExerciseRecord> recordCaptor = ArgumentCaptor.forClass(ExerciseRecord.class);
        when(exerciseDao.updateExerciseRecord(recordCaptor.capture())).thenAnswer(invocation -> {
            ExerciseRecord record = invocation.getArgument(0);
            record.setExerciseActivityOption(activityOption);
            return record;
        });

        ExerciseRecordResponse response = exerciseService.updateExerciseRecord(1L, 10L,
                ExerciseRecordRequest.builder().exerciseActivityOptionId(1L).intensityLevel("HIGH")
                        .exerciseDate(LocalDate.of(2026, 6, 2))
                        .durationMinutes(30).memo("언덕 걷기").build());

        ExerciseRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getId()).isEqualTo(10L);
        assertThat(savedRecord.getUserId()).isEqualTo(1L);
        assertThat(savedRecord.getIntensityLevel()).isEqualTo("HIGH");
        assertThat(savedRecord.getCaloriesBurned()).isEqualTo(210);
        assertThat(response.caloriesBurned()).isEqualTo(210);
        assertThat(response.activityNameKo()).isEqualTo("걷기");
        assertThat(response.metValue()).isEqualByComparingTo("6.0");
    }

    @Test
    void updateExerciseRecordRejectsMissingRecord() {
        ExerciseActivityOption activityOption = ExerciseActivityOption.builder()
                .id(1L)
                .activityNameKo("걷기")
                .mediumMetValue(BigDecimal.valueOf(3.8))
                .build();

        UserProfile userProfile = UserProfile.builder().userId(1L).currentWeightKg(BigDecimal.valueOf(70)).build();

        when(exerciseDao.findExerciseActivityOptionById(1L)).thenReturn(activityOption);
        when(userDao.findUserProfileByUserId(1L)).thenReturn(userProfile);
        when(exerciseDao.updateExerciseRecord(any(ExerciseRecord.class))).thenReturn(null);

        assertThatThrownBy(() -> exerciseService.updateExerciseRecord(1L, 10L,
                ExerciseRecordRequest.builder().exerciseActivityOptionId(1L).intensityLevel("MEDIUM")
                        .exerciseDate(LocalDate.of(2026, 6, 2))
                        .durationMinutes(30).build()))
                .isInstanceOf(ExerciseException.class)
                .hasMessage("운동 기록이 존재하지 않습니다.");
    }
}
