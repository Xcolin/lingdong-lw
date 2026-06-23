package com.lingdong.payroll.domain.advance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.BusinessException;
import com.lingdong.payroll.domain.advance.dto.AdvancePaymentSaveRequest;
import com.lingdong.payroll.domain.log.OperationLogService;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.DataScope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdvancePaymentService {

    private final AdvancePaymentMapper mapper;
    private final OperationLogService operationLogService;
    private final JdbcTemplate jdbcTemplate;

    public AdvancePaymentService(AdvancePaymentMapper mapper, OperationLogService operationLogService, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<AdvancePayment> list(String keyword, LocalDate startDate, LocalDate endDate, long page, long size, DataScope scope) {
        LambdaQueryWrapper<AdvancePayment> query = new LambdaQueryWrapper<AdvancePayment>()
                .eq(AdvancePayment::getDeleted, false)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(AdvancePayment::getName, keyword)
                        .or().like(AdvancePayment::getIdCardNo, keyword)
                        .or().like(AdvancePayment::getPhone, keyword)
                        .or().like(AdvancePayment::getReason, keyword))
                .ge(startDate != null, AdvancePayment::getAdvanceTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, AdvancePayment::getAdvanceTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .eq(!scope.unrestricted(), AdvancePayment::getCreatedBy, scope.createdBy())
                .orderByDesc(AdvancePayment::getAdvanceTime)
                .orderByDesc(AdvancePayment::getUpdatedAt);
        Page<AdvancePayment> result = mapper.selectPage(Page.of(page, size), query);
        fillCreatorName(result.getRecords());
        return result;
    }

    @Transactional
    public AdvancePayment save(Long id, AdvancePaymentSaveRequest request, CurrentUser currentUser) {
        boolean creating = id == null;
        AdvancePayment payment = creating ? new AdvancePayment() : getEditable(id, currentUser);
        AdvancePayment before = creating ? null : copy(payment);
        apply(payment, request);
        if (creating) {
            payment.setCreatedBy(currentUser.id());
            payment.setDeleted(false);
            mapper.insert(payment);
        } else {
            mapper.updateById(payment);
        }
        operationLogService.record(currentUser, creating ? "CREATE" : "UPDATE", "ADVANCE_PAYMENT", payment.getId(), before, payment);
        return payment;
    }

    @Transactional
    public void delete(Long id, CurrentUser currentUser) {
        AdvancePayment payment = getEditable(id, currentUser);
        AdvancePayment before = copy(payment);
        payment.setDeleted(true);
        mapper.updateById(payment);
        operationLogService.record(currentUser, "DELETE", "ADVANCE_PAYMENT", id, before, payment);
    }

    private AdvancePayment getEditable(Long id, CurrentUser currentUser) {
        AdvancePayment payment = mapper.selectById(id);
        if (payment == null || Boolean.TRUE.equals(payment.getDeleted())) {
            throw new BusinessException("预支记录不存在");
        }
        if (!currentUser.isAdmin() && !payment.getCreatedBy().equals(currentUser.id())) {
            throw new BusinessException("无权操作该预支记录");
        }
        return payment;
    }

    private void apply(AdvancePayment payment, AdvancePaymentSaveRequest request) {
        payment.setPersonId(request.personId());
        payment.setName(request.name());
        payment.setIdCardNo(request.idCardNo());
        payment.setPhone(request.phone());
        payment.setAmount(request.amount());
        payment.setAdvanceTime(request.advanceTime());
        payment.setAdvanceMethod(request.advanceMethod());
        payment.setReason(request.reason());
        payment.setRemark(request.remark());
    }

    private AdvancePayment copy(AdvancePayment source) {
        AdvancePayment copy = new AdvancePayment();
        copy.setId(source.getId());
        copy.setPersonId(source.getPersonId());
        copy.setName(source.getName());
        copy.setIdCardNo(source.getIdCardNo());
        copy.setPhone(source.getPhone());
        copy.setAmount(source.getAmount());
        copy.setAdvanceTime(source.getAdvanceTime());
        copy.setAdvanceMethod(source.getAdvanceMethod());
        copy.setReason(source.getReason());
        copy.setRemark(source.getRemark());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setDeleted(source.getDeleted());
        return copy;
    }

    private void fillCreatorName(List<AdvancePayment> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(AdvancePayment::getCreatedBy).filter(java.util.Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return;
        }
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        Map<Long, String> names = jdbcTemplate.query(
                "select id, display_name from sys_user where id in (" + placeholders + ")",
                rs -> {
                    java.util.HashMap<Long, String> map = new java.util.HashMap<>();
                    while (rs.next()) {
                        map.put(rs.getLong("id"), rs.getString("display_name"));
                    }
                    return map;
                },
                userIds.toArray()
        );
        records.forEach(record -> record.setCreatorName(names.get(record.getCreatedBy())));
    }
}
