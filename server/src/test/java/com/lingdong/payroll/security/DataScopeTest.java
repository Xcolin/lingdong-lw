package com.lingdong.payroll.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DataScopeTest {

    @Test
    void adminCanAccessAllData() {
        CurrentUser admin = new CurrentUser(1L, "admin", "系统管理员", Set.of("ADMIN"));

        DataScope scope = DataScope.forUser(admin);

        assertThat(scope.unrestricted()).isTrue();
        assertThat(scope.createdBy()).isNull();
    }

    @Test
    void operatorCanAccessOwnDataOnly() {
        CurrentUser operator = new CurrentUser(9L, "operator", "录入人员", Set.of("OPERATOR"));

        DataScope scope = DataScope.forUser(operator);

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.createdBy()).isEqualTo(9L);
    }
}
