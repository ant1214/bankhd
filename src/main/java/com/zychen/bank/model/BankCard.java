package com.zychen.bank.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BankCard {
    private String cardId;           // 银行卡号
    private String userId;           // 用户ID
    private String cardPassword;     // 交易密码（加密后）
    private BigDecimal balance;      // 当前余额
    private BigDecimal availableBalance;  // 可用余额
    private BigDecimal frozenAmount; // 冻结金额
    private Integer cardType;        // 卡类型：0=储蓄卡，1=信用卡
    private Integer status;          // 状态：0=正常，1=挂失，2=冻结，3=已注销
    private LocalDateTime bindTime;  // 绑定时间
    private LocalDateTime lastTransactionTime;  // 最后交易时间
    private BigDecimal dailyLimit;   // 日交易限额
    private BigDecimal monthlyLimit; // 月交易限额
}