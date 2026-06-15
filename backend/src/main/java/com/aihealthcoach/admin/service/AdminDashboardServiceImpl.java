package com.aihealthcoach.admin.service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;

import org.springframework.stereotype.Service;

import com.aihealthcoach.admin.dto.AiUsageSummary;
import com.aihealthcoach.admin.dto.AdminDto.AdminDashboardResponse;
import com.aihealthcoach.admin.dto.AdminDto.AiMetricsResponse;
import com.aihealthcoach.admin.dto.AdminDto.ServerMetricsResponse;
import com.aihealthcoach.admin.dto.AdminDto.UserMetricsResponse;
import com.aihealthcoach.admin.mapper.AdminMapper;
import com.sun.management.OperatingSystemMXBean;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final AdminMapper adminMapper;
    private final ApiTrafficMetricsService apiTrafficMetricsService;

    @Override
    public AdminDashboardResponse getDashboard() {
        AiUsageSummary aiUsageSummary = adminMapper.summarizeAiUsageToday();
        if (aiUsageSummary == null) {
            aiUsageSummary = AiUsageSummary.empty();
        }

        return new AdminDashboardResponse(
                serverMetrics(),
                apiTrafficMetricsService.snapshot(),
                aiMetrics(aiUsageSummary),
                userMetrics()
        );
    }

    private ServerMetricsResponse serverMetrics() {
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();

        long jvmUsedMb = memoryMxBean.getHeapMemoryUsage().getUsed() / BYTES_PER_MB;
        long jvmMaxMb = memoryMxBean.getHeapMemoryUsage().getMax() / BYTES_PER_MB;
        long uptimeSeconds = runtimeMxBean.getUptime() / 1000L;

        java.lang.management.OperatingSystemMXBean baseOsBean = ManagementFactory.getOperatingSystemMXBean();
        if (baseOsBean instanceof OperatingSystemMXBean osBean) {
            long totalMemoryMb = osBean.getTotalMemorySize() / BYTES_PER_MB;
            long freeMemoryMb = osBean.getFreeMemorySize() / BYTES_PER_MB;
            double cpuUsagePercent = Math.max(0, osBean.getCpuLoad() * 100);

            return new ServerMetricsResponse(
                    cpuUsagePercent,
                    totalMemoryMb - freeMemoryMb,
                    totalMemoryMb,
                    jvmUsedMb,
                    jvmMaxMb,
                    uptimeSeconds
            );
        }

        return new ServerMetricsResponse(null, null, null, jvmUsedMb, jvmMaxMb, uptimeSeconds);
    }

    private AiMetricsResponse aiMetrics(AiUsageSummary summary) {
        return new AiMetricsResponse(
                valueOrZero(summary.getRequestCountToday()),
                valueOrZero(summary.getSuccessCountToday()),
                valueOrZero(summary.getFailureCountToday()),
                summary.getAverageLatencyMsToday() == null ? 0.0 : summary.getAverageLatencyMsToday(),
                valueOrZero(summary.getInputTokensToday()),
                valueOrZero(summary.getOutputTokensToday()),
                valueOrZero(summary.getTotalTokensToday())
        );
    }

    private UserMetricsResponse userMetrics() {
        return new UserMetricsResponse(
                adminMapper.countUsers(),
                adminMapper.countTodaySignups(),
                apiTrafficMetricsService.activeUsers5m()
        );
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
