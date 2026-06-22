package com.aihealthcoach.admin.filter;

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.aihealthcoach.admin.service.ActiveUserMetricsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActiveUserMetricsFilter extends OncePerRequestFilter {

    private final ObjectProvider<ActiveUserMetricsService> activeUserMetricsServiceProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/")) {
                ActiveUserMetricsService activeUserMetricsService = activeUserMetricsServiceProvider.getIfAvailable();
                if (activeUserMetricsService != null) {
                    activeUserMetricsService.record(currentUserId());
                }
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
