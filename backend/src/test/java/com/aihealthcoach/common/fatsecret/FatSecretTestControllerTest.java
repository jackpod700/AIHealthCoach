package com.aihealthcoach.common.fatsecret;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretFoodItemResponse;
import com.aihealthcoach.common.fatsecret.FatSecretDto.FatSecretSearchTestResponse;
import com.aihealthcoach.user.oauth.OAuth2LoginFailureHandler;
import com.aihealthcoach.user.oauth.OAuth2LoginSuccessHandler;

@WebMvcTest(FatSecretTestController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class FatSecretTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FatSecretTestService fatSecretTestService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

    @Test
    void searchIsPublicAndReturnsFatSecretConnectionResult() throws Exception {
        when(fatSecretTestService.search(eq("apple"), eq(5), eq(null), eq(null)))
                .thenReturn(new FatSecretSearchTestResponse(
                        true,
                        "apple",
                        1,
                        0,
                        5,
                        List.of(new FatSecretFoodItemResponse(
                                "123",
                                "Apple",
                                "Generic",
                                null,
                                "https://example.test/apple",
                                "Per 100g - Calories: 52kcal | Fat: 0.17g | Carbs: 13.81g | Protein: 0.26g"
                        ))
                ));

        mockMvc.perform(get("/api/fatsecret/test/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.connected", is(true)))
                .andExpect(jsonPath("$.data.query", is("apple")))
                .andExpect(jsonPath("$.data.items[0].foodId", is("123")))
                .andExpect(jsonPath("$.data.items[0].foodDescription", containsString("Calories")));
    }

    @Test
    void searchFailureDoesNotExposeSecretOrToken() throws Exception {
        when(fatSecretTestService.search(eq("apple"), eq(5), eq(null), eq(null)))
                .thenThrow(FatSecretException.responseInvalid(
                        "FatSecret error code=21, message=Premiere access required"
                ));

        mockMvc.perform(get("/api/fatsecret/test/search"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FATSECRET_RESPONSE_INVALID")))
                .andExpect(jsonPath("$.error.message", containsString("FatSecret error code=21")))
                .andExpect(jsonPath("$.error.message", containsString("Premiere access required")))
                .andExpect(jsonPath("$.error.message", not(containsString("client-secret"))))
                .andExpect(jsonPath("$.error.message", not(containsString("access-token"))));
    }
}
