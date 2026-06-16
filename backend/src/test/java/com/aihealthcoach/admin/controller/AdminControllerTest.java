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
import com.aihealthcoach.admin.dto.AdminDto.AiMetricsResponse;
import com.aihealthcoach.admin.dto.AdminDto.AdminDashboardHistoryResponse;
import com.aihealthcoach.admin.dto.AdminDto.ServerMetricsResponse;
import com.aihealthcoach.admin.dto.AdminDto.TrafficMetricsResponse;
import com.aihealthcoach.admin.dto.AdminDto.UserMetricsResponse;
import com.aihealthcoach.admin.service.AdminDashboardService;
import com.aihealthcoach.admin.service.AdminMetricsHistoryService;
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
    private AdminMetricsHistoryService adminMetricsHistoryService;

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

    @Test
    void dashboardHistoryRejectsUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/history").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardHistoryAllowsAdminRole() throws Exception {
        when(adminMetricsHistoryService.history(60)).thenReturn(new AdminDashboardHistoryResponse(60, java.util.List.of()));

        mockMvc.perform(get("/api/admin/dashboard/history").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rangeMinutes").value(60));
    }

    private AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                new ServerMetricsResponse(10.0, 100L, 200L, 50L, 100L, 70.0, 3600L),
                new TrafficMetricsResponse(1L, 2L, 3L, 100.0, 0L, 0L),
                new AiMetricsResponse(4L, 3L, 1L, 1500.0, 100L, 50L, 150L),
                new UserMetricsResponse(10L, 1L, 2L)
        );
    }
}
