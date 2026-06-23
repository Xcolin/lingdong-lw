package com.lingdong.payroll.domain.person.dto;

import jakarta.validation.constraints.NotBlank;

public record PayeePersonUpsertRequest(
        @NotBlank String name,
        @NotBlank String idCardNo,
        String phone,
        @NotBlank String bankAccount,
        @NotBlank String accountName,
        String bankName,
        String bankType,
        String bankCategory,
        String cnapsNo
) {
}
