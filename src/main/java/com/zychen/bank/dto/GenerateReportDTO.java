package com.zychen.bank.dto;


import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class GenerateReportDTO {
    @NotNull(message = "用户ID不能为空")
    private String userId;

    private String cardId;        // 可选，不传则查询所有卡

    @NotNull(message = "报告类型不能为空")
    private String reportType;    // "monthly"=月账单，"yearly"=年账单

    @NotNull(message = "年份不能为空")
    private Integer year;

    private Integer month;        // 仅monthly需要

    private Boolean includeDetails = false;  // 是否包含交易明细
}