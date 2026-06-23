package com.lingdong.payroll.domain.user;

import java.time.LocalDateTime;
import java.util.Set;

public record UserSummary(
        Long id,
        String username,
        String displayName,
        Boolean enabled,
        Set<String> roleCodes,
        Set<String> permissions,
        LocalDateTime createdAt
) {
}
