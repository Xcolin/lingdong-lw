package com.lingdong.payroll.domain.person.dto;

import com.lingdong.payroll.domain.person.PayeePerson;

public record PayeePersonResponse(
        Long id,
        String name,
        String idCardNo,
        String phone,
        String bankAccount,
        String accountName,
        String bankName,
        String bankType,
        String bankCategory,
        String cnapsNo,
        Long createdBy
) {
    public static PayeePersonResponse from(PayeePerson person) {
        return new PayeePersonResponse(
                person.getId(),
                person.getName(),
                person.getIdCardNo(),
                person.getPhone(),
                person.getBankAccount(),
                person.getAccountName(),
                person.getBankName(),
                person.getBankType(),
                person.getBankCategory(),
                person.getCnapsNo(),
                person.getCreatedBy()
        );
    }
}
