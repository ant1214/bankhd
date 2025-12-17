package com.zychen.bank.service;

import com.zychen.bank.dto.OperationLogQueryDTO;
import java.util.Map;

public interface OperationLogService {

    /**
     * 记录操作日志
     */
    void logOperation(String userId, Integer userRole, String module,
                      String operationType, String operationDetail,
                      String targetType, String targetId,
                      String ipAddress, String userAgent,
                      Integer status, String errorMessage, Integer executionTime);

    /**
     * 查询操作日志（管理员用）
     */
    Map<String, Object> getOperationLogs(OperationLogQueryDTO queryDTO);
}