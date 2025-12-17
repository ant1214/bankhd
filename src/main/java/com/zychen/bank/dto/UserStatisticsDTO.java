package com.zychen.bank.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UserStatisticsDTO {
    // 资产总览
    private BigDecimal totalBalance;          // 所有银行卡的balance总和
    private BigDecimal availableBalance;      // 所有银行卡的available_balance总和
    private BigDecimal frozenAmount;          // 所有银行卡的frozen_amount总和
    private BigDecimal fixedDepositAmount;    // fixed_deposit表中principal的总和（状态为0或1）

    // 银行卡统计
    private Integer cardCount;                // 银行卡总数
    private Integer activeCardCount;          // status=0的正常卡数量

    // 本月交易统计
    private MonthStatistics thisMonth;

    @Data
    public static class MonthStatistics {
        private Integer depositCount;         // DEPOSIT类型交易次数
        private BigDecimal depositAmount;     // DEPOSIT类型总额
        private Integer withdrawCount;        // WITHDRAW类型交易次数
        private BigDecimal withdrawAmount;    // WITHDRAW类型总额
        private BigDecimal interestEarned;    // INTEREST类型总额
        private Integer transactionCount;     // 所有类型交易总次数
    }
}