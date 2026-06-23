package com.lingdong.payroll.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank
        @Size(min = 6, message = "密码至少6位")
        String password
) {
}
