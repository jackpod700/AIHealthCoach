package com.aihealthcoach.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordRequest;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.entity.ExerciseRecord;
import com.aihealthcoach.exercise.entity.PhysicalActivity;
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
        PhysicalActivity activity = PhysicalActivity.builder().id(1L).compendiumCode("17190")
                .compendiumVersion("2024_ADULT").metValue(BigDecimal.valueOf(3.8))
                .description("Walking, 2.8 to 3.4 mph, level, moderate pace, firm surface").build();

        UserProfile userProfile = UserProfile.builder().userId(1L).currentWeightKg(BigDecimal.valueOf(70)).build();

        when(exerciseDao.findPhysicalActivityById(1L)).thenReturn(activity);
        when(userDao.findUserProfileByUserId(1L)).thenReturn(userProfile);

        ArgumentCaptor<ExerciseRecord> recordCaptor = ArgumentCaptor.forClass(ExerciseRecord.class);
        when(exerciseDao.insertExerciseRecord(recordCaptor.capture())).thenAnswer(invocation -> {
            ExerciseRecord record = invocation.getArgument(0);
            record.setId(10L);
            record.setPhysicalActivity(activity);
            return record;
        });

        ExerciseRecordResponse response = exerciseService.insertExerciseRecord(1L,
                ExerciseRecordRequest.builder().physicalActivityId(1L).exerciseDate(LocalDate.of(2026, 6, 1))
                        .durationMinutes(30).memo("퇴근 후 걷기").build());

        ExerciseRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getCaloriesBurned()).isEqualTo(133);
        assertThat(response.caloriesBurned()).isEqualTo(133);
        assertThat(response.compendiumCode()).isEqualTo("17190");
    }
}
