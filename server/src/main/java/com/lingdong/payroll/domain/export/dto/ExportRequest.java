package com.lingdong.payroll.domain.export.dto;

import com.lingdong.payroll.domain.export.BankTemplateType;
import jakarta.validation.constraints.NotNull;

public record ExportRequest(
        @NotNull Long batchId,
        @NotNull Long payingUnitId,
        @NotNull BankTemplateType templateType
) {
}
