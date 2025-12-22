package com.zychen.bank.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminTransactionVO {
    private String transNo;                  // 交易流水号
    private String cardId;                   // 银行卡号
    private String userId;                   // 用户ID
    private String userName;                 // 用户名
    private String transType;               // 交易类型
    private String transTypeText;           // 交易类型中文
    private String transSubtype;            // 交易子类型
    private BigDecimal amount;               // 交易金额
    private BigDecimal balanceBefore;        // 交易前余额
    private BigDecimal balanceAfter;         // 交易后余额
    private BigDecimal fee;                  // 手续费
    private String currency;                 // 币种
    private Integer status;                  // 状态
    private String statusText;               // 状态中文
    private String remark;                   // 备注
    private String operatorId;               // 操作员ID
    private String operatorType;             // 操作员类型
    private LocalDateTime transTime;         // 交易时间
    private LocalDateTime completedTime;     // 完成时间
    private String ipAddress;                // IP地址
    private String deviceInfo;               // 设备信息
}
