package com.lingdong.payroll.security;

import com.lingdong.payroll.common.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class PermissionGuard {

    private final CurrentUserProvider currentUserProvider;

    public PermissionGuard(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public CurrentUser requireAdmin() {
        CurrentUser currentUser = currentUserProvider.get();
        if (!currentUser.isAdmin()) {
            throw new BusinessException("无权操作");
        }
        return currentUser;
    }

    public CurrentUser require(String permission) {
        CurrentUser currentUser = currentUserProvider.get();
        if (!currentUser.hasPermission(permission)) {
            throw new BusinessException("无权操作");
        }
        return currentUser;
    }

    public CurrentUser requireAny(String... permissions) {
        CurrentUser currentUser = currentUserProvider.get();
        for (String permission : permissions) {
            if (currentUser.hasPermission(permission)) {
                return currentUser;
            }
        }
        throw new BusinessException("无权操作");
    }
}
