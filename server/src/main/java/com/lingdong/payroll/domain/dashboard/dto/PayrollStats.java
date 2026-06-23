package com.lingdong.payroll.domain.dashboard.dto;

import java.math.BigDecimal;

public record PayrollStats(
        long batchCount,
        long itemCount,
        BigDecimal totalAmount,
        long exportCount,
        long advanceCount,
        BigDecimal advanceAmount
) {
}
