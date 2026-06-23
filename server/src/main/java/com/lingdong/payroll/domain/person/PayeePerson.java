package com.lingdong.payroll.domain.person;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payee_person")
public class PayeePerson {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String idCardNo;
    private String phone;
    private String bankAccount;
    private String accountName;
    private String bankName;
    private String bankType;
    private String bankCategory;
    private String cnapsNo;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean enabled;
    private Boolean deleted;
}
