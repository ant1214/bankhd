package com.zychen.bank.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TransactionQueryDTO {

    private String cardId;  // 银行卡号（可选）

    private String transType;  // 交易类型：DEPOSIT, WITHDRAW, ALL（可选）

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;  // 开始日期（可选）

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;  // 结束日期（可选）

    private Integer page = 1;  // 页码，默认第1页

    private Integer pageSize = 20;  // 每页大小，默认20条
}