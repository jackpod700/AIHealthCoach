package com.aihealthcoach.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.admin.dto.AdminDto.AdminDashboardResponse;
import com.aihealthcoach.admin.dto.AdminDto.AdminDashboardHistoryResponse;
import com.aihealthcoach.admin.service.AdminDashboardService;
import com.aihealthcoach.admin.service.AdminMetricsHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminMetricsHistoryService adminMetricsHistoryService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminDashboardService.getDashboard());
    }

    @GetMapping("/dashboard/history")
    public ResponseEntity<AdminDashboardHistoryResponse> getDashboardHistory(
            @RequestParam(value = "rangeMinutes", required = false, defaultValue = "60") Integer rangeMinutes
    ) {
        return ResponseEntity.ok(adminMetricsHistoryService.history(rangeMinutes));
    }
}
