package com.lingdong.payroll.domain.auth.dto;

import java.util.Set;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String displayName,
        Set<String> roleCodes,
        Set<String> permissions
) {
}
