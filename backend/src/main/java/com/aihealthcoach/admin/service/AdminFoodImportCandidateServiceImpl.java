package com.aihealthcoach.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateGroupResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidatePageResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateResponse;
import com.aihealthcoach.meal.service.FoodImportApprovalService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminFoodImportCandidateServiceImpl implements AdminFoodImportCandidateService {

    private final FoodImportApprovalService foodImportApprovalService;

    @Override
    public FoodImportCandidatePageResponse findCandidateGroups(String status, int page, int size) {
        return foodImportApprovalService.findCandidateGroups(status, page, size);
    }

    @Override
    public FoodImportCandidateResponse approveCandidate(Long adminId, Long candidateId) {
        return foodImportApprovalService.approveCandidate(adminId, candidateId);
    }

    @Override
    public FoodImportCandidateGroupResponse approveCandidates(Long adminId, Long searchMissId, List<Long> candidateIds) {
        return foodImportApprovalService.approveCandidates(adminId, searchMissId, candidateIds);
    }

    @Override
    public FoodImportCandidateGroupResponse rejectSearchMiss(Long adminId, Long searchMissId, String rejectionReason) {
        return foodImportApprovalService.rejectSearchMiss(adminId, searchMissId, rejectionReason);
    }
}
