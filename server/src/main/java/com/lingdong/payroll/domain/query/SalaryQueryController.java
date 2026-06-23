package com.lingdong.payroll.domain.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.domain.query.dto.PublicSalaryQueryRequest;
import com.lingdong.payroll.domain.query.dto.SalaryQueryRecord;
import com.lingdong.payroll.security.PermissionGuard;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salary-query")
public class SalaryQueryController {

    private final SalaryQueryService service;
    private final PermissionGuard permissionGuard;

    public SalaryQueryController(SalaryQueryService service, PermissionGuard permissionGuard) {
        this.service = service;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping("/public")
    public ApiResponse<Page<SalaryQueryRecord>> publicQuery(@Valid @RequestBody PublicSalaryQueryRequest request) {
        return ApiResponse.ok(service.publicQuery(request.name(), request.idCardNo()));
    }

    @GetMapping("/admin")
    public ApiResponse<Page<SalaryQueryRecord>> adminQuery(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        permissionGuard.require("salary:view");
        return ApiResponse.ok(service.adminQuery(name, page, size));
    }
}
