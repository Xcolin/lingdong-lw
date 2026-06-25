package com.lingdong.payroll.domain.batch;

import com.lingdong.payroll.common.BusinessException;
import com.lingdong.payroll.domain.batch.dto.PayrollBatchSaveRequest;
import com.lingdong.payroll.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PayrollBatchServiceTest {

    @Autowired
    private PayrollBatchService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final CurrentUser admin = new CurrentUser(1L, "admin", "系统管理员", Set.of("ADMIN"), Set.of());

    @Test
    void saveRejectsActualPaidBatch() {
        Long batchId = insertActualPaidBatch("已发放更新锁定批次");

        assertThatThrownBy(() -> service.save(batchId, new PayrollBatchSaveRequest(
                "尝试修改",
                LocalDate.now(),
                "工资",
                "不能修改",
                List.of()
        ), admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实际已发");
    }

    @Test
    void deleteRejectsActualPaidBatch() {
        Long batchId = insertActualPaidBatch("已发放删除锁定批次");

        assertThatThrownBy(() -> service.delete(batchId, admin))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实际已发");
    }

    private Long insertActualPaidBatch(String batchName) {
        jdbcTemplate.update(
                "insert into payroll_batch(batch_name, pay_date, default_summary, total_people, total_amount, created_by, created_at, updated_at, actual_paid, actual_paid_at, actual_paid_by, deleted) " +
                        "values (?, '2025-01-01', '工资', 1, 100.00, 1, '2025-01-01 10:00:00', '2025-01-01 10:00:00', 1, '2025-01-01 10:00:00', 1, 0)",
                batchName
        );
        return jdbcTemplate.queryForObject("select max(id) from payroll_batch where batch_name = ?", Long.class, batchName);
    }
}
