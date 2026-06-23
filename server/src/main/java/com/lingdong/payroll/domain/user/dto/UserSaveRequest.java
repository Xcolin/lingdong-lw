package com.lingdong.payroll.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserSaveRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        @Size(min = 6, message = "密码至少6位") String password,
        Boolean enabled,
        Set<String> permissionCodes
) {
}
