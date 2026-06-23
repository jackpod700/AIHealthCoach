package com.aihealthcoach.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.admin.dto.AdminFoodImportCandidateDto.AdminFoodImportCandidateApproveRequest;
import com.aihealthcoach.admin.dto.AdminFoodImportCandidateDto.AdminFoodImportCandidateRejectRequest;
import com.aihealthcoach.admin.service.AdminFoodImportCandidateService;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateGroupResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidatePageResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/food-import-candidates")
@RequiredArgsConstructor
@Validated
public class AdminFoodImportCandidateController {

    private final AdminFoodImportCandidateService adminFoodImportCandidateService;

    @GetMapping
    public ResponseEntity<FoodImportCandidatePageResponse> findCandidateGroups(
            @RequestParam(required = false, defaultValue = "PENDING_REVIEW") String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminFoodImportCandidateService.findCandidateGroups(status, page, size));
    }

    @PostMapping("/{candidateId}/approve")
    public ResponseEntity<FoodImportCandidateResponse> approveCandidate(
            Authentication authentication,
            @PathVariable Long candidateId
    ) {
        Long adminId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminFoodImportCandidateService.approveCandidate(adminId, candidateId));
    }

    @PostMapping("/search-misses/{searchMissId}/approve")
    public ResponseEntity<FoodImportCandidateGroupResponse> approveCandidates(
            Authentication authentication,
            @PathVariable Long searchMissId,
            @RequestBody(required = false) AdminFoodImportCandidateApproveRequest request
    ) {
        Long adminId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminFoodImportCandidateService.approveCandidates(
                adminId,
                searchMissId,
                request == null ? null : request.candidateIds()
        ));
    }

    @PostMapping("/search-misses/{searchMissId}/reject")
    public ResponseEntity<FoodImportCandidateGroupResponse> rejectSearchMiss(
            Authentication authentication,
            @PathVariable Long searchMissId,
            @RequestBody(required = false) AdminFoodImportCandidateRejectRequest request
    ) {
        Long adminId = (Long) authentication.getPrincipal();
        String rejectionReason = request == null ? null : request.rejectionReason();
        return ResponseEntity.ok(adminFoodImportCandidateService.rejectSearchMiss(adminId, searchMissId, rejectionReason));
    }
}
