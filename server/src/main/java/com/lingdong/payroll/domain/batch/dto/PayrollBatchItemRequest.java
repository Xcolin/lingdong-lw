package com.lingdong.payroll.domain.batch.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayrollBatchItemRequest(
        String targetType,
        Long personId,
        Long unitId,
        @NotBlank String name,
        String idCardNo,
        String phone,
        @NotBlank String bankAccount,
        @NotBlank String accountName,
        @NotBlank String bankName,
        String bankType,
        String bankCategory,
        String cnapsNo,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String summary,
        String remark
) {
}
