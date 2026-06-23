package com.lingdong.payroll.domain.query.dto;

import jakarta.validation.constraints.NotBlank;

public record PublicSalaryQueryRequest(
        @NotBlank String name,
        @NotBlank String idCardNo
) {
}
