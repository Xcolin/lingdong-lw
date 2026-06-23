package com.lingdong.payroll.domain.dashboard;

import com.lingdong.payroll.domain.dashboard.dto.DashboardSummary;
import com.lingdong.payroll.domain.dashboard.dto.LibraryStats;
import com.lingdong.payroll.domain.dashboard.dto.PayrollStats;
import com.lingdong.payroll.security.DataScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummary summary(LocalDate startDate, LocalDate endDate, DataScope scope) {
        LocalDate actualEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate actualStart = startDate == null ? actualEnd.minusDays(29) : startDate;
        LocalDateTime from = actualStart.atStartOfDay();
        LocalDateTime toExclusive = actualEnd.plusDays(1).atStartOfDay();
        return new DashboardSummary(
                actualStart,
                actualEnd,
                payrollStats(from, toExclusive, scope),
                libraryStats("payee_person", from, toExclusive, scope),
                libraryStats("paying_unit", from, toExclusive, scope)
        );
    }

    private PayrollStats payrollStats(LocalDateTime from, LocalDateTime toExclusive, DataScope scope) {
        CountAndAmount batch = queryOne(
                "select count(*) as count_value, coalesce(sum(total_amount), 0) as amount_value from payroll_batch" +
                        whereCreated(scope, "created_at", "created_by", "deleted = 0 and actual_paid = 1"),
                from,
                toExclusive,
                scope
        );
        Long itemCount = jdbcTemplate.queryForObject(
                "select count(*) from payroll_batch_item i join payroll_batch b on b.id = i.batch_id" +
                        whereCreated(scope, "b.created_at", "b.created_by", "b.deleted = 0 and b.actual_paid = 1"),
                Long.class,
                params(from, toExclusive, scope).toArray()
        );
        Long exportCount = jdbcTemplate.queryForObject(
                "select count(*) from export_record e join payroll_batch b on b.id = e.batch_id" + whereCreated(scope, "e.created_at", "e.created_by", "b.deleted = 0 and b.actual_paid = 1"),
                Long.class,
                params(from, toExclusive, scope).toArray()
        );
        CountAndAmount advance = queryOne(
                "select count(*) as count_value, coalesce(sum(amount), 0) as amount_value from advance_payment" +
                        whereCreated(scope, "advance_time", "created_by", "deleted = 0"),
                from,
                toExclusive,
                scope
        );
        long totalItemCount = (itemCount == null ? 0 : itemCount) + advance.count();
        BigDecimal totalAmount = batch.amount().add(advance.amount());
        return new PayrollStats(
                batch.count(),
                totalItemCount,
                totalAmount,
                exportCount == null ? 0 : exportCount,
                advance.count(),
                advance.amount()
        );
    }

    private LibraryStats libraryStats(String tableName, LocalDateTime from, LocalDateTime toExclusive, DataScope scope) {
        CountAndStatus stats = jdbcTemplate.queryForObject(
                "select count(*) as total_count, " +
                        "sum(case when enabled = 1 then 1 else 0 end) as enabled_count, " +
                        "sum(case when enabled = 0 then 1 else 0 end) as disabled_count " +
                        "from " + tableName + whereCreated(scope, "created_at", "created_by", "deleted = 0"),
                (rs, rowNum) -> new CountAndStatus(
                        rs.getLong("total_count"),
                        rs.getLong("enabled_count"),
                        rs.getLong("disabled_count")
                ),
                params(from, toExclusive, scope).toArray()
        );
        CountAndStatus value = stats == null ? new CountAndStatus(0, 0, 0) : stats;
        return new LibraryStats(value.total(), value.enabled(), value.disabled());
    }

    private CountAndAmount queryOne(String sql, LocalDateTime from, LocalDateTime toExclusive, DataScope scope) {
        CountAndAmount value = jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> new CountAndAmount(rs.getLong("count_value"), rs.getBigDecimal("amount_value")),
                params(from, toExclusive, scope).toArray()
        );
        return value == null ? new CountAndAmount(0, BigDecimal.ZERO) : value;
    }

    private String whereCreated(DataScope scope, String dateColumn, String createdByColumn, String extraCondition) {
        StringBuilder where = new StringBuilder(" where ");
        if (extraCondition != null && !extraCondition.isBlank()) {
            where.append(extraCondition).append(" and ");
        }
        where.append(dateColumn).append(" >= ? and ").append(dateColumn).append(" < ?");
        if (!scope.unrestricted()) {
            where.append(" and ").append(createdByColumn).append(" = ?");
        }
        return where.toString();
    }

    private List<Object> params(LocalDateTime from, LocalDateTime toExclusive, DataScope scope) {
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.valueOf(from));
        params.add(Timestamp.valueOf(toExclusive));
        if (!scope.unrestricted()) {
            params.add(scope.createdBy());
        }
        return params;
    }

    private record CountAndAmount(long count, BigDecimal amount) {
    }

    private record CountAndStatus(long total, long enabled, long disabled) {
    }
}
