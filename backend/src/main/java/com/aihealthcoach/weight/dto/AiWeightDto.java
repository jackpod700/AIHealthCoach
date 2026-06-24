package com.aihealthcoach.weight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AiWeightDto {

    public record ExtractedWeightResult(
            boolean weightIntent,
            LocalDate recordDate,
            BigDecimal weightKg
    ) {
        public static ExtractedWeightResult noWeight() {
            return new ExtractedWeightResult(false, null, null);
        }
    }

    public record WeightProposalResponse(
            LocalDate recordDate,
            BigDecimal weightKg
    ) {
        public static WeightProposalResponse fromExtraction(ExtractedWeightResult extraction) {
            if (extraction == null || !extraction.weightIntent() || extraction.weightKg() == null) {
                return null;
            }

            return new WeightProposalResponse(
                    extraction.recordDate(),
                    extraction.weightKg()
            );
        }
    }
}
