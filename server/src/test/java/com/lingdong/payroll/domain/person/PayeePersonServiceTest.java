package com.lingdong.payroll.domain.person;

import com.lingdong.payroll.domain.person.dto.PayeePersonUpsertRequest;
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
class PayeePersonServiceTest {

    @Autowired
    private PayeePersonService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void upsertByIdCardRestoresDeletedPersonInsteadOfInsertingDuplicate() {
        String idCardNo = "500235199210073233";
        jdbcTemplate.update(
                "insert into payee_person(name, id_card_no, phone, bank_account, account_name, bank_name, created_by, created_at, updated_at, enabled, deleted) " +
                        "values ('旧姓名', ?, '13000000000', 'OLD_ACCOUNT', '旧姓名', '旧行名', 1, now(), now(), 1, 1)",
                idCardNo
        );

        PayeePerson person = service.upsertByIdCard(new PayeePersonUpsertRequest(
                "新姓名",
                idCardNo,
                "13900000000",
                "NEW_ACCOUNT",
                "新姓名",
                "新行名",
                "中国建设银行",
                null,
                null
        ), new CurrentUser(1L, "admin", "系统管理员", Set.of("ADMIN"), Set.of()));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from payee_person where created_by = 1 and id_card_no = ?",
                Integer.class,
                idCardNo
        );

        assertThat(count).isEqualTo(1);
        assertThat(person.getName()).isEqualTo("新姓名");
        assertThat(person.getBankAccount()).isEqualTo("NEW_ACCOUNT");
        assertThat(person.getDeleted()).isFalse();
        assertThat(person.getEnabled()).isTrue();
    }
}
