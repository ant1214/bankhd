package com.zychen.bank.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Long logId;
    private String userId;
    private Integer userRole;
    private String module;          // AUTH/CARD/TRANSACTION/SECURITY
    private String operationType;   // LOGIN/BIND_CARD/DEPOSIT等
    private String operationDetail;
    private String targetType;      // USER/CARD/ACCOUNT
    private String targetId;
    private String ipAddress;
    private String userAgent;
    private Integer status;         // 1=成功，0=失败
    private String errorMessage;
    private Integer executionTime;  // 毫秒
    private LocalDateTime createdTime;
}