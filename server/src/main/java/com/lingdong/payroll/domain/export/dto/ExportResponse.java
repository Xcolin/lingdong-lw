package com.lingdong.payroll.domain.export.dto;

import com.lingdong.payroll.domain.export.ExportRecord;

import java.math.BigDecimal;

public record ExportResponse(
        Long id,
        String fileName,
        Integer totalPeople,
        BigDecimal totalAmount,
        String downloadUrl
) {
    public static ExportResponse from(ExportRecord record) {
        return new ExportResponse(
                record.getId(),
                record.getFileName(),
                record.getTotalPeople(),
                record.getTotalAmount(),
                "/api/exports/" + record.getId() + "/download"
        );
    }
}
