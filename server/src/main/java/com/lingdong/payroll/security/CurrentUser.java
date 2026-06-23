package com.lingdong.payroll.security;

import java.util.Set;

public record CurrentUser(Long id, String username, String displayName, Set<String> roleCodes, Set<String> permissions) {

    public CurrentUser(Long id, String username, String displayName, Set<String> roleCodes) {
        this(id, username, displayName, roleCodes, Set.of());
    }

    public boolean isAdmin() {
        return roleCodes != null && roleCodes.contains("ADMIN");
    }

    public boolean hasPermission(String permission) {
        return isAdmin() || permissions != null && permissions.contains(permission);
    }
}
