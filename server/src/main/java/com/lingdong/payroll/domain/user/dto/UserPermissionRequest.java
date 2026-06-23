package com.lingdong.payroll.domain.user.dto;

import java.util.Set;

public record UserPermissionRequest(Set<String> permissionCodes) {
}
