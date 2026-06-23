package com.lingdong.payroll.domain.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.domain.user.dto.ResetPasswordRequest;
import com.lingdong.payroll.domain.user.dto.UserPermissionRequest;
import com.lingdong.payroll.domain.user.dto.UserSaveRequest;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.PermissionGuard;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

    private final UserAccountService service;
    private final PermissionGuard permissionGuard;

    public UserAdminController(UserAccountService service, PermissionGuard permissionGuard) {
        this.service = service;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<Page<UserSummary>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        permissionGuard.requireAdmin();
        return ApiResponse.ok(service.list(keyword, enabled, page, size));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionSummary>> permissions() {
        permissionGuard.requireAdmin();
        return ApiResponse.ok(service.permissions());
    }

    @PostMapping
    public ApiResponse<UserSummary> create(@Valid @RequestBody UserSaveRequest request) {
        CurrentUser operator = permissionGuard.requireAdmin();
        return ApiResponse.ok(service.create(request.username(), request.displayName(), request.password(), request.enabled(), request.permissionCodes(), operator));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserSummary> update(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        CurrentUser operator = permissionGuard.requireAdmin();
        return ApiResponse.ok(service.update(id, request.displayName(), request.enabled(), request.permissionCodes(), operator));
    }

    @PutMapping("/{id}/enabled")
    public ApiResponse<Void> updateEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        service.updateEnabled(id, enabled, permissionGuard.requireAdmin());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(id, request.password(), permissionGuard.requireAdmin());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<Void> updatePermissions(@PathVariable Long id, @RequestBody UserPermissionRequest request) {
        service.updatePermissions(id, request.permissionCodes(), permissionGuard.requireAdmin());
        return ApiResponse.ok();
    }
}
