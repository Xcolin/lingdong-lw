package com.lingdong.payroll.domain.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void summaryAggregatesPayrollPersonUnitAndExportCountsByCreatedTime() throws Exception {
        Long batchId = insertBatch("2026-06-08 09:00:00", 2, "3000.50", 1, true);
        insertBatchItem(batchId, "统计张三", "510101198801010011", "1000.50");
        insertBatchItem(batchId, "统计李四", "510101198802020022", "2000.00");
        insertBatch("2026-06-10 09:00:00", 1, "999.00", 1, false);
        insertBatch("2026-05-01 09:00:00", 1, "999.00", 1, true);
        insertPerson("王五", "2026-06-03 10:00:00", true, 1);
        insertPerson("赵六", "2026-06-04 10:00:00", false, 1);
        insertUnit("四川某公司", "2026-06-05 10:00:00", true, 1);
        insertExport(batchId, "2026-06-09 10:00:00", 1);
        insertAdvance("统计张三", "2026-06-11 10:00:00", "500.25", 1);
        insertAdvance("统计李四", "2026-05-20 10:00:00", "900.00", 1);
        String token = loginAdminToken();

        String response = mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(response).get("data");

        assertThat(data.get("payroll").get("batchCount").asLong()).isEqualTo(1);
        assertThat(data.get("payroll").get("itemCount").asLong()).isEqualTo(3);
        assertThat(new BigDecimal(data.get("payroll").get("totalAmount").asText())).isEqualByComparingTo("3500.75");
        assertThat(data.get("payroll").get("exportCount").asLong()).isEqualTo(1);
        assertThat(data.get("payroll").get("advanceCount").asLong()).isEqualTo(1);
        assertThat(new BigDecimal(data.get("payroll").get("advanceAmount").asText())).isEqualByComparingTo("500.25");
        assertThat(data.get("person").get("createdCount").asLong()).isEqualTo(2);
        assertThat(data.get("person").get("enabledCount").asLong()).isEqualTo(1);
        assertThat(data.get("person").get("disabledCount").asLong()).isEqualTo(1);
        assertThat(data.get("unit").get("createdCount").asLong()).isEqualTo(1);
        assertThat(data.get("unit").get("enabledCount").asLong()).isEqualTo(1);
    }

    private Long insertBatch(String createdAt, int totalPeople, String totalAmount, long createdBy, boolean actualPaid) {
        jdbcTemplate.update(
                "insert into payroll_batch(batch_name, pay_date, total_people, total_amount, created_by, created_at, updated_at, deleted, actual_paid, actual_paid_at, actual_paid_by) " +
                        "values ('统计批次', '2026-06-18', ?, ?, ?, ?, ?, 0, ?, ?, ?)",
                totalPeople,
                totalAmount,
                createdBy,
                createdAt,
                createdAt,
                actualPaid ? 1 : 0,
                actualPaid ? createdAt : null,
                actualPaid ? createdBy : null
        );
        return jdbcTemplate.queryForObject("select max(id) from payroll_batch", Long.class);
    }

    private void insertBatchItem(Long batchId, String name, String idCardNo, String amount) {
        jdbcTemplate.update(
                "insert into payroll_batch_item(batch_id, target_type, row_no, name, id_card_no, bank_account, account_name, bank_name, amount) " +
                        "values (?, 'PERSON', 1, ?, ?, '622700001', ?, '中国建设银行成都支行', ?)",
                batchId,
                name,
                idCardNo,
                name,
                amount
        );
    }

    private void insertPerson(String name, String createdAt, boolean enabled, long createdBy) {
        jdbcTemplate.update(
                "insert into payee_person(name, id_card_no, bank_account, account_name, created_by, created_at, updated_at, enabled, deleted) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, 0)",
                name,
                "ID" + name,
                "ACCT" + name,
                name,
                createdBy,
                createdAt,
                createdAt,
                enabled ? 1 : 0
        );
    }

    private void insertUnit(String accountName, String createdAt, boolean enabled, long createdBy) {
        jdbcTemplate.update(
                "insert into paying_unit(bank_account, account_name, created_by, created_at, updated_at, enabled, deleted) " +
                        "values (?, ?, ?, ?, ?, ?, 0)",
                "UNIT" + accountName,
                accountName,
                createdBy,
                createdAt,
                createdAt,
                enabled ? 1 : 0
        );
    }

    private void insertExport(Long batchId, String createdAt, long createdBy) {
        jdbcTemplate.update(
                "insert into export_record(batch_id, paying_unit_id, template_type, file_name, file_path, total_people, total_amount, created_by, created_at) " +
                        "values (?, 1, 'CCB', 'test.xlsx', './test.xlsx', 2, 3000.50, ?, ?)",
                batchId,
                createdBy,
                createdAt
        );
    }

    private void insertAdvance(String name, String advanceTime, String amount, long createdBy) {
        jdbcTemplate.update(
                "insert into advance_payment(name, id_card_no, phone, amount, advance_time, advance_method, reason, remark, created_by, created_at, updated_at, deleted) " +
                        "values (?, ?, '13800000000', ?, ?, '现金', '生活费', '统计测试', ?, ?, ?, 0)",
                name,
                "ID" + name,
                amount,
                advanceTime,
                createdBy,
                advanceTime,
                advanceTime
        );
    }

    private String loginAdminToken() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginBody("admin", "admin123"));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).get("data").get("token").asText();
    }

    private record LoginBody(String username, String password) {
    }
}
