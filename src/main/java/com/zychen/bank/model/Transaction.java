package com.zychen.bank.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Transaction {
    private Long transId;
    private String transNo;
    private String cardId;
    private String userId;
    private String transType;      // DEPOSIT, WITHDRAW, TRANSFER, INTEREST
    private String transSubtype;   // CURRENT_DEPOSIT, FIXED_DEPOSIT
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal fee;
    private String currency;
    private Integer status;        // 1=成功，0=失败，2=处理中
    private String remark;
    private String operatorId;
    private String operatorType;   // USER, ADMIN
    private LocalDateTime transTime;
    private LocalDateTime completedTime;
    private String ipAddress;
    private String deviceInfo;
}