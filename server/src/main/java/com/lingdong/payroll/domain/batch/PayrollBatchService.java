package com.lingdong.payroll.domain.batch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.BusinessException;
import com.lingdong.payroll.domain.batch.dto.PayrollBatchItemRequest;
import com.lingdong.payroll.domain.batch.dto.PayrollBatchSaveRequest;
import com.lingdong.payroll.domain.log.OperationLogService;
import com.lingdong.payroll.domain.person.PayeePerson;
import com.lingdong.payroll.domain.person.PayeePersonService;
import com.lingdong.payroll.domain.person.dto.PayeePersonUpsertRequest;
import com.lingdong.payroll.domain.unit.PayingUnit;
import com.lingdong.payroll.domain.unit.PayingUnitService;
import com.lingdong.payroll.domain.unit.dto.PayingUnitUpsertRequest;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.DataScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class PayrollBatchService {

    private final PayrollBatchMapper batchMapper;
    private final PayrollBatchItemMapper itemMapper;
    private final PayeePersonService personService;
    private final PayingUnitService unitService;
    private final OperationLogService operationLogService;
    private final JdbcTemplate jdbcTemplate;

    public PayrollBatchService(
            PayrollBatchMapper batchMapper,
            PayrollBatchItemMapper itemMapper,
            PayeePersonService personService,
            PayingUnitService unitService,
            OperationLogService operationLogService,
            JdbcTemplate jdbcTemplate
    ) {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.personService = personService;
        this.unitService = unitService;
        this.operationLogService = operationLogService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public PayrollBatch save(Long id, PayrollBatchSaveRequest request, CurrentUser currentUser) {
        boolean creating = id == null;
        PayrollBatch batch = creating ? new PayrollBatch() : getVisible(id, currentUser);
        if (!creating) {
            ensureMutable(batch);
        }
        PayrollBatch before = creating ? null : copy(batch);
        batch.setBatchName(request.batchName());
        batch.setPayDate(request.payDate());
        batch.setDefaultSummary(request.defaultSummary());
        batch.setRemark(request.remark());
        List<PayrollBatchItemRequest> items = request.items() == null ? List.of() : request.items();
        batch.setTotalPeople(items.size());
        batch.setTotalAmount(items.stream()
                .map(PayrollBatchItemRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        if (creating) {
            batch.setCreatedBy(currentUser.id());
            batch.setActualPaid(false);
            batch.setDeleted(false);
            batchMapper.insert(batch);
        } else {
            batchMapper.updateById(batch);
            itemMapper.deleteByBatchId(batch.getId());
        }
        int rowNo = 1;
        for (PayrollBatchItemRequest itemRequest : items) {
            String targetType = normalizeTargetType(itemRequest.targetType());
            Long personId = null;
            Long unitId = null;
            if ("UNIT".equals(targetType)) {
                PayingUnit unit = unitService.upsertByBankAccount(new PayingUnitUpsertRequest(
                        itemRequest.bankAccount(),
                        itemRequest.accountName(),
                        itemRequest.bankName(),
                        itemRequest.bankType(),
                        itemRequest.bankCategory(),
                        itemRequest.cnapsNo()
                ), currentUser);
                unitId = unit.getId();
            } else {
                if (itemRequest.idCardNo() == null || itemRequest.idCardNo().isBlank()) {
                    throw new BusinessException("第 " + rowNo + " 行人员明细缺少身份证号");
                }
                PayeePerson person = personService.upsertByIdCard(new PayeePersonUpsertRequest(
                        itemRequest.name(),
                        itemRequest.idCardNo(),
                        itemRequest.phone(),
                        itemRequest.bankAccount(),
                        itemRequest.accountName(),
                        itemRequest.bankName(),
                        itemRequest.bankType(),
                        itemRequest.bankCategory(),
                        itemRequest.cnapsNo()
                ), currentUser);
                personId = person.getId();
            }
            PayrollBatchItem item = toItem(batch.getId(), targetType, personId, unitId, rowNo++, itemRequest);
            itemMapper.insert(item);
        }
        operationLogService.record(currentUser, creating ? "CREATE" : "UPDATE", "PAYROLL_BATCH", batch.getId(), before, batch);
        return batch;
    }

    public Page<PayrollBatch> list(String keyword, LocalDate startDate, LocalDate endDate, long page, long size, DataScope scope) {
        LambdaQueryWrapper<PayrollBatch> query = new LambdaQueryWrapper<PayrollBatch>()
                .eq(PayrollBatch::getDeleted, false)
                .like(keyword != null && !keyword.isBlank(), PayrollBatch::getBatchName, keyword)
                .ge(startDate != null, PayrollBatch::getCreatedAt, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, PayrollBatch::getCreatedAt, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .eq(!scope.unrestricted(), PayrollBatch::getCreatedBy, scope.createdBy())
                .orderByDesc(PayrollBatch::getUpdatedAt);
        Page<PayrollBatch> result = batchMapper.selectPage(Page.of(page, size), query);
        fillCreatorName(result.getRecords());
        return result;
    }

    public PayrollBatch getVisible(Long id, CurrentUser currentUser) {
        PayrollBatch batch = batchMapper.selectById(id);
        if (batch == null || Boolean.TRUE.equals(batch.getDeleted())) {
            throw new BusinessException("工资批次不存在");
        }
        if (!currentUser.isAdmin() && !batch.getCreatedBy().equals(currentUser.id())) {
            throw new BusinessException("无权操作该工资批次");
        }
        return batch;
    }

    public List<PayrollBatchItem> listItems(Long batchId, CurrentUser currentUser) {
        getVisible(batchId, currentUser);
        return itemMapper.selectByBatchId(batchId);
    }

    @Transactional
    public void markActualPaid(Long id, CurrentUser currentUser) {
        PayrollBatch batch = getVisible(id, currentUser);
        PayrollBatch before = copy(batch);
        batch.setActualPaid(true);
        batch.setActualPaidAt(LocalDateTime.now());
        batch.setActualPaidBy(currentUser.id());
        batchMapper.updateById(batch);
        operationLogService.record(currentUser, "ACTUAL_PAID", "PAYROLL_BATCH", id, before, batch);
    }

    @Transactional
    public void delete(Long id, CurrentUser currentUser) {
        PayrollBatch batch = getVisible(id, currentUser);
        ensureMutable(batch);
        PayrollBatch before = copy(batch);
        batch.setDeleted(true);
        batchMapper.updateById(batch);
        operationLogService.record(currentUser, "DELETE", "PAYROLL_BATCH", id, before, batch);
    }

    private void ensureMutable(PayrollBatch batch) {
        if (Boolean.TRUE.equals(batch.getActualPaid())) {
            throw new BusinessException("工资批次已确认实际已发，不能修改或删除");
        }
    }

    private PayrollBatchItem toItem(Long batchId, String targetType, Long personId, Long unitId, int rowNo, PayrollBatchItemRequest request) {
        PayrollBatchItem item = new PayrollBatchItem();
        item.setBatchId(batchId);
        item.setTargetType(targetType);
        item.setPersonId(personId);
        item.setUnitId(unitId);
        item.setRowNo(rowNo);
        item.setName(request.name());
        item.setIdCardNo(request.idCardNo());
        item.setPhone(request.phone());
        item.setBankAccount(request.bankAccount());
        item.setAccountName(request.accountName());
        item.setBankName(request.bankName());
        item.setBankType(request.bankType());
        item.setBankCategory(request.bankCategory());
        item.setCnapsNo(request.cnapsNo());
        item.setAmount(request.amount());
        item.setSummary(request.summary());
        item.setRemark(request.remark());
        return item;
    }

    private String normalizeTargetType(String targetType) {
        return "UNIT".equalsIgnoreCase(targetType) ? "UNIT" : "PERSON";
    }

    private PayrollBatch copy(PayrollBatch source) {
        PayrollBatch copy = new PayrollBatch();
        copy.setId(source.getId());
        copy.setBatchName(source.getBatchName());
        copy.setPayDate(source.getPayDate());
        copy.setDefaultSummary(source.getDefaultSummary());
        copy.setRemark(source.getRemark());
        copy.setTotalPeople(source.getTotalPeople());
        copy.setTotalAmount(source.getTotalAmount());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setActualPaid(source.getActualPaid());
        copy.setActualPaidAt(source.getActualPaidAt());
        copy.setActualPaidBy(source.getActualPaidBy());
        copy.setDeleted(source.getDeleted());
        return copy;
    }

    private void fillCreatorName(List<PayrollBatch> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(PayrollBatch::getCreatedBy).filter(java.util.Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return;
        }
        String placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        Map<Long, String> names = jdbcTemplate.query(
                "select id, display_name from sys_user where id in (" + placeholders + ")",
                (rs) -> {
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
