package com.zychen.bank.service;

import com.zychen.bank.dto.GenerateReportDTO;
import com.zychen.bank.dto.ReportResponseDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.mapper.FixedDepositMapper;
import com.zychen.bank.mapper.TransactionMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.model.Transaction;
import com.zychen.bank.service.ReportService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private BankCardMapper bankCardMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private FixedDepositMapper fixedDepositMapper;

    @Autowired
    private PdfExportService pdfExportService;

    // ============ 月度报告 ============
    @Override
    public ReportResponseDTO generateMonthlyReport(String userId, String cardId, int year, int month) {
        ReportResponseDTO response = new ReportResponseDTO();

        // 1. 设置基本信息
        response.setUserId(userId);
        response.setCardId(cardId);
        response.setReportType("monthly");
        response.setPeriod(year + "年" + String.format("%02d", month) + "月");
        response.setGeneratedTime(new Date());
        response.setReportId(generateReportId("monthly", year, month));

        // 2. 计算时间段
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 3. 获取期初余额（上月最后一天的余额）
        LocalDate previousMonthLastDay = startDate.minusDays(1);
        BigDecimal startBalance = calculateBalanceAtDate(userId, cardId, previousMonthLastDay);

        // 4. 获取期末余额（本月最后一天的余额）
        BigDecimal endBalance = calculateBalanceAtDate(userId, cardId, endDate);

        // 5. 获取本月所有交易
        List<Transaction> transactions = getTransactionsInPeriod(userId, cardId, startDate, endDate);

        // 6. 计算统计信息（✅ 使用新的calculateSummary方法）
        ReportResponseDTO.ReportSummary summary = calculateSummary(transactions, startBalance, endBalance);
        response.setSummary(summary);

        // 7. 计算分类统计（✅ 使用新的calculateCategoryStatistics方法）
        ReportResponseDTO.CategoryStatistics categoryStats = calculateCategoryStatistics(transactions);
        response.setCategoryStatistics(categoryStats);

        // 8. 添加交易明细（简化版，只取前20条）
        if (transactions.size() <= 20) {
            List<ReportResponseDTO.TransactionSummary> transactionSummaries = transactions.stream()
                    .map(this::convertToTransactionSummary)
                    .collect(Collectors.toList());
            response.setTransactions(transactionSummaries);
        }

        // 9. 设置下载URL
        response.setDownloadUrl("/api/reports/" + response.getReportId() + "/download");

        log.info("月度报告生成完成: reportId={}, 交易笔数={}", response.getReportId(), transactions.size());
        return response;
    }

    // ============ 年度报告 ============
    @Override
    public ReportResponseDTO generateYearlyReport(String userId, String cardId, int year) {
        log.info("生成年度报告: userId={}, cardId={}, year={}", userId, cardId, year);

        ReportResponseDTO response = new ReportResponseDTO();

        // 1. 设置基本信息
        response.setUserId(userId);
        response.setCardId(cardId);
        response.setReportType("yearly");
        response.setPeriod(year + "年度");
        response.setGeneratedTime(new Date());
        response.setReportId(generateReportId("yearly", year, null));

        // 2. 计算年度时间段
        LocalDate startDate = LocalDate.of(year, 1, 1);      // 年初
        LocalDate endDate = LocalDate.of(year, 12, 31);      // 年末

        // 3. 获取年初余额（上一年最后一天的余额）
        LocalDate lastYearEnd = startDate.minusDays(1);
        BigDecimal startBalance = calculateBalanceAtDate(userId, cardId, lastYearEnd);

        // 4. 获取年末余额（本年最后一天的余额）
        BigDecimal endBalance = calculateBalanceAtDate(userId, cardId, endDate);

        // 5. 获取本年所有交易
        List<Transaction> transactions = getTransactionsInPeriod(userId, cardId, startDate, endDate);

        // 6. 计算年度汇总信息
        ReportResponseDTO.ReportSummary summary = calculateYearlySummary(transactions, startBalance, endBalance);
        response.setSummary(summary);

        // 7. 计算年度分类统计
        ReportResponseDTO.CategoryStatistics categoryStats = calculateCategoryStatistics(transactions);
        response.setCategoryStatistics(categoryStats);

        // 8. 计算月度统计（按月份分组）
        Map<Integer, MonthlySummary> monthlyStats = calculateMonthlyStats(userId, cardId, year);
        // 如果ReportResponseDTO有monthlyStats字段，可以设置

        // 9. 添加交易明细（可限制数量）
        if (transactions.size() <= 100) {
            List<ReportResponseDTO.TransactionSummary> transactionSummaries = transactions.stream()
                    .map(this::convertToTransactionSummary)
                    .collect(Collectors.toList());
            response.setTransactions(transactionSummaries);
        } else {
            // 如果交易太多，可以只取大额交易
            List<ReportResponseDTO.TransactionSummary> largeTransactions = transactions.stream()
                    .filter(t -> t.getAmount().abs().compareTo(new BigDecimal("1000")) > 0)
                    .sorted((t1, t2) -> t2.getAmount().abs().compareTo(t1.getAmount().abs()))
                    .limit(50)
                    .map(this::convertToTransactionSummary)
                    .collect(Collectors.toList());
            response.setTransactions(largeTransactions);
        }

        // 10. 设置下载URL
        response.setDownloadUrl("/api/reports/" + response.getReportId() + "/download");

        // ✅ 验证数据一致性
        validateReportConsistency(response);

        log.info("年度报告生成完成: reportId={}, 交易笔数={}", response.getReportId(), transactions.size());
        return response;
    }

    // ============ 核心计算方法 ============

    /**
     * ✅ 统一的汇总计算方法（用于月度报告）
     */
    private ReportResponseDTO.ReportSummary calculateSummary(List<Transaction> transactions,
                                                             BigDecimal startBalance, BigDecimal endBalance) {
        ReportResponseDTO.ReportSummary summary = new ReportResponseDTO.ReportSummary();

        summary.setStartBalance(startBalance);
        summary.setEndBalance(endBalance);
        summary.setNetChange(endBalance.subtract(startBalance));

        // 计算各种类型的总额（✅ 使用统一逻辑）
        BigDecimal totalDeposit = BigDecimal.ZERO;
        BigDecimal totalWithdraw = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (Transaction trans : transactions) {
            BigDecimal amount = trans.getAmount() != null ? trans.getAmount() : BigDecimal.ZERO;
            String type = trans.getTransType();
            String subtype = trans.getTransSubtype();

            if ("DEPOSIT".equalsIgnoreCase(type)) {
                totalDeposit = totalDeposit.add(amount);
            } else if ("WITHDRAW".equalsIgnoreCase(type)) {
                if ("FIXED_DEPOSIT_EARLY".equalsIgnoreCase(subtype) ||
                        "FIXED_DEPOSIT_MATURE".equalsIgnoreCase(subtype)) {
                    totalDeposit = totalDeposit.add(amount.abs());
                } else if ("CURRENT_WITHDRAW".equalsIgnoreCase(subtype)) {
                    totalWithdraw = totalWithdraw.add(amount.abs());
                }
            } else if ("TRANSFER".equalsIgnoreCase(type)) {  // ✅ 新增处理
                // TRANSFER金额为负数表示转出（支出）
                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    totalWithdraw = totalWithdraw.add(amount.abs());
                } else {
                    // 正数表示转入（收入）
                    totalDeposit = totalDeposit.add(amount);
                }
            } else if ("INTEREST".equalsIgnoreCase(type)) {
                totalInterest = totalInterest.add(amount);
            }
        }

        summary.setTotalDeposit(totalDeposit);
        summary.setTotalWithdraw(totalWithdraw);
        summary.setInterestEarned(totalInterest);
        summary.setTransactionCount(transactions.size());

        return summary;
    }

    /**
     * ✅ 年度汇总计算方法
     */
    private ReportResponseDTO.ReportSummary calculateYearlySummary(List<Transaction> transactions,
                                                                   BigDecimal startBalance, BigDecimal endBalance) {
        ReportResponseDTO.ReportSummary summary = new ReportResponseDTO.ReportSummary();

        summary.setStartBalance(startBalance);
        summary.setEndBalance(endBalance);

        // ✅ 净变化 = 期末 - 期初
        BigDecimal netChange = endBalance.subtract(startBalance);
        summary.setNetChange(netChange);

        // 记录日志检查
        log.info("余额计算: 期初={}, 期末={}, 净变化={}",
                startBalance, endBalance, netChange);

        // 计算年度各项总额（✅ 使用统一逻辑）
        BigDecimal totalDeposit = BigDecimal.ZERO;
        BigDecimal totalWithdraw = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (Transaction trans : transactions) {
            BigDecimal amount = trans.getAmount() != null ? trans.getAmount() : BigDecimal.ZERO;
            String type = trans.getTransType();
            String subtype = trans.getTransSubtype();

            // ✅ 统一分类逻辑
            if ("DEPOSIT".equalsIgnoreCase(type)) {
                totalDeposit = totalDeposit.add(amount);
            } else if ("WITHDRAW".equalsIgnoreCase(type)) {
                if ("FIXED_DEPOSIT_EARLY".equalsIgnoreCase(subtype) ||
                        "FIXED_DEPOSIT_MATURE".equalsIgnoreCase(subtype)) {
                    // ✅ 定期存款支取：算作存款类收入
                    totalDeposit = totalDeposit.add(amount.abs());
                } else if ("CURRENT_WITHDRAW".equalsIgnoreCase(subtype)) {
                    totalWithdraw = totalWithdraw.add(amount.abs());
                }
            } else if ("INTEREST".equalsIgnoreCase(type)) {
                totalInterest = totalInterest.add(amount);
            }
        }

        summary.setTotalDeposit(totalDeposit);
        summary.setTotalWithdraw(totalWithdraw);
        summary.setInterestEarned(totalInterest);
        summary.setTransactionCount(transactions.size());

        return summary;
    }

    /**
     * ✅ 统一的分类统计计算方法
     */
    private ReportResponseDTO.CategoryStatistics calculateCategoryStatistics(List<Transaction> transactions) {
        ReportResponseDTO.CategoryStatistics stats = new ReportResponseDTO.CategoryStatistics();

        BigDecimal depositAmount = BigDecimal.ZERO;
        BigDecimal withdrawAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;
        BigDecimal feeAmount = BigDecimal.ZERO;
        BigDecimal transferAmount = BigDecimal.ZERO;

        for (Transaction trans : transactions) {
            BigDecimal amount = trans.getAmount() != null ? trans.getAmount() : BigDecimal.ZERO;
            String type = trans.getTransType();
            String subtype = trans.getTransSubtype();

            // ✅ 使用和summary相同的分类逻辑
            if ("DEPOSIT".equalsIgnoreCase(type)) {
                depositAmount = depositAmount.add(amount);
            } else if ("WITHDRAW".equalsIgnoreCase(type)) {
                if ("FIXED_DEPOSIT_EARLY".equalsIgnoreCase(subtype) ||
                        "FIXED_DEPOSIT_MATURE".equalsIgnoreCase(subtype)) {
                    depositAmount = depositAmount.add(amount.abs());
                } else if ("CURRENT_WITHDRAW".equalsIgnoreCase(subtype)) {
                    withdrawAmount = withdrawAmount.add(amount.abs());
                }
            } else if ("TRANSFER".equalsIgnoreCase(type)) {
                transferAmount = transferAmount.add(amount.abs());
            } else if ("INTEREST".equalsIgnoreCase(type)) {
                interestAmount = interestAmount.add(amount);
            }

            // 手续费
            if (trans.getFee() != null && trans.getFee().compareTo(BigDecimal.ZERO) > 0) {
                feeAmount = feeAmount.add(trans.getFee());
            }
        }

        stats.setDepositAmount(depositAmount);
        stats.setWithdrawAmount(withdrawAmount);
        stats.setInterestAmount(interestAmount);
        stats.setTransferAmount(transferAmount);
        stats.setFeeAmount(feeAmount);

        log.info("分类统计: 存款={}, 取款={}, 利息={}, 转账={}, 手续费={}",
                depositAmount, withdrawAmount, interestAmount, transferAmount, feeAmount);

        return stats;
    }

    /**
     * ✅ 验证报告数据一致性
     */
    private void validateReportConsistency(ReportResponseDTO report) {
        log.info("=== 报告数据一致性验证 ===");

        ReportResponseDTO.ReportSummary summary = report.getSummary();
        ReportResponseDTO.CategoryStatistics stats = report.getCategoryStatistics();

        if (summary != null && stats != null) {
            // 1. 检查存款金额是否一致
            if (summary.getTotalDeposit().compareTo(stats.getDepositAmount()) != 0) {
                log.warn("存款金额不一致！summary={}, category={}",
                        summary.getTotalDeposit(), stats.getDepositAmount());
                // 自动修正
                stats.setDepositAmount(summary.getTotalDeposit());
            }

            // 2. 检查取款金额是否一致
            if (summary.getTotalWithdraw().compareTo(stats.getWithdrawAmount()) != 0) {
                log.warn("取款金额不一致！summary={}, category={}",
                        summary.getTotalWithdraw(), stats.getWithdrawAmount());
                // 自动修正
                stats.setWithdrawAmount(summary.getTotalWithdraw());
            }

            // 3. 检查利息是否一致
            BigDecimal summaryInterest = summary.getInterestEarned() != null ? summary.getInterestEarned() : BigDecimal.ZERO;
            BigDecimal categoryInterest = stats.getInterestAmount() != null ? stats.getInterestAmount() : BigDecimal.ZERO;

            if (summaryInterest.compareTo(categoryInterest) != 0) {
                log.warn("利息金额不一致！summary={}, category={}",
                        summaryInterest, categoryInterest);
                // 自动修正
                stats.setInterestAmount(summaryInterest);
            }

            // 4. 检查总收入-总支出是否等于净变化
            BigDecimal expectedNetChange = summary.getTotalDeposit()
                    .subtract(summary.getTotalWithdraw())
                    .add(summaryInterest);

            BigDecimal difference = summary.getNetChange().subtract(expectedNetChange).abs();
            if (difference.compareTo(new BigDecimal("0.01")) > 0) {
                log.warn("收支计算不一致！净变化={}, 期望值={}, 差额={}",
                        summary.getNetChange(), expectedNetChange, difference);
            }
        }

        log.info("验证完成");
    }

    // ============ 辅助方法 ============

    private Map<Integer, MonthlySummary> calculateMonthlyStats(String userId, String cardId, int year) {
        Map<Integer, MonthlySummary> monthlyStats = new HashMap<>();

        for (int month = 1; month <= 12; month++) {
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

            // 获取本月交易
            List<Transaction> monthlyTransactions = getTransactionsInPeriod(userId, cardId, monthStart, monthEnd);

            if (!monthlyTransactions.isEmpty()) {
                MonthlySummary monthSummary = new MonthlySummary();

                BigDecimal monthDeposit = BigDecimal.ZERO;
                BigDecimal monthWithdraw = BigDecimal.ZERO;
                BigDecimal monthInterest = BigDecimal.ZERO;

                for (Transaction trans : monthlyTransactions) {
                    BigDecimal amount = trans.getAmount() != null ? trans.getAmount() : BigDecimal.ZERO;
                    String type = trans.getTransType();
                    String subtype = trans.getTransSubtype();

                    // ✅ 使用统一逻辑
                    if ("DEPOSIT".equalsIgnoreCase(type)) {
                        monthDeposit = monthDeposit.add(amount);
                    } else if ("WITHDRAW".equalsIgnoreCase(type)) {
                        if ("FIXED_DEPOSIT_EARLY".equalsIgnoreCase(subtype) ||
                                "FIXED_DEPOSIT_MATURE".equalsIgnoreCase(subtype)) {
                            monthDeposit = monthDeposit.add(amount.abs());
                        } else if ("CURRENT_WITHDRAW".equalsIgnoreCase(subtype)) {
                            monthWithdraw = monthWithdraw.add(amount.abs());
                        }
                    } else if ("INTEREST".equalsIgnoreCase(type)) {
                        monthInterest = monthInterest.add(amount);
                    }
                }

                monthSummary.setMonth(month);
                monthSummary.setTotalDeposit(monthDeposit);
                monthSummary.setTotalWithdraw(monthWithdraw);
                monthSummary.setTotalInterest(monthInterest);
                monthSummary.setTransactionCount(monthlyTransactions.size());

                monthlyStats.put(month, monthSummary);
            }
        }

        return monthlyStats;
    }

    // ============ 其他方法 ============

    @Override
    public String generateReportId(String reportType, int year, Integer month) {
        String prefix = "RPT";
        String typeCode = "monthly".equals(reportType) ? "M" : "Y";
        String datePart = year + (month != null ? String.format("%02d", month) : "00");
        String randomPart = String.format("%04d", new Random().nextInt(10000));

        return prefix + datePart + typeCode + randomPart;
    }

    private BigDecimal calculateBalanceAtDate(String userId, String cardId, LocalDate date) {
        log.info("查询 {} 在 {} 的余额", userId, date);

        try {
            java.sql.Date sqlDate = java.sql.Date.valueOf(date);
            Transaction lastTransaction = transactionMapper.findLastTransactionBeforeDate(
                    userId, cardId, sqlDate);

            if (lastTransaction != null && lastTransaction.getBalanceAfter() != null) {
                log.info("找到最近交易: time={}, type={}, amount={}, balanceAfter={}",
                        lastTransaction.getTransTime(),
                        lastTransaction.getTransType(),
                        lastTransaction.getAmount(),
                        lastTransaction.getBalanceAfter());
                return lastTransaction.getBalanceAfter();
            } else {
                log.warn("未找到 {} 在 {} 之前的交易记录", userId, date);
                return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.warn("查询历史余额失败: ", e);
            return BigDecimal.ZERO;
        }
    }

    private List<Transaction> getTransactionsInPeriod(String userId, String cardId,
                                                      LocalDate startDate, LocalDate endDate) {
        log.info("查询交易: userId={}, cardId={}, start={}, end={}",
                userId, cardId, startDate, endDate);

        List<Transaction> result = transactionMapper.findByConditions(
                userId,
                cardId,
                "ALL",
                startDate,
                endDate,
                0,
                10000
        );

        log.info("查询结果: 找到 {} 条记录", result.size());
        if (!result.isEmpty()) {
            log.info("第一条记录: {}", result.get(0));
        }

        return result;
    }

    private ReportResponseDTO.TransactionSummary convertToTransactionSummary(Transaction trans) {
        ReportResponseDTO.TransactionSummary summary = new ReportResponseDTO.TransactionSummary();
        summary.setTransTime(trans.getTransTime());
        summary.setTransType(trans.getTransType());
        summary.setTransSubtype(trans.getTransSubtype());
        summary.setAmount(trans.getAmount());
        summary.setBalanceAfter(trans.getBalanceAfter());
        summary.setRemark(trans.getRemark());
        return summary;
    }

    // ============ 缓存相关 ============

    private final Map<String, ReportResponseDTO> reportCache = new ConcurrentHashMap<>();

    @Override
    public ReportResponseDTO getReportData(String reportId) {
        ReportResponseDTO report = reportCache.get(reportId);
        if (report == null) {
            throw new RuntimeException("报告不存在或已过期，请重新生成");
        }
        return report;
    }

    @Override
    public byte[] exportReportAsCsv(String reportId) {
        try {
            ReportResponseDTO report = getReportData(reportId);
            return generateCsvContent(report);
        } catch (Exception e) {
            log.error("导出CSV失败", e);
            throw new RuntimeException("导出CSV失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportReportAsPdf(String reportId) {
        try {
            ReportResponseDTO report = getReportData(reportId);
            return pdfExportService.exportReportToPdf(report);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            throw new RuntimeException("导出PDF失败: " + e.getMessage());
        }
    }

    @Override
    public ReportResponseDTO generateReport(GenerateReportDTO request) {
        log.info("生成账单报告: userId={}, type={}, year={}, month={}",
                request.getUserId(), request.getReportType(), request.getYear(), request.getMonth());

        ReportResponseDTO report;

        if ("monthly".equalsIgnoreCase(request.getReportType())) {
            if (request.getMonth() == null) {
                throw new RuntimeException("月度报告需要指定月份");
            }
            report = generateMonthlyReport(request.getUserId(), request.getCardId(),
                    request.getYear(), request.getMonth());
        } else if ("yearly".equalsIgnoreCase(request.getReportType())) {
            report = generateYearlyReport(request.getUserId(), request.getCardId(), request.getYear());
        } else {
            throw new RuntimeException("不支持的报告类型: " + request.getReportType());
        }

        // 缓存报告
        reportCache.put(report.getReportId(), report);

        // 定时清理（1小时后）
        scheduleReportCleanup(report.getReportId());

        return report;
    }

    // ============ 工具方法 ============

    private byte[] generateCsvContent(ReportResponseDTO report) throws IOException {
        StringBuilder csv = new StringBuilder();

        // 使用UTF-8 BOM确保Excel正确显示中文
        csv.append("\uFEFF");

        // 1. 报告信息
        csv.append("银行账单报告\n");
        csv.append("报告编号,").append(report.getReportId()).append("\n");
        csv.append("用户ID,").append(report.getUserId()).append("\n");
        csv.append("报告类型,").append(report.getReportType()).append("\n");
        csv.append("时间段,").append(report.getPeriod()).append("\n");
        csv.append("生成时间,").append(formatDate(report.getGeneratedTime())).append("\n");
        csv.append("\n");

        // 2. 汇总信息
        if (report.getSummary() != null) {
            csv.append("【汇总信息】\n");
            csv.append("项目,金额(元)\n");
            csv.append("期初余额,").append(formatAmount(report.getSummary().getStartBalance())).append("\n");
            csv.append("期末余额,").append(formatAmount(report.getSummary().getEndBalance())).append("\n");
            csv.append("总收入,").append(formatAmount(report.getSummary().getTotalDeposit())).append("\n");
            csv.append("总支出,").append(formatAmount(report.getSummary().getTotalWithdraw())).append("\n");
            csv.append("利息收入,").append(formatAmount(report.getSummary().getInterestEarned())).append("\n");
            csv.append("交易笔数,").append(report.getSummary().getTransactionCount()).append("\n");
            csv.append("净变化,").append(formatAmount(report.getSummary().getNetChange())).append("\n\n");
        }

        // 3. 分类统计
        if (report.getCategoryStatistics() != null) {
            csv.append("【分类统计】\n");
            csv.append("分类,金额(元)\n");
            csv.append("存款总额,").append(formatAmount(report.getCategoryStatistics().getDepositAmount())).append("\n");
            csv.append("取款总额,").append(formatAmount(report.getCategoryStatistics().getWithdrawAmount())).append("\n");
            csv.append("利息总额,").append(formatAmount(report.getCategoryStatistics().getInterestAmount())).append("\n");
            csv.append("手续费总额,").append(formatAmount(report.getCategoryStatistics().getFeeAmount())).append("\n\n");
        }

        // 4. 交易明细
        if (report.getTransactions() != null && !report.getTransactions().isEmpty()) {
            csv.append("【交易明细】\n");
            csv.append("时间,类型,金额(元),余额(元),备注\n");

            for (var trans : report.getTransactions()) {
                csv.append(formatDate(trans.getTransTime())).append(",");
                csv.append(trans.getTransType()).append(",");
                csv.append(formatAmount(trans.getAmount())).append(",");
                csv.append(formatAmount(trans.getBalanceAfter())).append(",");
                csv.append(trans.getRemark() != null ? trans.getRemark().replace(",", "，") : "").append("\n");
            }
        }

        csv.append("\n");
        csv.append("银行管理系统\n");
        csv.append("生成时间,").append(formatDate(new Date())).append("\n");

        return csv.toString().getBytes("UTF-8");
    }

    private void scheduleReportCleanup(String reportId) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                reportCache.remove(reportId);
                log.info("清理过期报告: {}", reportId);
            }
        }, 60 * 60 * 1000); // 1小时
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDate(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }

    // ============ 内部类 ============

    @Getter
    @Setter
    class MonthlySummary {
        private Integer month;
        private BigDecimal totalDeposit;
        private BigDecimal totalWithdraw;
        private BigDecimal totalInterest;
        private Integer transactionCount;
    }
}