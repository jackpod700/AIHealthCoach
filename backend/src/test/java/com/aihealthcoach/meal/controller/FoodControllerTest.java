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

import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.config.SecurityConfig;
import com.aihealthcoach.meal.dto.FoodCandidateRow;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.meal.util.FoodSearchQuery.Token;

@WebMvcTest(FoodController.class)
@Import(SecurityConfig.class)
class FoodControllerTest {

    private static final String TOKEN = "access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MealMapper mealMapper;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void searchFoodsReturnsCandidatesWithoutLimit() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);
        List<Token> tokens = List.of(new Token("닭가슴살", "닭가슴살"), new Token("하림", "하림"));
        when(mealMapper.searchFoods("닭가슴살 하림", tokens))
                .thenReturn(List.of(food()));

        mockMvc.perform(get("/api/foods/search")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("query", " 닭가슴살   하림 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].foodCode").value("1234567890123456789"))
                .andExpect(jsonPath("$[0].foodName").value("닭가슴살"))
                .andExpect(jsonPath("$[0].servingSize").value(100))
                .andExpect(jsonPath("$[0].servingUnit").value("g"))
                .andExpect(jsonPath("$[0].calories").value(120));

        verify(mealMapper).searchFoods("닭가슴살 하림", tokens);
    }

    @Test
    void searchFoodsReturnsEmptyForBlankQuery() throws Exception {
        when(jwtTokenProvider.getUserId(TOKEN)).thenReturn(1L);

        mockMvc.perform(get("/api/foods/search")
                        .header("Authorization", "Bearer " + TOKEN)
                        .param("query", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(mealMapper, never()).searchFoods(eq(""), eq(List.of()));
    }

    private FoodCandidateRow food() {
        FoodCandidateRow row = new FoodCandidateRow();
        row.setFoodCode("1234567890123456789");
        row.setFoodName("닭가슴살");
        row.setManufacturer("브랜드");
        row.setServingSize(new BigDecimal("100"));
        row.setServingUnit("g");
        row.setCalories(new BigDecimal("120"));
        row.setCarbohydrate(BigDecimal.ZERO);
        row.setProtein(new BigDecimal("23"));
        row.setFat(new BigDecimal("2"));
        return row;
    }
}
