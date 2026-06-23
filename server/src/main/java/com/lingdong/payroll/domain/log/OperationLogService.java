package com.lingdong.payroll.domain.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingdong.payroll.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogService {

    private final OperationLogMapper mapper;
    private final ObjectMapper objectMapper;

    public OperationLogService(OperationLogMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public void record(CurrentUser operator, String action, String module, Long businessId, Object beforeValue, Object afterValue) {
        OperationLog log = new OperationLog();
        log.setOperatorId(operator.id());
        log.setOperatorName(operator.displayName());
        log.setAction(action);
        log.setModule(module);
        log.setBusinessId(businessId);
        log.setBeforeValue(toJson(beforeValue));
        log.setAfterValue(toJson(afterValue));
        log.setOperatedAt(LocalDateTime.now());
        mapper.insert(log);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }
}
