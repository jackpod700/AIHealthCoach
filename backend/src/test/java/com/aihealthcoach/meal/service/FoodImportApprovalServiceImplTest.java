package com.aihealthcoach.meal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateGroupResponse;
import com.aihealthcoach.meal.entity.FoodImportCandidate;
import com.aihealthcoach.meal.entity.FoodSearchMiss;
import com.aihealthcoach.meal.mapper.FoodImportMapper;

@ExtendWith(MockitoExtension.class)
class FoodImportApprovalServiceImplTest {

    @Mock
    private FoodImportMapper foodImportMapper;

    @InjectMocks
    private FoodImportApprovalServiceImpl foodImportApprovalService;

    @Test
    void approveCandidatesInsertFoodsAndRejectUnselectedCandidates() {
        FoodImportCandidate pending = candidate(100L, "PENDING", "fatsecret:100", "hash-100");
        FoodImportCandidate duplicate = candidate(101L, "DUPLICATE", "fatsecret:101", "hash-101");
        duplicate.setDuplicateFoodId(88L);
        FoodImportCandidate approved = candidate(100L, "APPROVED", "fatsecret:100", "hash-100");
        approved.setApprovedFoodId(77L);
        FoodImportCandidate approvedDuplicate = candidate(101L, "APPROVED", "fatsecret:101", "hash-101");
        approvedDuplicate.setApprovedFoodId(88L);
        FoodImportCandidate rejected = candidate(102L, "REJECTED", "fatsecret:102", "hash-102");

        when(foodImportMapper.findFoodSearchMissByIdForUpdate(10L)).thenReturn(searchMiss("PENDING_REVIEW"));
        when(foodImportMapper.findFoodImportCandidatesByIdsForUpdate(List.of(100L, 101L))).thenReturn(List.of(pending, duplicate));
        when(foodImportMapper.findFoodIdByContentHash("hash-100")).thenReturn(null);
        when(foodImportMapper.findFoodIdBySourceKeyServing("fatsecret:100", "100g")).thenReturn(null);
        when(foodImportMapper.insertApprovedFood(
                eq("fatsecret:100"),
                eq("https://example.test/food/123"),
                eq("Apple"),
                eq(null),
                eq("100g"),
                eq(new BigDecimal("100")),
                eq("g"),
                eq(new BigDecimal("52")),
                eq(new BigDecimal("0.17")),
                eq(new BigDecimal("13.81")),
                eq(new BigDecimal("0.26")),
                eq("hash-100")
        )).thenReturn(77L);
        when(foodImportMapper.approveFoodImportCandidate(100L, 1L, 77L)).thenReturn(1);
        when(foodImportMapper.approveFoodImportCandidate(101L, 1L, 88L)).thenReturn(1);
        when(foodImportMapper.findFoodSearchMissById(10L)).thenReturn(searchMiss("APPROVED"));
        when(foodImportMapper.findFoodImportCandidatesBySearchMissIds(List.of(10L)))
                .thenReturn(List.of(approved, approvedDuplicate, rejected));

        FoodImportCandidateGroupResponse response = foodImportApprovalService.approveCandidates(
                1L,
                10L,
                List.of(100L, 101L)
        );

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.candidates())
                .extracting(FoodImportCandidateResponse::status)
                .containsExactly("APPROVED", "APPROVED", "REJECTED");
        verify(foodImportMapper).rejectUnselectedFoodImportCandidates(10L, List.of(100L, 101L), 1L);
        verify(foodImportMapper).markFoodSearchMissApproved(10L);
    }

    @Test
    void approveCandidateLinksExistingContentHashFood() {
        FoodImportCandidate pending = candidate(100L, "DUPLICATE", "fatsecret:123", "hash");
        pending.setDuplicateFoodId(88L);
        FoodImportCandidate approved = candidate(100L, "APPROVED", "fatsecret:123", "hash");
        approved.setApprovedFoodId(88L);

        when(foodImportMapper.findSearchMissIdByFoodImportCandidateId(100L)).thenReturn(10L);
        when(foodImportMapper.findFoodSearchMissByIdForUpdate(10L)).thenReturn(searchMiss("PENDING_REVIEW"));
        when(foodImportMapper.findFoodImportCandidatesByIdsForUpdate(List.of(100L))).thenReturn(List.of(pending));
        when(foodImportMapper.approveFoodImportCandidate(100L, 1L, 88L)).thenReturn(1);
        when(foodImportMapper.findFoodSearchMissById(10L)).thenReturn(searchMiss("APPROVED"));
        when(foodImportMapper.findFoodImportCandidatesBySearchMissIds(List.of(10L))).thenReturn(List.of(approved));

        FoodImportCandidateResponse response = foodImportApprovalService.approveCandidate(1L, 100L);

        assertThat(response.approvedFoodId()).isEqualTo(88L);
        verify(foodImportMapper, org.mockito.Mockito.never()).insertApprovedFood(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void rejectSearchMissRejectsSearchMissAndCandidates() {
        FoodImportCandidate rejectedCandidate = candidate(100L, "REJECTED", "fatsecret:123", "hash");

        when(foodImportMapper.findFoodSearchMissByIdForUpdate(10L)).thenReturn(searchMiss("PENDING_REVIEW"));
        when(foodImportMapper.findFoodSearchMissById(10L)).thenReturn(searchMiss("REJECTED"));
        when(foodImportMapper.findFoodImportCandidatesBySearchMissIds(List.of(10L)))
                .thenReturn(List.of(rejectedCandidate));

        FoodImportCandidateGroupResponse response = foodImportApprovalService.rejectSearchMiss(
                1L,
                10L,
                "not a good match"
        );

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.candidates()).hasSize(1);
        assertThat(response.candidates().get(0).status()).isEqualTo("REJECTED");
        verify(foodImportMapper).rejectFoodImportCandidatesBySearchMiss(10L, 1L, "not a good match");
        verify(foodImportMapper).markFoodSearchMissRejected(10L);
    }

    private FoodSearchMiss searchMiss(String status) {
        return FoodSearchMiss.builder()
                .id(10L)
                .status(status)
                .build();
    }

    private FoodImportCandidate candidate(Long id, String status, String sourceKey, String contentHash) {
        return FoodImportCandidate.builder()
                .id(id)
                .searchMissId(10L)
                .status(status)
                .sourceProvider("FATSECRET")
                .sourceKey(sourceKey)
                .sourceUrl("https://example.test/food/123")
                .name("Apple")
                .servingDescription("100g")
                .servingSize(new BigDecimal("100"))
                .servingUnit("g")
                .calories(new BigDecimal("52"))
                .fat(new BigDecimal("0.17"))
                .carbohydrate(new BigDecimal("13.81"))
                .protein(new BigDecimal("0.26"))
                .contentHash(contentHash)
                .build();
    }
}
