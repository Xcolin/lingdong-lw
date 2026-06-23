package com.lingdong.payroll.domain.batch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("payroll_batch_item")
public class PayrollBatchItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private String targetType;
    private Long personId;
    private Long unitId;
    private Integer rowNo;
    private String name;
    private String idCardNo;
    private String phone;
    private String bankAccount;
    private String accountName;
    private String bankName;
    private String bankType;
    private String bankCategory;
    private String cnapsNo;
    private BigDecimal amount;
    private String summary;
    private String remark;
}
