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
public class PhysicalActivity {
    private Long id;
    private String compendiumCode;
    private String compendiumVersion;
    private String majorHeading;
    private BigDecimal metValue;
    private String description;
}
