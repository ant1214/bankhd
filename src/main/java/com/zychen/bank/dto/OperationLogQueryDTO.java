package com.zychen.bank.dto;

import lombok.Data;

@Data
public class OperationLogQueryDTO {
    private String userId;        // 操作用户ID（可选）
    private String targetType;    // 目标类型：USER/CARD/ACCOUNT（可选）
    private String targetId;      // 目标ID（可选）
    private String operationType; // 操作类型：LOGIN/BIND_CARD等（可选）
    private String startTime;     // 开始时间 yyyy-MM-dd（可选）
    private String endTime;       // 结束时间 yyyy-MM-dd（可选）
    private Integer page = 1;     // 页码
    private Integer pageSize = 10;// 每页大小
}