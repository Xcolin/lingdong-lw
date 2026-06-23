package com.lingdong.payroll.domain.advance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdvancePaymentSaveRequest(
        Long personId,
        @NotBlank String name,
        @NotBlank String idCardNo,
        String phone,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDateTime advanceTime,
        @NotBlank String advanceMethod,
        @NotBlank String reason,
        String remark
) {
}
