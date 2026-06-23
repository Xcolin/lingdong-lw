package com.lingdong.payroll.domain.advance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("advance_payment")
public class AdvancePayment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long personId;
    private String name;
    private String idCardNo;
    private String phone;
    private BigDecimal amount;
    private LocalDateTime advanceTime;
    private String advanceMethod;
    private String reason;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
    @TableField(exist = false)
    private String creatorName;
}
