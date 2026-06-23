package com.aihealthcoach.admin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aihealthcoach.admin.service.AdminFoodImportCandidateService;
import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateGroupResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidatePageResponse;
import com.aihealthcoach.meal.dto.FoodImportCandidateDto.FoodImportCandidateResponse;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.mapper.UserMapper;

@WebMvcTest(AdminFoodImportCandidateController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class AdminFoodImportCandidateControllerTest {

    private static final String TOKEN = "admin-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminFoodImportCandidateService adminFoodImportCandidateService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void findCandidateGroupsAllowsAdminRole() throws Exception {
        mockAdminToken();
        when(adminFoodImportCandidateService.findCandidateGroups("PENDING_REVIEW", 1, 20))
                .thenReturn(new FoodImportCandidatePageResponse(
                        List.of(new FoodImportCandidateGroupResponse(
                                10L,
                                "apple",
                                "apple",
                                "PENDING_REVIEW",
                                3,
                                null,
                                List.of()
                        )),
                        1,
                        20,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/admin/food-import-candidates")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].searchMissId").value(10))
                .andExpect(jsonPath("$.data.items[0].query").value("apple"));
    }

    @Test
    void approveCandidateUsesAuthenticatedAdminId() throws Exception {
        mockAdminToken();
        when(adminFoodImportCandidateService.approveCandidate(1L, 100L))
                .thenReturn(new FoodImportCandidateResponse(
                        100L,
                        10L,
                        "APPROVED",
                        "FATSECRET",
                        "fatsecret:123",
                        null,
                        "Generic",
                        "Apple",
                        null,
                        null,
                        "100g",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "hash",
                        null,
                        77L,
                        null,
                        null
                ));

        mockMvc.perform(post("/api/admin/food-import-candidates/100/approve")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedFoodId").value(77));
    }

    @Test
    void approveCandidatesUsesAuthenticatedAdminId() throws Exception {
        mockAdminToken();
        when(adminFoodImportCandidateService.approveCandidates(1L, 10L, List.of(100L, 101L)))
                .thenReturn(new FoodImportCandidateGroupResponse(
                        10L,
                        "apple",
                        "apple",
                        "APPROVED",
                        3,
                        null,
                        List.of(
                                new FoodImportCandidateResponse(
                                        100L,
                                        10L,
                                        "APPROVED",
                                        "FATSECRET",
                                        "fatsecret:100",
                                        null,
                                        "Generic",
                                        "Apple",
                                        null,
                                        null,
                                        "100g",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "hash-100",
                                        null,
                                        77L,
                                        null,
                                        null
                                ),
                                new FoodImportCandidateResponse(
                                        101L,
                                        10L,
                                        "APPROVED",
                                        "FATSECRET",
                                        "fatsecret:101",
                                        null,
                                        "Generic",
                                        "Apple Juice",
                                        null,
                                        null,
                                        "100ml",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "hash-101",
                                        null,
                                        78L,
                                        null,
                                        null
                                )
                        )
                ));

        mockMvc.perform(post("/api/admin/food-import-candidates/search-misses/10/approve")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateIds\":[100,101]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.candidates[0].approvedFoodId").value(77))
                .andExpect(jsonPath("$.data.candidates[1].approvedFoodId").value(78));
    }

    private void mockAdminToken() {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);
        when(jwtTokenProvider.getTokenId(TOKEN)).thenReturn("token-id");
        when(tokenRedisRepository.isAccessTokenBlacklisted("token-id")).thenReturn(false);
        when(userMapper.findUserById(1L)).thenReturn(User.builder()
                .id(1L)
                .email("admin@example.com")
                .nickname("admin")
                .role("ADMIN")
                .build());
    }
}
