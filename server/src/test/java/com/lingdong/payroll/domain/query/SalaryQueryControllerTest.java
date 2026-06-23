package com.lingdong.payroll.domain.query;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalaryQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicQueryReturnsOwnSalaryDetailsWithoutLogin() throws Exception {
        insertBatchItem("未发工资", "张三", "510101199001010011", "622700000", "工资", "未发", false);
        insertBatchItem("2026年06月工资", "张三", "510101199001010011", "622700001", "工资", "6月工资", true);
        insertAdvance("张三", "510101199001010011", "2026-06-19 10:00:00", "微信", "生活费", "提前借支");

        String body = objectMapper.writeValueAsString(new PublicQueryBody("张三", "510101199001010011"));

        String response = mockMvc.perform(post("/api/salary-query/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode records = objectMapper.readTree(response).get("data").get("records");

        assertThat(records).hasSize(2);
        assertThat(records.get(0).get("sourceType").asText()).isEqualTo("ADVANCE");
        assertThat(records.get(0).get("batchName").asText()).isEqualTo("平时预支");
        assertThat(records.get(0).get("summary").asText()).isEqualTo("微信");
        assertThat(records.get(0).get("remark").asText()).contains("生活费");
        assertThat(records.get(0).get("remark").asText()).contains("提前借支");
        assertThat(records.get(1).get("sourceType").asText()).isEqualTo("PAYROLL");
        assertThat(records.get(1).get("batchName").asText()).isEqualTo("2026年06月工资");
        assertThat(records.get(1).get("name").asText()).isEqualTo("张三");
        assertThat(records.get(1).get("idCardNo").asText()).isEqualTo("510101199001010011");
        assertThat(records.get(1).get("summary").asText()).isEqualTo("工资");
        assertThat(records.get(1).get("batchCreatedAt").asText()).contains("2026-06-18");
    }

    @Test
    void adminQuerySupportsFuzzyNameSearchForAllSalaryDetails() throws Exception {
        insertBatchItem("奖金批次", "李四", "510101199002020022", "622700002", "奖金", "项目奖金", true);
        String token = loginAdminToken();

        String response = mockMvc.perform(get("/api/salary-query/admin")
                        .header("Authorization", "Bearer " + token)
                        .param("name", "李")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(response).get("data");

        assertThat(data.get("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(data.get("records").get(0).get("name").asText()).contains("李");
    }

    private void insertBatchItem(String batchName, String name, String idCardNo, String bankAccount, String summary, String remark, boolean actualPaid) {
        jdbcTemplate.update(
                "insert into payroll_batch(batch_name, pay_date, default_summary, remark, total_people, total_amount, created_by, created_at, updated_at, deleted, actual_paid, actual_paid_at, actual_paid_by) " +
                        "values (?, '2026-06-18', ?, ?, 1, 1234.56, 1, '2026-06-18 08:30:00', '2026-06-18 08:30:00', 0, ?, ?, ?)",
                batchName,
                summary,
                remark,
                actualPaid ? 1 : 0,
                actualPaid ? "2026-06-18 09:30:00" : null,
                actualPaid ? 1 : null
        );
        Long batchId = jdbcTemplate.queryForObject("select max(id) from payroll_batch", Long.class);
        jdbcTemplate.update(
                "insert into payroll_batch_item(batch_id, target_type, row_no, name, id_card_no, phone, bank_account, account_name, bank_name, bank_type, amount, summary, remark) " +
                        "values (?, 'PERSON', 1, ?, ?, '13800000000', ?, ?, '中国建设银行成都支行', '中国建设银行', 1234.56, ?, ?)",
                batchId,
                name,
                idCardNo,
                bankAccount,
                name,
                summary,
                remark
        );
    }

    private void insertAdvance(String name, String idCardNo, String advanceTime, String method, String reason, String remark) {
        jdbcTemplate.update(
                "insert into advance_payment(name, id_card_no, phone, amount, advance_time, advance_method, reason, remark, created_by, created_at, updated_at, deleted) " +
                        "values (?, ?, '13800000000', 200.00, ?, ?, ?, ?, 1, ?, ?, 0)",
                name,
                idCardNo,
                advanceTime,
                method,
                reason,
                remark,
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

    private record PublicQueryBody(String name, String idCardNo) {
    }

    private record LoginBody(String username, String password) {
    }
}
