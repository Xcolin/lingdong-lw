package com.lingdong.payroll.domain.unit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.BusinessException;
import com.lingdong.payroll.domain.log.OperationLogService;
import com.lingdong.payroll.domain.unit.dto.PayingUnitUpsertRequest;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.DataScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayingUnitService {

    private final PayingUnitMapper mapper;
    private final OperationLogService operationLogService;

    public PayingUnitService(PayingUnitMapper mapper, OperationLogService operationLogService) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public PayingUnit upsertByBankAccount(PayingUnitUpsertRequest request, CurrentUser currentUser) {
        PayingUnit existing = mapper.selectOne(new LambdaQueryWrapper<PayingUnit>()
                .eq(PayingUnit::getCreatedBy, currentUser.id())
                .eq(PayingUnit::getBankAccount, request.bankAccount()));
        if (existing == null) {
            PayingUnit created = new PayingUnit();
            apply(created, request);
            created.setCreatedBy(currentUser.id());
            created.setEnabled(true);
            created.setDeleted(false);
            mapper.insert(created);
            operationLogService.record(currentUser, "CREATE", "PAYING_UNIT", created.getId(), null, created);
            return created;
        }
        PayingUnit before = copy(existing);
        apply(existing, request);
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            existing.setDeleted(false);
            existing.setEnabled(true);
        }
        mapper.updateById(existing);
        operationLogService.record(currentUser, "UPDATE", "PAYING_UNIT", existing.getId(), before, existing);
        return existing;
    }

    public Page<PayingUnit> list(String keyword, Boolean enabled, long page, long size, DataScope scope) {
        LambdaQueryWrapper<PayingUnit> query = new LambdaQueryWrapper<PayingUnit>()
                .eq(PayingUnit::getDeleted, false)
                .eq(enabled != null, PayingUnit::getEnabled, enabled)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(PayingUnit::getAccountName, keyword)
                        .or().like(PayingUnit::getBankAccount, keyword)
                        .or().like(PayingUnit::getBankName, keyword))
                .eq(!scope.unrestricted(), PayingUnit::getCreatedBy, scope.createdBy())
                .orderByDesc(PayingUnit::getUpdatedAt);
        return mapper.selectPage(Page.of(page, size), query);
    }

    public PayingUnit getVisible(Long id, CurrentUser currentUser) {
        PayingUnit unit = mapper.selectById(id);
        if (unit == null || Boolean.TRUE.equals(unit.getDeleted())) {
            throw new BusinessException("计发单位不存在");
        }
        return unit;
    }

    private PayingUnit getEditable(Long id, CurrentUser currentUser) {
        PayingUnit unit = mapper.selectById(id);
        if (unit == null || Boolean.TRUE.equals(unit.getDeleted())) {
            throw new BusinessException("计发单位不存在");
        }
        if (!currentUser.isAdmin() && !unit.getCreatedBy().equals(currentUser.id())) {
            throw new BusinessException("无权操作该计发单位");
        }
        return unit;
    }

    @Transactional
    public void delete(Long id, CurrentUser currentUser) {
        PayingUnit unit = getEditable(id, currentUser);
        PayingUnit before = copy(unit);
        unit.setDeleted(true);
        mapper.updateById(unit);
        operationLogService.record(currentUser, "DELETE", "PAYING_UNIT", id, before, unit);
    }

    @Transactional
    public void updateEnabled(Long id, boolean enabled, CurrentUser currentUser) {
        PayingUnit unit = getEditable(id, currentUser);
        PayingUnit before = copy(unit);
        unit.setEnabled(enabled);
        mapper.updateById(unit);
        operationLogService.record(currentUser, enabled ? "ENABLE" : "DISABLE", "PAYING_UNIT", id, before, unit);
    }

    private void apply(PayingUnit unit, PayingUnitUpsertRequest request) {
        unit.setBankAccount(request.bankAccount());
        unit.setAccountName(request.accountName());
        unit.setBankName(request.bankName());
        unit.setBankType(request.bankType());
        unit.setBankCategory(request.bankCategory());
        unit.setCnapsNo(request.cnapsNo());
    }

    private PayingUnit copy(PayingUnit source) {
        PayingUnit copy = new PayingUnit();
        copy.setId(source.getId());
        copy.setBankAccount(source.getBankAccount());
        copy.setAccountName(source.getAccountName());
        copy.setBankName(source.getBankName());
        copy.setBankType(source.getBankType());
        copy.setBankCategory(source.getBankCategory());
        copy.setCnapsNo(source.getCnapsNo());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setEnabled(source.getEnabled());
        copy.setDeleted(source.getDeleted());
        return copy;
    }
}
