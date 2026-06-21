package com.aihealthcoach.admin.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aihealthcoach.admin.dto.AdminDto.AdminDashboardResponse;
import com.aihealthcoach.admin.dto.AdminDto.UserMetricsResponse;
import com.aihealthcoach.admin.service.AdminDashboardService;
import com.aihealthcoach.common.auth.JwtAccessDeniedHandler;
import com.aihealthcoach.common.auth.JwtAuthenticationEntryPoint;
import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.common.config.SecurityConfig;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenRedisRepository tokenRedisRepository;

    @Test
    void dashboardRejectsUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardAllowsAdminRole() throws Exception {
        when(adminDashboardService.getDashboard()).thenReturn(dashboard());

        mockMvc.perform(get("/api/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.users.totalUsers").value(10));
    }

    private AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                new UserMetricsResponse(10L, 1L, 2L)
        );
    }
}
