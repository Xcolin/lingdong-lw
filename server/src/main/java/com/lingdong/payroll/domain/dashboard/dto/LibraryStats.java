package com.lingdong.payroll.domain.dashboard.dto;

public record LibraryStats(
        long createdCount,
        long enabledCount,
        long disabledCount
) {
}
