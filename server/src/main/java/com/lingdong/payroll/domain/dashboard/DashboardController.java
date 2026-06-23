package com.lingdong.payroll.domain.dashboard;

import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.domain.dashboard.dto.DashboardSummary;
import com.lingdong.payroll.security.CurrentUserProvider;
import com.lingdong.payroll.security.PermissionGuard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final PermissionGuard permissionGuard;
    private final CurrentUserProvider currentUserProvider;

    public DashboardController(DashboardService service, PermissionGuard permissionGuard, CurrentUserProvider currentUserProvider) {
        this.service = service;
        this.permissionGuard = permissionGuard;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        permissionGuard.require("dashboard:view");
        return ApiResponse.ok(service.summary(startDate, endDate, currentUserProvider.dataScope()));
    }
}
