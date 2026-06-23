package com.lingdong.payroll.domain.person;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.domain.person.dto.PayeePersonResponse;
import com.lingdong.payroll.domain.person.dto.PayeePersonUpsertRequest;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.CurrentUserProvider;
import com.lingdong.payroll.security.DataScope;
import com.lingdong.payroll.security.PermissionGuard;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/persons")
public class PayeePersonController {

    private final PayeePersonService service;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionGuard permissionGuard;

    public PayeePersonController(PayeePersonService service, CurrentUserProvider currentUserProvider, PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<Page<PayeePerson>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return ApiResponse.ok(service.list(keyword, enabled, page, size, new DataScope(true, null)));
    }

    @PostMapping
    public ApiResponse<PayeePersonResponse> save(@Valid @RequestBody PayeePersonUpsertRequest request) {
        permissionGuard.requireAny("person:create", "person:update");
        CurrentUser currentUser = currentUserProvider.get();
        return ApiResponse.ok(PayeePersonResponse.from(service.upsertByIdCard(request, currentUser)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionGuard.require("person:delete");
        service.delete(id, currentUserProvider.get());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/enabled")
    public ApiResponse<Void> updateEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        permissionGuard.require("person:enable");
        service.updateEnabled(id, enabled, currentUserProvider.get());
        return ApiResponse.ok();
    }
}
