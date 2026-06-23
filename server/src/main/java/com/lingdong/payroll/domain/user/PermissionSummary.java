package com.lingdong.payroll.domain.user;

public record PermissionSummary(
        String code,
        String name,
        String type,
        String menuPath,
        Integer sortNo
) {
}
