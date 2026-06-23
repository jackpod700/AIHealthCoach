package com.aihealthcoach.admin.service;

import java.util.List;

import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateGroupResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidatePageResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateResponse;

public interface AdminFoodImportCandidateService {

    FoodImportCandidatePageResponse findCandidateGroups(String status, int page, int size);

    FoodImportCandidateResponse approveCandidate(Long adminId, Long candidateId);

    FoodImportCandidateGroupResponse approveCandidates(Long adminId, Long searchMissId, List<Long> candidateIds);

    FoodImportCandidateGroupResponse rejectSearchMiss(Long adminId, Long searchMissId, String rejectionReason);
}
