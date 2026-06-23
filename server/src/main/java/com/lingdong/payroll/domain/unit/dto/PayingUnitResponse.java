package com.lingdong.payroll.domain.unit.dto;

import com.lingdong.payroll.domain.unit.PayingUnit;

public record PayingUnitResponse(
        Long id,
        String bankAccount,
        String accountName,
        String bankName,
        String bankType,
        String bankCategory,
        String cnapsNo,
        Long createdBy
) {
    public static PayingUnitResponse from(PayingUnit unit) {
        return new PayingUnitResponse(
                unit.getId(),
                unit.getBankAccount(),
                unit.getAccountName(),
                unit.getBankName(),
                unit.getBankType(),
                unit.getBankCategory(),
                unit.getCnapsNo(),
                unit.getCreatedBy()
        );
    }
}
