package com.aihealthcoach.weight.dto;

import com.aihealthcoach.weight.entity.WeightRecord;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WeightRecordDto {

    public record WeightRecordRequest(
            @NotNull(message = "recordDate는 필수입니다.")
            LocalDate recordDate,

            @NotNull(message = "weightKg는 필수입니다.")
            @DecimalMin(value = "0.01", message = "weightKg는 0보다 커야 합니다.")
            @DecimalMax(value = "500.00", message = "weightKg는 500 이하이어야 합니다.")
            BigDecimal weightKg
    ) {
    }

    public record WeightRecordDeleteRequest(
            @NotNull(message = "recordDate는 필수입니다.")
            LocalDate recordDate
    ) {
    }

    public record WeightRecordResponse(
            LocalDate recordDate,
            BigDecimal weightKg,
            LocalDateTime updatedAt
    ) {
        public static WeightRecordResponse fromEntity(WeightRecord entity) {
            return new WeightRecordResponse(
                    entity.getRecordDate(),
                    entity.getWeightKg(),
                    entity.getUpdatedAt()
            );
        }
    }
}
