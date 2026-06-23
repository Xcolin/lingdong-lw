package com.lingdong.payroll.domain.batch.dto;

import com.lingdong.payroll.domain.batch.PayrollBatch;
import com.lingdong.payroll.domain.batch.PayrollBatchItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PayrollBatchDetailResponse(
        Long id,
        String batchName,
        LocalDate payDate,
        String defaultSummary,
        String remark,
        Integer totalPeople,
        BigDecimal totalAmount,
        Long createdBy,
        List<PayrollBatchItem> items
) {
    public static PayrollBatchDetailResponse from(PayrollBatch batch, List<PayrollBatchItem> items) {
        return new PayrollBatchDetailResponse(
                batch.getId(),
                batch.getBatchName(),
                batch.getPayDate(),
                batch.getDefaultSummary(),
                batch.getRemark(),
                batch.getTotalPeople(),
                batch.getTotalAmount(),
                batch.getCreatedBy(),
                items
        );
    }
}
