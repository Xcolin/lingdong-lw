package com.lingdong.payroll.domain.export;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("export_record")
public class ExportRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long payingUnitId;
    private String templateType;
    private String fileName;
    private String filePath;
    private Integer totalPeople;
    private BigDecimal totalAmount;
    private Long createdBy;
    private LocalDateTime createdAt;
}
