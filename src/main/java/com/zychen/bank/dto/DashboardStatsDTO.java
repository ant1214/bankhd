package com.zychen.bank.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class DashboardStatsDTO {

    // 用户统计
    private Long totalUsers;          // 总用户数
    private Long activeUsers;         // 活跃用户数
    private Long frozenUsers;         // 冻结用户数
    private Long newUsersToday;       // 今日新增用户

    // 银行卡统计
    private Long totalCards;          // 总银行卡数
    private Long activeCards;         // 正常卡数
    private Long frozenCards;         // 冻结卡数
    private Long lostCards;          // 挂失卡数

    // 交易统计
    private Long totalTransactions;   // 总交易笔数
    private Long todayTransactions;   // 今日交易笔数
    private Long pendingTransactions; // 处理中交易数

    // 资金统计
    private BigDecimal totalBalance;      // 系统总余额
    private BigDecimal todayIncome;       // 今日收入
    private BigDecimal todayOutcome;      // 今日支出
    private BigDecimal fixedDepositTotal; // 定期存款总额

    // 定期存款统计
    private Long activeFixedDeposits;     // 持有中定期存款数
    private Long maturedFixedDeposits;    // 已到期定期存款数

    // 系统状态
    private String systemStatus;          // 系统状态：健康、警告、危险
    private String securityLevel;         // 安全等级：高、中、低

    // 今日注册用户列表（前5个）
    private Map<String, Object> recentUsers;

    // 系统告警
    private Map<String, Object> systemAlerts;
}