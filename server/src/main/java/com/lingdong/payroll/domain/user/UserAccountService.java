package com.lingdong.payroll.domain.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.BusinessException;
import com.lingdong.payroll.domain.log.OperationLogService;
import com.lingdong.payroll.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserAccountService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public UserAccountService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, OperationLogService operationLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    public CurrentUser authenticate(String username, String password) {
        List<UserPasswordRow> rows = jdbcTemplate.query(
                "select id, username, password_hash, display_name, enabled from sys_user where username = ?",
                (rs, rowNum) -> new UserPasswordRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("display_name"),
                        rs.getBoolean("enabled")
                ),
                username
        );
        if (rows.isEmpty() || !Boolean.TRUE.equals(rows.get(0).enabled()) || !passwordEncoder.matches(password, rows.get(0).passwordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        UserPasswordRow user = rows.get(0);
        Set<String> roleCodes = roleCodes(user.id());
        Set<String> permissions = permissionCodes(user.id());
        return new CurrentUser(user.id(), user.username(), user.displayName(), roleCodes, permissions);
    }

    public Page<UserSummary> list(String keyword, Boolean enabled, long page, long size) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        SqlParamsBuilder params = new SqlParamsBuilder();
        if (StringUtils.hasText(keyword)) {
            where.append(" and (username like ? or display_name like ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like).add(like);
        }
        if (enabled != null) {
            where.append(" and enabled = ?");
            params.add(enabled ? 1 : 0);
        }
        Long total = jdbcTemplate.queryForObject("select count(*) from sys_user" + where, Long.class, params.values());
        List<UserSummary> records = jdbcTemplate.query(
                "select id, username, display_name, enabled, created_at from sys_user" + where +
                        " order by id desc limit ? offset ?",
                userMapper(),
                params.copy().add(size).add((page - 1) * size).values()
        ).stream().map(this::withAuthInfo).toList();
        Page<UserSummary> result = new Page<>(page, size);
        result.setTotal(total == null ? 0 : total);
        result.setRecords(records);
        return result;
    }

    public List<PermissionSummary> permissions() {
        return jdbcTemplate.query(
                "select code, name, type, menu_path, sort_no from sys_permission order by sort_no, id",
                (rs, rowNum) -> new PermissionSummary(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("menu_path"),
                        rs.getInt("sort_no")
                )
        );
    }

    @Transactional
    public UserSummary create(String username, String displayName, String password, Boolean enabled, Set<String> permissionCodes, CurrentUser operator) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("请填写密码");
        }
        Integer exists = jdbcTemplate.queryForObject("select count(*) from sys_user where username = ?", Integer.class, username);
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }
        jdbcTemplate.update(
                "insert into sys_user(username, password_hash, display_name, enabled) values (?, ?, ?, ?)",
                username.trim(),
                passwordEncoder.encode(password),
                displayName.trim(),
                enabled == null || enabled ? 1 : 0
        );
        Long id = jdbcTemplate.queryForObject("select id from sys_user where username = ?", Long.class, username.trim());
        grantOperatorRole(id);
        replacePermissions(id, permissionCodes);
        UserSummary created = get(id);
        operationLogService.record(operator, "CREATE", "SYS_USER", id, null, created);
        return created;
    }

    @Transactional
    public UserSummary update(Long id, String displayName, Boolean enabled, Set<String> permissionCodes, CurrentUser operator) {
        UserSummary before = get(id);
        jdbcTemplate.update(
                "update sys_user set display_name = ?, enabled = ? where id = ?",
                displayName.trim(),
                enabled == null || enabled ? 1 : 0,
                id
        );
        replacePermissions(id, permissionCodes);
        UserSummary after = get(id);
        operationLogService.record(operator, "UPDATE", "SYS_USER", id, before, after);
        return after;
    }

    @Transactional
    public void updateEnabled(Long id, boolean enabled, CurrentUser operator) {
        UserSummary before = get(id);
        if (before.roleCodes().contains("ADMIN") && !enabled) {
            throw new BusinessException("管理员不能停用");
        }
        jdbcTemplate.update("update sys_user set enabled = ? where id = ?", enabled ? 1 : 0, id);
        operationLogService.record(operator, enabled ? "ENABLE" : "DISABLE", "SYS_USER", id, before, get(id));
    }

    @Transactional
    public void resetPassword(Long id, String password, CurrentUser operator) {
        UserSummary before = get(id);
        jdbcTemplate.update("update sys_user set password_hash = ? where id = ?", passwordEncoder.encode(password), id);
        operationLogService.record(operator, "RESET_PASSWORD", "SYS_USER", id, before, get(id));
    }

    @Transactional
    public void updatePermissions(Long id, Set<String> permissionCodes, CurrentUser operator) {
        UserSummary before = get(id);
        replacePermissions(id, permissionCodes);
        operationLogService.record(operator, "AUTHORIZE", "SYS_USER", id, before, get(id));
    }

    public UserSummary get(Long id) {
        List<UserSummary> rows = jdbcTemplate.query(
                "select id, username, display_name, enabled, created_at from sys_user where id = ?",
                userMapper(),
                id
        );
        if (rows.isEmpty()) {
            throw new BusinessException("用户不存在");
        }
        return withAuthInfo(rows.get(0));
    }

    private UserSummary withAuthInfo(UserSummary user) {
        return new UserSummary(user.id(), user.username(), user.displayName(), user.enabled(), roleCodes(user.id()), permissionCodes(user.id()), user.createdAt());
    }

    private Set<String> roleCodes(Long userId) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                "select r.code from sys_role r join sys_user_role ur on ur.role_id = r.id where ur.user_id = ? order by r.id",
                String.class,
                userId
        ));
    }

    private Set<String> permissionCodes(Long userId) {
        Set<String> roles = roleCodes(userId);
        if (roles.contains("ADMIN")) {
            return new LinkedHashSet<>(jdbcTemplate.queryForList("select code from sys_permission order by sort_no, id", String.class));
        }
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                "select p.code from sys_permission p join sys_user_permission up on up.permission_id = p.id where up.user_id = ? order by p.sort_no, p.id",
                String.class,
                userId
        ));
    }

    private void grantOperatorRole(Long userId) {
        jdbcTemplate.update(
                "insert into sys_user_role(user_id, role_id) select ?, id from sys_role where code = 'OPERATOR'",
                userId
        );
    }

    private void replacePermissions(Long userId, Set<String> permissionCodes) {
        jdbcTemplate.update("delete from sys_user_permission where user_id = ?", userId);
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return;
        }
        for (String code : permissionCodes) {
            jdbcTemplate.update(
                    "insert into sys_user_permission(user_id, permission_id) select ?, id from sys_permission where code = ?",
                    userId,
                    code
            );
        }
    }

    private RowMapper<UserSummary> userMapper() {
        return (ResultSet rs, int rowNum) -> new UserSummary(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getBoolean("enabled"),
                Set.of(),
                Set.of(),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record UserPasswordRow(Long id, String username, String passwordHash, String displayName, Boolean enabled) {
    }

    private static final class SqlParamsBuilder {
        private final java.util.ArrayList<Object> params = new java.util.ArrayList<>();

        SqlParamsBuilder add(Object value) {
            params.add(value);
            return this;
        }

        Object[] values() {
            return params.toArray();
        }

        SqlParamsBuilder copy() {
            SqlParamsBuilder next = new SqlParamsBuilder();
            next.params.addAll(params);
            return next;
        }
    }
}
