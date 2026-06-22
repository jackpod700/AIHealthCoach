package com.aihealthcoach.admin.service;

import org.springframework.stereotype.Service;

import com.aihealthcoach.admin.dto.AdminDto.AdminDashboardResponse;
import com.aihealthcoach.admin.dto.AdminDto.UserMetricsResponse;
import com.aihealthcoach.admin.mapper.AdminMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminMapper adminMapper;
    private final ActiveUserMetricsService activeUserMetricsService;

    @Override
    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                userMetrics()
        );
    }

    private UserMetricsResponse userMetrics() {
        return new UserMetricsResponse(
                adminMapper.countUsers(),
                adminMapper.countTodaySignups(),
                activeUserMetricsService.activeUsers5m()
        );
    }
}
