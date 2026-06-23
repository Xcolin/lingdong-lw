package com.lingdong.payroll.domain.batch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record PayrollBatchSaveRequest(
        @NotBlank String batchName,
        LocalDate payDate,
        String defaultSummary,
        String remark,
        @Valid List<PayrollBatchItemRequest> items
) {
}
