package com.aihealthcoach.meal.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.meal.dto.AiMealDto.FoodCandidateResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodGroupResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodSearchPageResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodServingResponse;
import com.aihealthcoach.meal.service.FoodService;

@WebMvcTest(FoodController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        com.aihealthcoach.common.config.OAuthWebMvcTestConfig.class})
class FoodControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FoodService foodService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void searchFoodGroupsReturnsGroupedFoods() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);
        when(foodService.searchFoodGroups("chicken", 1, 20))
                .thenReturn(new FoodSearchPageResponse(
                        List.of(new FoodGroupResponse(
                                "source-1",
                                "Chicken Breast",
                                "Brand",
                                List.of(
                                        serving(1L, "100g", "100", "g", "120"),
                                        serving(2L, "1 serving", "1", "serving", "180")
                                )
                        )),
                        1,
                        20,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/foods")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("query", "chicken")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].sourceKey").value("source-1"))
                .andExpect(jsonPath("$.data.items[0].foodName").value("Chicken Breast"))
                .andExpect(jsonPath("$.data.items[0].servings[0].foodId").value(1))
                .andExpect(jsonPath("$.data.items[0].servings[0].servingDescription").value("100g"))
                .andExpect(jsonPath("$.data.items[0].servings[1].servingUnit").value("serving"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(foodService).searchFoodGroups("chicken", 1, 20);
    }

    @Test
    void searchFoodsKeepsCandidateResponseShape() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);
        when(foodService.searchFoods(" chicken   brand "))
                .thenReturn(List.of(new FoodCandidateResponse(
                        1234567890123456789L,
                        "Chicken Breast",
                        "Brand",
                        new BigDecimal("100"),
                        "g",
                        new BigDecimal("120"),
                        BigDecimal.ZERO,
                        new BigDecimal("23"),
                        new BigDecimal("2")
                )));

        mockMvc.perform(get("/api/foods/search")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("query", " chicken   brand "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].foodId").value("1234567890123456789"))
                .andExpect(jsonPath("$.data[0].foodName").value("Chicken Breast"))
                .andExpect(jsonPath("$.data[0].servingSize").value(100))
                .andExpect(jsonPath("$.data[0].servingUnit").value("g"))
                .andExpect(jsonPath("$.data[0].calories").value(120));

        verify(foodService).searchFoods(" chicken   brand ");
    }

    @Test
    void countFoodsReturnsCandidateSearchCount() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);
        when(foodService.countFoods(" chicken   brand ")).thenReturn(42L);

        mockMvc.perform(get("/api/foods/search/count")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("query", " chicken   brand "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(42));

        verify(foodService).countFoods(" chicken   brand ");
    }

    private FoodServingResponse serving(
            Long foodId,
            String servingDescription,
            String servingSize,
            String servingUnit,
            String calories
    ) {
        return new FoodServingResponse(
                foodId,
                servingDescription,
                new BigDecimal(servingSize),
                servingUnit,
                new BigDecimal(calories),
                BigDecimal.ZERO,
                new BigDecimal("23"),
                new BigDecimal("2")
        );
    }
}
