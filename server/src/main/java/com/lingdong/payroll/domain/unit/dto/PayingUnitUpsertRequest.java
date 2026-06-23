package com.lingdong.payroll.domain.unit.dto;

import jakarta.validation.constraints.NotBlank;

public record PayingUnitUpsertRequest(
        @NotBlank String bankAccount,
        @NotBlank String accountName,
        String bankName,
        String bankType,
        String bankCategory,
        String cnapsNo
) {
}
