package com.lingdong.payroll.domain.batch;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.domain.batch.dto.PayrollBatchDetailResponse;
import com.lingdong.payroll.domain.batch.dto.PayrollBatchSaveRequest;
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
@RequestMapping("/api/batches")
public class PayrollBatchController {

    private final PayrollBatchService service;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionGuard permissionGuard;

    public PayrollBatchController(PayrollBatchService service, CurrentUserProvider currentUserProvider, PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<Page<PayrollBatch>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        permissionGuard.require("batch:view");
        return ApiResponse.ok(service.list(keyword, startDate, endDate, page, size, currentUserProvider.dataScope()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PayrollBatchDetailResponse> detail(@PathVariable Long id) {
        permissionGuard.require("batch:view");
        CurrentUser currentUser = currentUserProvider.get();
        PayrollBatch batch = service.getVisible(id, currentUser);
        return ApiResponse.ok(PayrollBatchDetailResponse.from(batch, service.listItems(id, currentUser)));
    }

    @PostMapping
    public ApiResponse<PayrollBatch> create(@Valid @RequestBody PayrollBatchSaveRequest request) {
        permissionGuard.require("batch:create");
        return ApiResponse.ok(service.save(null, request, currentUserProvider.get()));
    }

    @PutMapping("/{id}")
    public ApiResponse<PayrollBatch> update(@PathVariable Long id, @Valid @RequestBody PayrollBatchSaveRequest request) {
        permissionGuard.require("batch:update");
        return ApiResponse.ok(service.save(id, request, currentUserProvider.get()));
    }

    @PutMapping("/{id}/actual-paid")
    public ApiResponse<Void> markActualPaid(@PathVariable Long id) {
        permissionGuard.require("batch:update");
        service.markActualPaid(id, currentUserProvider.get());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionGuard.require("batch:delete");
        service.delete(id, currentUserProvider.get());
        return ApiResponse.ok();
    }
}
