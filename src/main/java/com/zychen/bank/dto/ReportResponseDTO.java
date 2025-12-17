package com.zychen.bank.dto;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ReportResponseDTO {
    private String reportId;           // 报告ID
    private String userId;
    private String cardId;
    private String reportType;         // "monthly"或"yearly"
    private String period;             // 时间段，如"2024年01月"
    private Date generatedTime;        // 生成时间

    // 汇总统计
    private ReportSummary summary;

    // 分类统计（可选）
    private CategoryStatistics categoryStatistics;

    // 交易明细（可选）
    private List<TransactionSummary> transactions;

    private String downloadUrl;        // 下载链接（暂为空）

    @Data
    public static class ReportSummary {
        private BigDecimal startBalance;      // 期初余额
        private BigDecimal endBalance;        // 期末余额
        private BigDecimal totalDeposit;      // 总收入
        private BigDecimal totalWithdraw;     // 总支出
        private BigDecimal interestEarned;    // 利息收入
        private Integer transactionCount;     // 交易笔数
        private BigDecimal netChange;         // 净变化
        private BigDecimal avgDailyBalance;   // 日均余额（可选）
    }

    @Data
    public static class CategoryStatistics {
        private BigDecimal depositAmount;
        private BigDecimal withdrawAmount;
        private BigDecimal interestAmount;
        private BigDecimal transferAmount;     // 转账金额（如果系统支持）
        private BigDecimal feeAmount;          // 手续费总额
    }

    @Data
    public static class TransactionSummary {
        private LocalDateTime transTime;
        private String transType;
        private String transSubtype;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String remark;
    }
}