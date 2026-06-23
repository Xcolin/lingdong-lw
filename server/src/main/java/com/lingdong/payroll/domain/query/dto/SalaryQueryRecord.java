package com.lingdong.payroll.domain.query.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SalaryQueryRecord(
        Long batchId,
        String batchName,
        LocalDate payDate,
        LocalDateTime batchCreatedAt,
        Long itemId,
        String name,
        String idCardNo,
        String phone,
        String bankAccount,
        String accountName,
        String bankName,
        BigDecimal amount,
        String summary,
        String remark,
        String sourceType
) {
}
