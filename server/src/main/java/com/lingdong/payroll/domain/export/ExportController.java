package com.lingdong.payroll.domain.export;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.domain.export.dto.ExportRequest;
import com.lingdong.payroll.domain.export.dto.ExportResponse;
import com.lingdong.payroll.security.CurrentUserProvider;
import com.lingdong.payroll.security.PermissionGuard;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final PayrollExportService service;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionGuard permissionGuard;

    public ExportController(PayrollExportService service, CurrentUserProvider currentUserProvider, PermissionGuard permissionGuard) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping
    public ApiResponse<ExportResponse> export(@Valid @RequestBody ExportRequest request) {
        permissionGuard.require("batch:export");
        return ApiResponse.ok(ExportResponse.from(service.export(
                request.batchId(),
                request.payingUnitId(),
                request.templateType(),
                currentUserProvider.get()
        )));
    }

    @GetMapping
    public ApiResponse<Page<ExportRecord>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        permissionGuard.require("export:view");
        return ApiResponse.ok(service.list(page, size, currentUserProvider.dataScope()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id) {
        permissionGuard.require("export:view");
        ExportRecord record = service.getVisible(id, currentUserProvider.get());
        String encodedName = URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(new FileSystemResource(record.getFilePath()));
    }
}
