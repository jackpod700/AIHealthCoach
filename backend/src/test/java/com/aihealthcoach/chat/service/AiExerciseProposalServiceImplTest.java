package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aihealthcoach.exercise.dto.AiExerciseDto.ExerciseProposalResponse;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.exercise.entity.ExerciseActivityOption;
import com.aihealthcoach.exercise.mapper.ExerciseMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiExerciseProposalServiceImplTest {

    @Mock
    private ExerciseMapper exerciseMapper;

    @InjectMocks
    private AiExerciseProposalServiceImpl aiExerciseProposalService;

    @Test
    void createProposalSearchesActivityOptionsAndKeepsExtractedFields() {
        ExtractedExerciseResult extracted = new ExtractedExerciseResult(
                true,
                "케틀벨",
                "HIGH",
                LocalDate.of(2026, 6, 8),
                30,
                "퇴근 후 운동",
                BigDecimal.valueOf(0.91),
                List.of()
        );
        when(exerciseMapper.findExerciseActivityOptions("케틀벨")).thenReturn(List.of(
                ExerciseActivityOption.builder()
                        .id(7L)
                        .activityNameKo("케틀벨")
                        .majorHeading("Conditioning Exercise")
                        .highMetValue(BigDecimal.valueOf(8.0))
                        .highMetSource("COMPENDIUM")
                        .build()
        ));

        ExerciseProposalResponse response = aiExerciseProposalService.createProposal(extracted);

        assertThat(response.activityKeyword()).isEqualTo("케틀벨");
        assertThat(response.intensityLevel()).isEqualTo("HIGH");
        assertThat(response.exerciseDate()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(response.durationMinutes()).isEqualTo(30);
        assertThat(response.missingFields()).isEmpty();
        assertThat(response.candidates()).hasSize(1);
        assertThat(response.candidates().getFirst().activityNameKo()).isEqualTo("케틀벨");
        assertThat(response.candidates().getFirst().high().metValue()).isEqualByComparingTo("8.0");
    }

    @Test
    void createProposalMarksMissingDateDurationAndInvalidIntensity() {
        ExtractedExerciseResult extracted = new ExtractedExerciseResult(
                true,
                "걷기",
                "EASY",
                null,
                null,
                null,
                null,
                List.of()
        );
        when(exerciseMapper.findExerciseActivityOptions("걷기")).thenReturn(List.of());

        ExerciseProposalResponse response = aiExerciseProposalService.createProposal(extracted);

        assertThat(response.intensityLevel()).isNull();
        assertThat(response.missingFields()).containsExactlyInAnyOrder(
                "intensityLevel",
                "exerciseDate",
                "durationMinutes"
        );
        assertThat(response.candidates()).isEmpty();
    }

    @Test
    void createProposalReturnsNullWhenExerciseIntentIsFalse() {
        ExerciseProposalResponse response = aiExerciseProposalService.createProposal(ExtractedExerciseResult.noExercise());

        assertThat(response).isNull();
    }
}
