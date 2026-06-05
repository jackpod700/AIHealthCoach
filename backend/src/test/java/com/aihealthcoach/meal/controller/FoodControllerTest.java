package com.aihealthcoach.meal.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import com.aihealthcoach.meal.entity.Food;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.meal.util.FoodSearchQuery.Token;

@WebMvcTest(FoodController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class FoodControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealMapper mealMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void searchFoodsReturnsCandidatesWithoutLimit() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);
        List<Token> tokens = List.of(new Token("chicken", "chicken"), new Token("brand", "brand"));
        when(mealMapper.searchFoods("chicken brand", tokens))
                .thenReturn(List.of(food()));

        mockMvc.perform(get("/api/foods/search")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("query", " chicken   brand "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].foodCode").value("1234567890123456789"))
                .andExpect(jsonPath("$.data[0].foodName").value("Chicken Breast"))
                .andExpect(jsonPath("$.data[0].servingSize").value(100))
                .andExpect(jsonPath("$.data[0].servingUnit").value("g"))
                .andExpect(jsonPath("$.data[0].calories").value(120));

        verify(mealMapper).searchFoods("chicken brand", tokens);
    }

    @Test
    void searchFoodsReturnsEmptyForBlankQuery() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);

        mockMvc.perform(get("/api/foods/search")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("query", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(mealMapper, never()).searchFoods(eq(""), eq(List.of()));
    }

    private Food food() {
        Food row = new Food();
        row.setFoodCode("1234567890123456789");
        row.setFoodName("Chicken Breast");
        row.setManufacturer("Brand");
        row.setServingSize(new BigDecimal("100"));
        row.setServingUnit("g");
        row.setCalories(new BigDecimal("120"));
        row.setCarbohydrate(BigDecimal.ZERO);
        row.setProtein(new BigDecimal("23"));
        row.setFat(new BigDecimal("2"));
        return row;
    }
}
