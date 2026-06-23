package com.lingdong.payroll.domain.unit;

import com.lingdong.payroll.domain.unit.dto.PayingUnitUpsertRequest;
import com.lingdong.payroll.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PayingUnitServiceTest {

    @Autowired
    private PayingUnitService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void upsertByBankAccountRestoresDeletedUnitInsteadOfInsertingDuplicate() {
        String bankAccount = "UNIT_DELETED_ACCOUNT";
        jdbcTemplate.update(
                "insert into paying_unit(bank_account, account_name, bank_name, created_by, created_at, updated_at, enabled, deleted) " +
                        "values (?, '旧单位', '旧行名', 1, now(), now(), 1, 1)",
                bankAccount
        );

        PayingUnit unit = service.upsertByBankAccount(new PayingUnitUpsertRequest(
                bankAccount,
                "新单位",
                "新行名",
                "中国建设银行",
                null,
                null
        ), new CurrentUser(1L, "admin", "系统管理员", Set.of("ADMIN"), Set.of()));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from paying_unit where created_by = 1 and bank_account = ?",
                Integer.class,
                bankAccount
        );

        assertThat(count).isEqualTo(1);
        assertThat(unit.getAccountName()).isEqualTo("新单位");
        assertThat(unit.getDeleted()).isFalse();
        assertThat(unit.getEnabled()).isTrue();
    }
}
