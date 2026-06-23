package com.lingdong.payroll.domain.batch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("payroll_batch")
public class PayrollBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchName;
    private LocalDate payDate;
    private String defaultSummary;
    private String remark;
    private Integer totalPeople;
    private BigDecimal totalAmount;
    private Long createdBy;
    private Boolean actualPaid;
    private LocalDateTime actualPaidAt;
    private Long actualPaidBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;

    @TableField(exist = false)
    private String creatorName;
}
