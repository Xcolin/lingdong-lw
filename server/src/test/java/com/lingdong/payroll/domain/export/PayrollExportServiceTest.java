package com.lingdong.payroll.domain.export;

import com.lingdong.payroll.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PayrollExportServiceTest {

    @Autowired
    private PayrollExportService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    private Path exportDir;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "系统管理员", Set.of("ADMIN"), Set.of());

    @Test
    void exportUsesRequestedDirectoryAndBatchTimestampFileNameWithoutUnitName() {
        Long batchId = insertBatch("六月工资");
        Long unitId = insertUnit("四川辰一思翰劳务有限公司");
        insertBatchItem(batchId);

        ExportRecord record = service.export(batchId, unitId, BankTemplateType.CCB, exportDir.toString(), admin);

        assertThat(record.getFilePath()).startsWith(exportDir.toAbsolutePath().toString());
        assertThat(Files.exists(Path.of(record.getFilePath()))).isTrue();
        assertThat(record.getFileName()).startsWith("六月工资-");
        assertThat(record.getFileName()).endsWith(".xlsx");
        assertThat(record.getFileName()).doesNotContain("四川辰一思翰劳务有限公司");
        assertThat(record.getFileName()).matches("六月工资-\\d{17}\\.xlsx");
    }

    private Long insertBatch(String batchName) {
        jdbcTemplate.update(
                "insert into payroll_batch(batch_name, pay_date, default_summary, total_people, total_amount, created_by, deleted) " +
                        "values (?, current_date, '工资', 1, 100.00, 1, 0)",
                batchName
        );
        return jdbcTemplate.queryForObject("select max(id) from payroll_batch where batch_name = ?", Long.class, batchName);
    }

    private Long insertUnit(String accountName) {
        jdbcTemplate.update(
                "insert into paying_unit(bank_account, account_name, bank_name, bank_type, created_by, enabled, deleted) " +
                        "values ('6227000000000000000', ?, '中国建设银行成都分行', '中国建设银行', 1, 1, 0)",
                accountName
        );
        return jdbcTemplate.queryForObject("select max(id) from paying_unit where account_name = ?", Long.class, accountName);
    }

    private void insertBatchItem(Long batchId) {
        jdbcTemplate.update(
                "insert into payroll_batch_item(batch_id, person_id, target_type, row_no, name, id_card_no, phone, bank_account, account_name, bank_name, bank_type, amount, summary) " +
                        "values (?, null, 'PERSON', 1, '张三', '510101199001011234', '13800000000', '6227000000000000001', '张三', '中国建设银行成都分行', '中国建设银行', ?, '工资')",
                batchId,
                new BigDecimal("100.00")
        );
    }
}
