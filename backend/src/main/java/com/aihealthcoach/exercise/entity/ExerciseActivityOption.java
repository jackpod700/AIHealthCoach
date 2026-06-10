package com.aihealthcoach.exercise.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseActivityOption {
    private Long id;
    private String activityNameKo;
    private String majorHeading;
    private Long lowPhysicalActivityId;
    private BigDecimal lowMetValue;
    private String lowSourceDescription;
    private String lowMetSource;
    private Long mediumPhysicalActivityId;
    private BigDecimal mediumMetValue;
    private String mediumSourceDescription;
    private String mediumMetSource;
    private Long highPhysicalActivityId;
    private BigDecimal highMetValue;
    private String highSourceDescription;
    private String highMetSource;
}
