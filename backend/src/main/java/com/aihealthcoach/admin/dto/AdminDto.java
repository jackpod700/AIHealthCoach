package com.aihealthcoach.admin.dto;

public class AdminDto {

    public record AdminDashboardResponse(
            ServerMetricsResponse server,
            TrafficMetricsResponse traffic,
            AiMetricsResponse ai,
            UserMetricsResponse users
    ) {
    }

    public record ServerMetricsResponse(
            Double cpuUsagePercent,
            Long systemMemoryUsedMb,
            Long systemMemoryTotalMb,
            Long jvmMemoryUsedMb,
            Long jvmMemoryMaxMb,
            Long uptimeSeconds
    ) {
    }

    public record TrafficMetricsResponse(
            Long requestCount1m,
            Long requestCount5m,
            Long requestCount1h,
            Double averageResponseMs5m,
            Long clientErrorCount5m,
            Long serverErrorCount5m
    ) {
    }

    public record AiMetricsResponse(
            Long requestCountToday,
            Long successCountToday,
            Long failureCountToday,
            Double averageLatencyMsToday,
            Long inputTokensToday,
            Long outputTokensToday,
            Long totalTokensToday
    ) {
    }

    public record UserMetricsResponse(
            Long totalUsers,
            Long todaySignups,
            Long activeUsers5m
    ) {
    }
}
