package com.lingdong.payroll.domain.advance;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.domain.advance.dto.AdvancePaymentSaveRequest;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.CurrentUserProvider;
import com.lingdong.payroll.security.PermissionGuard;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/advances")
public class AdvancePaymentController {

    private final AdvancePaymentService service;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionGuard permissionGuard;

    public AdvancePaymentController(AdvancePaymentService service, CurrentUserProvider currentUserProvider, PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<Page<AdvancePayment>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        permissionGuard.require("advance:view");
        return ApiResponse.ok(service.list(keyword, startDate, endDate, page, size, currentUserProvider.dataScope()));
    }

    @PostMapping
    public ApiResponse<AdvancePayment> create(@Valid @RequestBody AdvancePaymentSaveRequest request) {
        permissionGuard.require("advance:create");
        CurrentUser currentUser = currentUserProvider.get();
        return ApiResponse.ok(service.save(null, request, currentUser));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdvancePayment> update(@PathVariable Long id, @Valid @RequestBody AdvancePaymentSaveRequest request) {
        permissionGuard.require("advance:update");
        return ApiResponse.ok(service.save(id, request, currentUserProvider.get()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionGuard.require("advance:delete");
        service.delete(id, currentUserProvider.get());
        return ApiResponse.ok();
    }
}
