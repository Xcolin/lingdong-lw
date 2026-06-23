package com.lingdong.payroll.domain.unit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("paying_unit")
public class PayingUnit {
    @TableId(type = IdType.AUTO)
    private Long id;
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
