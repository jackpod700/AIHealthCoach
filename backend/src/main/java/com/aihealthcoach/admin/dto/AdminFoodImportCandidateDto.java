package com.aihealthcoach.admin.dto;

import java.util.List;

public class AdminFoodImportCandidateDto {

    private AdminFoodImportCandidateDto() {
    }

    public record AdminFoodImportCandidateApproveRequest(
            List<Long> candidateIds
    ) {
    }

    public record AdminFoodImportCandidateRejectRequest(
            String rejectionReason
    ) {
    }
}
