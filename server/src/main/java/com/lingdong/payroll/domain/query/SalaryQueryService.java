package com.lingdong.payroll.domain.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.domain.query.dto.SalaryQueryRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalaryQueryService {

    private final JdbcTemplate jdbcTemplate;

    public SalaryQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<SalaryQueryRecord> publicQuery(String name, String idCardNo) {
        String payrollWhere = payrollBaseWhere() + " and i.name = ? and i.id_card_no = ?";
        String advanceWhere = advanceBaseWhere() + " and a.name = ? and a.id_card_no = ?";
        List<Object> params = List.of(name.trim(), idCardNo.trim());
        return query(payrollWhere, advanceWhere, params, params, 1, 100);
    }

    public Page<SalaryQueryRecord> adminQuery(String name, long page, long size) {
        StringBuilder payrollWhere = new StringBuilder(payrollBaseWhere());
        StringBuilder advanceWhere = new StringBuilder(advanceBaseWhere());
        List<Object> payrollParams = new ArrayList<>();
        List<Object> advanceParams = new ArrayList<>();
        if (StringUtils.hasText(name)) {
            payrollWhere.append(" and i.name like ?");
            advanceWhere.append(" and a.name like ?");
            String like = "%" + name.trim() + "%";
            payrollParams.add(like);
            advanceParams.add(like);
        }
        return query(payrollWhere.toString(), advanceWhere.toString(), payrollParams, advanceParams, page, size);
    }

    private Page<SalaryQueryRecord> query(
            String payrollWhere,
            String advanceWhere,
            List<Object> payrollParams,
            List<Object> advanceParams,
            long page,
            long size
    ) {
        List<Object> countParams = new ArrayList<>(payrollParams);
        countParams.addAll(advanceParams);
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from (" +
                        "select i.id from payroll_batch_item i join payroll_batch b on b.id = i.batch_id " + payrollWhere +
                        " union all " +
                        "select a.id from advance_payment a " + advanceWhere +
                        ") q",
                Long.class,
                countParams.toArray()
        );
        List<Object> pageParams = new ArrayList<>(payrollParams);
        pageParams.addAll(advanceParams);
        pageParams.add(size);
        pageParams.add((page - 1) * size);
        List<SalaryQueryRecord> records = jdbcTemplate.query(
                "select * from (" +
                        "select 'PAYROLL' as source_type, b.id as batch_id, b.batch_name, b.pay_date, b.created_at as batch_created_at, " +
                        "i.id as item_id, i.name, i.id_card_no, i.phone, i.bank_account, i.account_name, i.bank_name, " +
                        "i.amount, i.summary, i.remark " +
                        "from payroll_batch_item i join payroll_batch b on b.id = i.batch_id " +
                        payrollWhere +
                        " union all " +
                        "select 'ADVANCE' as source_type, null as batch_id, '平时预支' as batch_name, null as pay_date, a.advance_time as batch_created_at, " +
                        "a.id as item_id, a.name, a.id_card_no, a.phone, '' as bank_account, a.name as account_name, '' as bank_name, " +
                        "a.amount, a.advance_method as summary, concat(a.reason, case when a.remark is null or a.remark = '' then '' else concat('；', a.remark) end) as remark " +
                        "from advance_payment a " + advanceWhere +
                        ") q order by batch_created_at desc, item_id desc limit ? offset ?",
                mapper(),
                pageParams.toArray()
        );
        Page<SalaryQueryRecord> result = new Page<>(page, size);
        result.setTotal(total == null ? 0 : total);
        result.setRecords(records);
        return result;
    }

    private String payrollBaseWhere() {
        return " where b.deleted = 0 and b.actual_paid = 1 and coalesce(i.target_type, 'PERSON') = 'PERSON'";
    }

    private String advanceBaseWhere() {
        return " where a.deleted = 0";
    }

    private RowMapper<SalaryQueryRecord> mapper() {
        return (rs, rowNum) -> new SalaryQueryRecord(
                getLongObject(rs, "batch_id"),
                rs.getString("batch_name"),
                toLocalDate(rs.getDate("pay_date")),
                toLocalDateTime(rs.getTimestamp("batch_created_at")),
                rs.getLong("item_id"),
                rs.getString("name"),
                rs.getString("id_card_no"),
                rs.getString("phone"),
                rs.getString("bank_account"),
                rs.getString("account_name"),
                rs.getString("bank_name"),
                rs.getBigDecimal("amount"),
                rs.getString("summary"),
                rs.getString("remark"),
                rs.getString("source_type")
        );
    }

    private Long getLongObject(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
