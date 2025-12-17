package com.zychen.bank.service;


import com.zychen.bank.dto.GenerateReportDTO;
import com.zychen.bank.dto.ReportResponseDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.mapper.FixedDepositMapper;
import com.zychen.bank.mapper.TransactionMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.model.Transaction;
import com.zychen.bank.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
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
//    @Override
//    public ReportResponseDTO generateReport(GenerateReportDTO request) {
//        log.info("生成账单报告: userId={}, type={}, year={}, month={}",
//                request.getUserId(), request.getReportType(), request.getYear(), request.getMonth());
//
//        if ("monthly".equalsIgnoreCase(request.getReportType())) {
//            if (request.getMonth() == null) {
//                throw new RuntimeException("月度报告需要指定月份");
//            }
//            return generateMonthlyReport(request.getUserId(), request.getCardId(),
//                    request.getYear(), request.getMonth());
//        } else if ("yearly".equalsIgnoreCase(request.getReportType())) {
//            return generateYearlyReport(request.getUserId(), request.getCardId(), request.getYear());
//        } else {
//            throw new RuntimeException("不支持的报告类型: " + request.getReportType());
//        }
//    }

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

        // 6. 计算统计信息
        ReportResponseDTO.ReportSummary summary = calculateSummary(transactions, startBalance, endBalance);
        response.setSummary(summary);

        // 7. 计算分类统计
        ReportResponseDTO.CategoryStatistics categoryStats = calculateCategoryStatistics(transactions);
        response.setCategoryStatistics(categoryStats);

        // 8. 添加交易明细（简化版，只取前20条）
        if (transactions.size() <= 20) {
            List<ReportResponseDTO.TransactionSummary> transactionSummaries = transactions.stream()
                    .map(this::convertToTransactionSummary)
                    .collect(Collectors.toList());
            response.setTransactions(transactionSummaries);
        }

        // 9. 设置下载URL（暂为空）
        response.setDownloadUrl("/api/reports/" + response.getReportId() + "/download");

        return response;
    }

    @Override
    public ReportResponseDTO generateYearlyReport(String userId, String cardId, int year) {
        // 类似月度报告，按年统计
        ReportResponseDTO response = new ReportResponseDTO();

        response.setUserId(userId);
        response.setCardId(cardId);
        response.setReportType("yearly");
        response.setPeriod(year + "年度");
        response.setGeneratedTime(new Date());
        response.setReportId(generateReportId("yearly", year, null));

        // 计算年度统计（可以用月度报告聚合）
        // 这里先返回简单版本
        response.setSummary(new ReportResponseDTO.ReportSummary());

        return response;
    }

    @Override
    public String generateReportId(String reportType, int year, Integer month) {
        String prefix = "RPT";
        String typeCode = "monthly".equals(reportType) ? "M" : "Y";
        String datePart = year + (month != null ? String.format("%02d", month) : "00");
        String randomPart = String.format("%04d", new Random().nextInt(10000));

        return prefix + datePart + typeCode + randomPart;
    }

    // 辅助方法
    private BigDecimal calculateBalanceAtDate(String userId, String cardId, LocalDate date) {
        // 简化实现：查询该日期前的最后一笔交易余额
        // 实际应该查询历史余额快照，这里用简化版本
        try {
            java.sql.Date sqlDate = java.sql.Date.valueOf(date);
            Transaction lastTransaction= transactionMapper.findLastTransactionBeforeDate(
                    userId, cardId, sqlDate);
            if (lastTransaction != null && lastTransaction.getBalanceAfter() != null) {
                return lastTransaction.getBalanceAfter();
            }
        } catch (Exception e) {
            log.warn("查询历史余额失败，使用0: ", e);
        }
        return BigDecimal.ZERO;
    }

    private List<Transaction> getTransactionsInPeriod(String userId, String cardId,
                                                      LocalDate startDate, LocalDate endDate) {
        // 使用你已有的查询方法
        return transactionMapper.findByConditions(
                userId,
                cardId,
                "ALL",  // 所有类型
                startDate,
                endDate,
                0,      // offset
                10000   // 取足够多的记录
        );
    }

    private ReportResponseDTO.ReportSummary calculateSummary(List<Transaction> transactions,
                                                             BigDecimal startBalance, BigDecimal endBalance) {
        ReportResponseDTO.ReportSummary summary = new ReportResponseDTO.ReportSummary();

        summary.setStartBalance(startBalance);
        summary.setEndBalance(endBalance);
        summary.setNetChange(endBalance.subtract(startBalance));

        // 计算各种类型的总额
        BigDecimal totalDeposit = BigDecimal.ZERO;
        BigDecimal totalWithdraw = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;

        for (Transaction trans : transactions) {
            BigDecimal amount = trans.getAmount() != null ? trans.getAmount() : BigDecimal.ZERO;
            String type = trans.getTransType();

            if ("DEPOSIT".equalsIgnoreCase(type)) {
                totalDeposit = totalDeposit.add(amount);
            } else if ("WITHDRAW".equalsIgnoreCase(type)) {
                totalWithdraw = totalWithdraw.add(amount);
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

    private ReportResponseDTO.CategoryStatistics calculateCategoryStatistics(List<Transaction> transactions) {
        ReportResponseDTO.CategoryStatistics stats = new ReportResponseDTO.CategoryStatistics();

        BigDecimal depositAmount = BigDecimal.ZERO;
        BigDecimal withdrawAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;
        BigDecimal feeAmount = BigDecimal.ZERO;

        for (Transaction trans : transactions) {
            BigDecimal amount = trans.getAmount() != null ? trans.getAmount() : BigDecimal.ZERO;
            String type = trans.getTransType();

            if ("DEPOSIT".equalsIgnoreCase(type)) {
                depositAmount = depositAmount.add(amount);
            } else if ("WITHDRAW".equalsIgnoreCase(type)) {
                withdrawAmount = withdrawAmount.add(amount);
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
        stats.setFeeAmount(feeAmount);

        return stats;
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

// 在 ReportServiceImpl.java 的末尾，在最后一个方法后添加：

    // 添加缓存
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
            // 这里调用PdfExportService
            // 需要先在类中注入PdfExportService
            return pdfExportService.exportReportToPdf(report);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            throw new RuntimeException("导出PDF失败: " + e.getMessage());
        }
    }

    // 在generateReport方法中缓存报告（修改现有的generateReport方法）
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
    // 添加新的 formatDate 方法处理 LocalDateTime
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    // 生成CSV内容
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

    // 定时清理报告缓存
    private void scheduleReportCleanup(String reportId) {
        // 简化：1小时后清理
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                reportCache.remove(reportId);
                log.info("清理过期报告: {}", reportId);
            }
        }, 60 * 60 * 1000); // 1小时
    }

    // 辅助方法
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }

    private String formatDate(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}