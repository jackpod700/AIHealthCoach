package com.aihealthcoach.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionReviewRequest;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionResponse;
import com.aihealthcoach.meal.entity.FoodSubmissionRequest;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.FoodSubmissionMapper;

@ExtendWith(MockitoExtension.class)
class AdminFoodSubmissionServiceImplTest {

    @Mock
    private FoodSubmissionMapper foodSubmissionMapper;

    @InjectMocks
    private AdminFoodSubmissionServiceImpl adminFoodSubmissionService;

    @Test
    void approveRequestInsertsUserSubmittedFoodAndMarksRequestApproved() {
        FoodSubmissionRequest pending = request(10L, "PENDING");
        FoodSubmissionRequest approved = request(10L, "APPROVED");
        approved.setApprovedFoodId(99L);
        FoodSubmissionReviewRequest reviewRequest = reviewRequest();

        when(foodSubmissionMapper.findFoodSubmissionRequestByIdForUpdate(10L)).thenReturn(pending);
        when(foodSubmissionMapper.findFoodIdBySourceKeyServing(any(), eq("100g"))).thenReturn(null);
        when(foodSubmissionMapper.insertApprovedFood(
                any(), any(), eq("테스트 음식"), eq("테스트 브랜드"), eq("100g"),
                eq(new BigDecimal("100")), eq("g"), eq(new BigDecimal("120")),
                eq(new BigDecimal("10")), eq(new BigDecimal("20")), eq(new BigDecimal("3")), any()
        )).thenReturn(99L);
        when(foodSubmissionMapper.approveFoodSubmissionRequest(
                eq(10L), eq(1L), eq(99L), eq("테스트 음식"), eq("테스트 브랜드"), eq("100g"),
                eq(new BigDecimal("100")), eq("g"), eq(new BigDecimal("120")),
                eq(new BigDecimal("10")), eq(new BigDecimal("20")), eq(new BigDecimal("3")), eq("확인 완료")
        )).thenReturn(1);
        when(foodSubmissionMapper.findFoodSubmissionRequestById(10L)).thenReturn(approved);

        FoodSubmissionResponse response = adminFoodSubmissionService.approveRequest(1L, 10L, reviewRequest);

        ArgumentCaptor<String> sourceUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(foodSubmissionMapper).insertApprovedFood(
                any(), sourceUrlCaptor.capture(), eq("테스트 음식"), eq("테스트 브랜드"), eq("100g"),
                eq(new BigDecimal("100")), eq("g"), eq(new BigDecimal("120")),
                eq(new BigDecimal("10")), eq(new BigDecimal("20")), eq(new BigDecimal("3")), any()
        );
        assertThat(sourceUrlCaptor.getValue()).isEqualTo("user-submission://food-requests/10");
        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(response.approvedFoodId()).isEqualTo(99L);
    }

    @Test
    void approveRequestRejectsAlreadyReviewedRequest() {
        when(foodSubmissionMapper.findFoodSubmissionRequestByIdForUpdate(10L)).thenReturn(request(10L, "APPROVED"));

        assertThatThrownBy(() -> adminFoodSubmissionService.approveRequest(1L, 10L, reviewRequest()))
                .isInstanceOf(MealException.class);
        verify(foodSubmissionMapper, never()).insertApprovedFood(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    private FoodSubmissionReviewRequest reviewRequest() {
        return new FoodSubmissionReviewRequest(
                " 테스트 음식 ",
                " 테스트 브랜드 ",
                "100g",
                new BigDecimal("100"),
                "g",
                new BigDecimal("120"),
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("3"),
                " 확인 완료 "
        );
    }

    private FoodSubmissionRequest request(Long id, String status) {
        return FoodSubmissionRequest.builder()
                .id(id)
                .submittedByUserId(2L)
                .status(status)
                .name("테스트 음식")
                .brand("테스트 브랜드")
                .servingDescription("100g")
                .servingSize(new BigDecimal("100"))
                .servingUnit("g")
                .calories(new BigDecimal("120"))
                .carbohydrate(new BigDecimal("10"))
                .protein(new BigDecimal("20"))
                .fat(new BigDecimal("3"))
                .build();
    }
}
