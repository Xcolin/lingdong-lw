package com.lingdong.payroll.domain.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.BusinessException;
import com.lingdong.payroll.domain.batch.PayrollBatch;
import com.lingdong.payroll.domain.batch.PayrollBatchItem;
import com.lingdong.payroll.domain.batch.PayrollBatchService;
import com.lingdong.payroll.domain.log.OperationLogService;
import com.lingdong.payroll.domain.unit.PayingUnit;
import com.lingdong.payroll.domain.unit.PayingUnitService;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.DataScope;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PayrollExportService {

    private final PayrollBatchService batchService;
    private final PayingUnitService unitService;
    private final ExportRecordMapper exportRecordMapper;
    private final OperationLogService operationLogService;
    private final Path exportDir;

    public PayrollExportService(
            PayrollBatchService batchService,
            PayingUnitService unitService,
            ExportRecordMapper exportRecordMapper,
            OperationLogService operationLogService,
            @Value("${payroll.export-dir}") String exportDir
    ) {
        this.batchService = batchService;
        this.unitService = unitService;
        this.exportRecordMapper = exportRecordMapper;
        this.operationLogService = operationLogService;
        this.exportDir = Path.of(exportDir);
    }

    @Transactional
    public ExportRecord export(Long batchId, Long payingUnitId, BankTemplateType templateType, CurrentUser currentUser) {
        PayrollBatch batch = batchService.getVisible(batchId, currentUser);
        PayingUnit unit = unitService.getVisible(payingUnitId, currentUser);
        List<PayrollBatchItem> items = batchService.listItems(batchId, currentUser);
        validate(items, templateType);
        try {
            Files.createDirectories(exportDir);
            String fileName = batch.getBatchName() + "-" + unit.getAccountName() + "-" + templateType.displayName() + "-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + templateType.extension();
            Path target = exportDir.resolve(fileName);
            writeWorkbook(templateType, items, target);
            ExportRecord record = new ExportRecord();
            record.setBatchId(batchId);
            record.setPayingUnitId(payingUnitId);
            record.setTemplateType(templateType.name());
            record.setFileName(fileName);
            record.setFilePath(target.toAbsolutePath().toString());
            record.setTotalPeople(items.size());
            record.setTotalAmount(items.stream().map(PayrollBatchItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            record.setCreatedBy(currentUser.id());
            exportRecordMapper.insert(record);
            operationLogService.record(currentUser, "EXPORT", "PAYROLL_EXPORT", record.getId(), null, record);
            return record;
        } catch (IOException exception) {
            throw new BusinessException("导出文件失败：" + exception.getMessage());
        }
    }

    public Page<ExportRecord> list(long page, long size, DataScope scope) {
        LambdaQueryWrapper<ExportRecord> query = new LambdaQueryWrapper<ExportRecord>()
                .eq(!scope.unrestricted(), ExportRecord::getCreatedBy, scope.createdBy())
                .orderByDesc(ExportRecord::getCreatedAt);
        return exportRecordMapper.selectPage(Page.of(page, size), query);
    }

    public ExportRecord getVisible(Long id, CurrentUser currentUser) {
        ExportRecord record = exportRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("导出记录不存在");
        }
        if (!currentUser.isAdmin() && !record.getCreatedBy().equals(currentUser.id())) {
            throw new BusinessException("无权下载该导出文件");
        }
        return record;
    }

    private void writeWorkbook(BankTemplateType templateType, List<PayrollBatchItem> items, Path target) throws IOException {
        try (InputStream input = new ClassPathResource(templateType.templatePath()).getInputStream();
             Workbook workbook = templateType == BankTemplateType.BOC ? new HSSFWorkbook(input) : new XSSFWorkbook(input);
             OutputStream output = Files.newOutputStream(target)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 0; i < items.size(); i++) {
                PayrollBatchItem item = items.get(i);
                Row row = sheet.getRow(i + 1);
                if (row == null) {
                    row = sheet.createRow(i + 1);
                }
                if (templateType == BankTemplateType.CCB) {
                    writeCcb(row, i + 1, item);
                } else {
                    writeBoc(row, item);
                }
            }
            workbook.write(output);
        }
    }

    private void writeCcb(Row row, int sequence, PayrollBatchItem item) {
        row.createCell(0).setCellValue(sequence);
        row.createCell(1).setCellValue(item.getBankAccount());
        row.createCell(2).setCellValue(item.getAccountName());
        row.createCell(3).setCellValue(item.getAmount().doubleValue());
        row.createCell(4).setCellValue(isCcb(item) ? 0 : 1);
        row.createCell(5).setCellValue(value(item.getBankName()));
        row.createCell(6).setCellValue(value(item.getCnapsNo()));
        row.createCell(7).setCellValue(value(item.getSummary()));
        row.createCell(8).setCellValue(value(item.getRemark()));
    }

    private void writeBoc(Row row, PayrollBatchItem item) {
        row.createCell(0).setCellValue(item.getBankAccount());
        row.createCell(1).setCellValue(item.getAccountName());
        row.createCell(2).setCellValue(item.getAmount().doubleValue());
        row.createCell(3).setCellValue(isBank(item, "中国银行") || isBank(item, "中行") ? "中行" : "他行");
        row.createCell(4).setCellValue(value(item.getCnapsNo()));
        row.createCell(5).setCellValue(isUnit(item) ? "" : "居民身份证");
        row.createCell(6).setCellValue(isUnit(item) ? "" : value(item.getIdCardNo()));
        row.createCell(7).setCellValue(value(item.getSummary()));
        row.createCell(8).setCellValue(value(item.getRemark()));
    }

    private void validate(List<PayrollBatchItem> items, BankTemplateType templateType) {
        if (items.isEmpty()) {
            throw new BusinessException("工资批次没有明细");
        }
        for (PayrollBatchItem item : items) {
            if (blank(item.getBankAccount()) || blank(item.getAccountName()) || blank(item.getBankName())) {
                throw new BusinessException("第 " + item.getRowNo() + " 行缺少银行账号、户名或行名");
            }
            if (!isUnit(item) && blank(item.getIdCardNo())) {
                throw new BusinessException("第 " + item.getRowNo() + " 行人员明细缺少身份证号");
            }
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("第 " + item.getRowNo() + " 行金额必须大于 0");
            }
        }
    }

    private boolean isBank(PayrollBatchItem item, String bankName) {
        return contains(item.getBankType(), bankName) || contains(item.getBankName(), bankName);
    }

    private boolean isCcb(PayrollBatchItem item) {
        if ("中国建设银行".equals(item.getBankType())) {
            return true;
        }
        return blank(item.getBankType()) && contains(item.getBankName(), "建设");
    }

    private boolean isUnit(PayrollBatchItem item) {
        return "UNIT".equalsIgnoreCase(item.getTargetType());
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
