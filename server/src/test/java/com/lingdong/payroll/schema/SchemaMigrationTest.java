package com.lingdong.payroll.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SchemaMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesCoreTables() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name in " +
                        "('sys_user', 'payee_person', 'paying_unit', 'payroll_batch', 'export_record', 'operation_log')",
                Integer.class
        );

        assertThat(count).isEqualTo(6);
    }

    @Test
    void migrationAddsEnabledFlagForPersonAndUnitLibraries() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name in ('payee_person', 'paying_unit') and column_name = 'enabled'",
                Integer.class
        );

        assertThat(count).isEqualTo(2);
    }

    @Test
    void migrationCreatesPermissionTablesAndSeedsPermissions() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name in ('sys_permission', 'sys_user_permission')",
                Integer.class
        );
        Integer permissionCount = jdbcTemplate.queryForObject(
                "select count(*) from sys_permission where code in ('dashboard:view', 'batch:view', 'person:view', 'unit:view', 'salary:view', 'user:view')",
                Integer.class
        );

        assertThat(tableCount).isEqualTo(2);
        assertThat(permissionCount).isEqualTo(6);
    }

    @Test
    void migrationAddsActualPaidFieldsForPayrollBatch() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'payroll_batch' and column_name in ('actual_paid', 'actual_paid_at', 'actual_paid_by')",
                Integer.class
        );

        assertThat(count).isEqualTo(3);
    }

    @Test
    void migrationCreatesAdvancePaymentTableAndPermissions() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'advance_payment'",
                Integer.class
        );
        Integer columnCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'advance_payment' and column_name in " +
                        "('person_id', 'name', 'id_card_no', 'phone', 'amount', 'advance_time', 'advance_method', 'reason', 'remark', 'created_by', 'created_at', 'updated_at', 'deleted')",
                Integer.class
        );
        Integer permissionCount = jdbcTemplate.queryForObject(
                "select count(*) from sys_permission where code in ('advance:view', 'advance:create', 'advance:update', 'advance:delete')",
                Integer.class
        );

        assertThat(tableCount).isEqualTo(1);
        assertThat(columnCount).isEqualTo(13);
        assertThat(permissionCount).isEqualTo(4);
    }
}
