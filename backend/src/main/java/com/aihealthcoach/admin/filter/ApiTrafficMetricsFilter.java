package com.aihealthcoach.admin.filter;

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aihealthcoach.admin.service.ApiTrafficMetricsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiTrafficMetricsFilter extends OncePerRequestFilter {

    private final ObjectProvider<ApiTrafficMetricsService> apiTrafficMetricsServiceProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/")) {
                ApiTrafficMetricsService apiTrafficMetricsService = apiTrafficMetricsServiceProvider.getIfAvailable();
                if (apiTrafficMetricsService == null) {
                    return;
                }
                long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
                apiTrafficMetricsService.record(
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMs,
                        currentUserId()
                );
            }
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }
}
