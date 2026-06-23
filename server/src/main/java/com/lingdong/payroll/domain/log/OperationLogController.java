package com.lingdong.payroll.domain.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.ApiResponse;
import com.lingdong.payroll.security.CurrentUserProvider;
import com.lingdong.payroll.security.DataScope;
import com.lingdong.payroll.security.PermissionGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final PermissionGuard permissionGuard;

    public OperationLogController(OperationLogMapper mapper, CurrentUserProvider currentUserProvider, PermissionGuard permissionGuard) {
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiResponse<Page<OperationLog>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        permissionGuard.require("log:view");
        DataScope scope = currentUserProvider.dataScope();
        LambdaQueryWrapper<OperationLog> query = new LambdaQueryWrapper<OperationLog>()
                .eq(module != null && !module.isBlank(), OperationLog::getModule, module)
                .eq(action != null && !action.isBlank(), OperationLog::getAction, action)
                .eq(!scope.unrestricted(), OperationLog::getOperatorId, scope.createdBy())
                .orderByDesc(OperationLog::getOperatedAt);
        return ApiResponse.ok(mapper.selectPage(Page.of(page, size), query));
    }
}
