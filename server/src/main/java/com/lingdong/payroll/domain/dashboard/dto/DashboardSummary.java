package com.lingdong.payroll.domain.dashboard.dto;

import java.time.LocalDate;

public record DashboardSummary(
        LocalDate startDate,
        LocalDate endDate,
        PayrollStats payroll,
        LibraryStats person,
        LibraryStats unit
) {
}
