package com.aihealthcoach.admin.dto;

public class AdminDto {

    public record AdminDashboardResponse(
            UserMetricsResponse users
    ) {
    }

    public record UserMetricsResponse(
            Long totalUsers,
            Long todaySignups,
            Long activeUsers5m
    ) {
    }
}
