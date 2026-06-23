package com.lingdong.payroll.domain.person;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lingdong.payroll.common.BusinessException;
import com.lingdong.payroll.domain.log.OperationLogService;
import com.lingdong.payroll.domain.person.dto.PayeePersonUpsertRequest;
import com.lingdong.payroll.security.CurrentUser;
import com.lingdong.payroll.security.DataScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayeePersonService {

    private final PayeePersonMapper mapper;
    private final OperationLogService operationLogService;

    public PayeePersonService(PayeePersonMapper mapper, OperationLogService operationLogService) {
        this.mapper = mapper;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public PayeePerson upsertByIdCard(PayeePersonUpsertRequest request, CurrentUser currentUser) {
        PayeePerson existing = mapper.selectOne(new LambdaQueryWrapper<PayeePerson>()
                .eq(PayeePerson::getCreatedBy, currentUser.id())
                .eq(PayeePerson::getIdCardNo, request.idCardNo()));
        if (existing == null) {
            PayeePerson created = new PayeePerson();
            apply(created, request);
            created.setCreatedBy(currentUser.id());
            created.setEnabled(true);
            created.setDeleted(false);
            mapper.insert(created);
            operationLogService.record(currentUser, "CREATE", "PAYEE_PERSON", created.getId(), null, created);
            return created;
        }
        PayeePerson before = copy(existing);
        apply(existing, request);
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            existing.setDeleted(false);
            existing.setEnabled(true);
        }
        mapper.updateById(existing);
        operationLogService.record(currentUser, "UPDATE", "PAYEE_PERSON", existing.getId(), before, existing);
        return existing;
    }

    public Page<PayeePerson> list(String keyword, Boolean enabled, long page, long size, DataScope scope) {
        LambdaQueryWrapper<PayeePerson> query = new LambdaQueryWrapper<PayeePerson>()
                .eq(PayeePerson::getDeleted, false)
                .eq(enabled != null, PayeePerson::getEnabled, enabled)
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(PayeePerson::getName, keyword)
                        .or().like(PayeePerson::getIdCardNo, keyword)
                        .or().like(PayeePerson::getPhone, keyword)
                        .or().like(PayeePerson::getBankAccount, keyword))
                .eq(!scope.unrestricted(), PayeePerson::getCreatedBy, scope.createdBy())
                .orderByDesc(PayeePerson::getUpdatedAt);
        return mapper.selectPage(Page.of(page, size), query);
    }

    @Transactional
    public void updateEnabled(Long id, boolean enabled, CurrentUser currentUser) {
        PayeePerson person = getEditable(id, currentUser);
        PayeePerson before = copy(person);
        person.setEnabled(enabled);
        mapper.updateById(person);
        operationLogService.record(currentUser, enabled ? "ENABLE" : "DISABLE", "PAYEE_PERSON", id, before, person);
    }

    @Transactional
    public void delete(Long id, CurrentUser currentUser) {
        PayeePerson person = getEditable(id, currentUser);
        PayeePerson before = copy(person);
        person.setDeleted(true);
        mapper.updateById(person);
        operationLogService.record(currentUser, "DELETE", "PAYEE_PERSON", id, before, person);
    }

    private void apply(PayeePerson person, PayeePersonUpsertRequest request) {
        person.setName(request.name());
        person.setIdCardNo(request.idCardNo());
        person.setPhone(request.phone());
        person.setBankAccount(request.bankAccount());
        person.setAccountName(request.accountName());
        person.setBankName(request.bankName());
        person.setBankType(request.bankType());
        person.setBankCategory(request.bankCategory());
        person.setCnapsNo(request.cnapsNo());
    }

    private PayeePerson copy(PayeePerson source) {
        PayeePerson copy = new PayeePerson();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setIdCardNo(source.getIdCardNo());
        copy.setPhone(source.getPhone());
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

    private PayeePerson getEditable(Long id, CurrentUser currentUser) {
        PayeePerson person = mapper.selectById(id);
        if (person == null || Boolean.TRUE.equals(person.getDeleted())) {
            throw new BusinessException("人员不存在");
        }
        if (!currentUser.isAdmin() && !person.getCreatedBy().equals(currentUser.id())) {
            throw new BusinessException("无权操作该人员");
        }
        return person;
    }
}
