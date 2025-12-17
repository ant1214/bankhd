package com.zychen.bank.service;


import com.zychen.bank.dto.GenerateReportDTO;
import com.zychen.bank.dto.ReportResponseDTO;

public interface ReportService {

    /**
     * 生成账单报告
     */
    ReportResponseDTO generateReport(GenerateReportDTO request);

    /**
     * 根据用户ID和年月生成月度报告
     */
    ReportResponseDTO generateMonthlyReport(String userId, String cardId, int year, int month);

    /**
     * 根据用户ID和年份生成年度报告
     */
    ReportResponseDTO generateYearlyReport(String userId, String cardId, int year);

    /**
     * 生成报告ID
     */
    String generateReportId(String reportType, int year, Integer month);


    /**
     * 获取报告数据（用于下载）
     */
    ReportResponseDTO getReportData(String reportId);

    /**
     * 导出报告为CSV文件
     */
    byte[] exportReportAsCsv(String reportId);

    /**
     * 导出报告为PDF文件
     */
    byte[] exportReportAsPdf(String reportId);

}
