//
//
//package com.zychen.bank.service;
//
//import com.zychen.bank.dto.ReportResponseDTO;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.pdmodel.PDPage;
//import org.apache.pdfbox.pdmodel.PDPageContentStream;
//import org.apache.pdfbox.pdmodel.common.PDRectangle;
//import org.apache.pdfbox.pdmodel.font.PDFont;
//import org.apache.pdfbox.pdmodel.font.PDType0Font;
//import org.apache.pdfbox.pdmodel.font.PDType1Font;
//import org.springframework.stereotype.Service;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.text.DecimalFormat;
//import java.text.SimpleDateFormat;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Date;
//
//@Slf4j
//@Service
//public class PdfExportService {
//
//    // 页面设置
//    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
//    private static final float MARGIN = 50;
//    private static final float LINE_HEIGHT = 20;
//    private static final float TITLE_FONT_SIZE = 18;
//    private static final float HEADER_FONT_SIZE = 14;
//    private static final float NORMAL_FONT_SIZE = 12;
//
//    // 字体变量（动态加载）
//    private PDFont chineseTitleFont;
//    private PDFont chineseHeaderFont;
//    private PDFont chineseNormalFont;
//
//    // 默认英文字体（备用）
//    private static final PDFont ENGLISH_TITLE_FONT = PDType1Font.HELVETICA_BOLD;
//    private static final PDFont ENGLISH_HEADER_FONT = PDType1Font.HELVETICA_BOLD;
//    private static final PDFont ENGLISH_NORMAL_FONT = PDType1Font.HELVETICA;
//
//    // 格式化
//    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
//    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
//
//    /**
//     * 加载中文字体
//     */
//    private void loadChineseFonts(PDDocument document) throws IOException {
//        try {
//            // 1. 尝试加载系统字体
//            String[] fontPaths = {
//                    "C:/Windows/Fonts/simhei.ttf",           // Windows 黑体
//                    "C:/Windows/Fonts/simsun.ttc",           // Windows 宋体
//                    "C:/Windows/Fonts/msyh.ttc",             // Windows 微软雅黑
//                    "/System/Library/Fonts/PingFang.ttc",    // Mac 苹方
//                    "/System/Library/Fonts/STHeiti Light.ttc", // Mac 黑体
//                    "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc" // Linux 文泉驿
//            };
//
//            File fontFile = null;
//            for (String path : fontPaths) {
//                File f = new File(path);
//                if (f.exists()) {
//                    fontFile = f;
//                    log.info("找到系统字体: {}", path);
//                    break;
//                }
//            }
//
//            if (fontFile != null) {
//                // 加载中文字体
//                chineseTitleFont = PDType0Font.load(document, fontFile);
//                chineseHeaderFont = chineseTitleFont;
//                chineseNormalFont = chineseTitleFont;
//                log.info("成功加载系统字体");
//                return;
//            }
//
//            // 2. 如果系统字体不存在，使用默认英文字体
//            log.warn("未找到系统字体，使用默认英文字体（中文将显示为方块）");
//            chineseTitleFont = ENGLISH_TITLE_FONT;
//            chineseHeaderFont = ENGLISH_HEADER_FONT;
//            chineseNormalFont = ENGLISH_NORMAL_FONT;
//
//        } catch (Exception e) {
//            log.error("加载字体失败，使用默认字体", e);
//            chineseTitleFont = ENGLISH_TITLE_FONT;
//            chineseHeaderFont = ENGLISH_HEADER_FONT;
//            chineseNormalFont = ENGLISH_NORMAL_FONT;
//        }
//    }
//
//    /**
//     * 安全显示文本（防止字体不支持字符）
//     */
//    private void safeShowText(PDPageContentStream contentStream, String text) throws IOException {
//        try {
//            contentStream.showText(text);
//        } catch (IllegalArgumentException e) {
//            // 如果字体不支持某些字符，替换为问号
//            log.warn("字体不支持字符: {}, 替换为?", text);
//            contentStream.showText("?");
//        }
//    }
//
//    /**
//     * 导出报告为PDF
//     */
//    public byte[] exportReportToPdf(ReportResponseDTO report) throws IOException {
//        try (PDDocument document = new PDDocument();
//             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//
//            // 加载字体
//            loadChineseFonts(document);
//
//            // 创建页面
//            PDPage page = new PDPage(PAGE_SIZE);
//            document.addPage(page);
//
//            // 创建内容流
//            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
//                // 当前Y坐标（从页面顶部开始）
//                float y = PAGE_SIZE.getHeight() - MARGIN;
//
//                // 1. 添加标题
//                y = addTitle(contentStream, "银行账单报告", y);
//                y -= LINE_HEIGHT * 0.5f;
//
//                // 2. 添加报告基本信息
//                y = addReportInfo(contentStream, report, y);
//                y -= LINE_HEIGHT * 0.5f;
//
//                // 3. 添加汇总信息
//                y = addSummary(contentStream, report.getSummary(), y);
//                y -= LINE_HEIGHT * 0.5f;
//
//                // 4. 添加分类统计
//                y = addCategoryStatistics(contentStream, report.getCategoryStatistics(), y);
//                y -= LINE_HEIGHT * 0.5f;
//
//                // 5. 添加交易明细（如果有）
//                if (report.getTransactions() != null && !report.getTransactions().isEmpty()) {
//                    y = addTransactionDetails(contentStream, report.getTransactions(), y);
//                }
//
//                // 6. 添加页脚
//                addFooter(contentStream, report);
//            }
//
//            // 保存文档到字节数组
//            document.save(baos);
//            log.info("PDF导出成功，报告ID: {}", report.getReportId());
//
//            return baos.toByteArray();
//
//        } catch (Exception e) {
//            log.error("PDF导出失败", e);
//            throw new IOException("生成PDF失败: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 添加标题
//     */
//    private float addTitle(PDPageContentStream contentStream, String title, float y) throws IOException {
//        contentStream.beginText();
//        contentStream.setFont(chineseTitleFont, TITLE_FONT_SIZE);
//        contentStream.newLineAtOffset(MARGIN, y);
//        safeShowText(contentStream, title);
//        contentStream.endText();
//        return y - LINE_HEIGHT * 1.5f;
//    }
//
//    /**
//     * 添加报告基本信息
//     */
//    private float addReportInfo(PDPageContentStream contentStream, ReportResponseDTO report, float y) throws IOException {
//        contentStream.beginText();
//        contentStream.setFont(chineseHeaderFont, HEADER_FONT_SIZE);
//        contentStream.newLineAtOffset(MARGIN, y);
//        safeShowText(contentStream, "报告信息");
//        contentStream.endText();
//        y -= LINE_HEIGHT;
//
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
//        contentStream.newLineAtOffset(MARGIN + 20, y);
//        safeShowText(contentStream, "报告编号: " + report.getReportId());
//        contentStream.endText();
//        y -= LINE_HEIGHT;
//
//        contentStream.beginText();
//        contentStream.newLineAtOffset(MARGIN + 20, y);
//        safeShowText(contentStream, "用户ID: " + report.getUserId());
//        contentStream.endText();
//        y -= LINE_HEIGHT;
//
//        contentStream.beginText();
//        contentStream.newLineAtOffset(MARGIN + 20, y);
//        safeShowText(contentStream, "报告类型: " + (report.getReportType().equals("monthly") ? "月度账单" : "年度账单"));
//        contentStream.endText();
//        y -= LINE_HEIGHT;
//
//        contentStream.beginText();
//        contentStream.newLineAtOffset(MARGIN + 20, y);
//        safeShowText(contentStream, "时间段: " + report.getPeriod());
//        contentStream.endText();
//        y -= LINE_HEIGHT;
//
//        contentStream.beginText();
//        contentStream.newLineAtOffset(MARGIN + 20, y);
//        safeShowText(contentStream, "生成时间: " + DATE_FORMAT.format(report.getGeneratedTime()));
//        contentStream.endText();
//
//        return y - LINE_HEIGHT;
//    }
//
//    /**
//     * 添加汇总信息
//     */
//    private float addSummary(PDPageContentStream contentStream,
//                             ReportResponseDTO.ReportSummary summary, float y) throws IOException {
//        if (summary == null) return y;
//
//        contentStream.beginText();
//        contentStream.setFont(chineseHeaderFont, HEADER_FONT_SIZE);
//        contentStream.newLineAtOffset(MARGIN, y);
//        safeShowText(contentStream, "汇总信息");
//        contentStream.endText();
//        y -= LINE_HEIGHT;
//
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
//
//        float x1 = MARGIN + 20;
//        float x2 = MARGIN + 200;
//
//        // 第一列
//        contentStream.newLineAtOffset(x1, y);
//        safeShowText(contentStream, "期初余额:");
//        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT);
//        safeShowText(contentStream, "期末余额:");
//        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 2);
//        safeShowText(contentStream, "总收入:");
//        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 3);
//        safeShowText(contentStream, "总支出:");
//        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 4);
//        safeShowText(contentStream, "利息收入:");
//        contentStream.endText();
//
//        // 第二列
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
//        contentStream.newLineAtOffset(x2, y);
//        safeShowText(contentStream, formatAmount(summary.getStartBalance()) + " 元");
//        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT);
//        safeShowText(contentStream, formatAmount(summary.getEndBalance()) + " 元");
//        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 2);
//        safeShowText(contentStream, formatAmount(summary.getTotalDeposit()) + " 元");
//        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 3);
//        safeShowText(contentStream, formatAmount(summary.getTotalWithdraw()) + " 元");
//        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 4);
//        safeShowText(contentStream, formatAmount(summary.getInterestEarned()) + " 元");
//        contentStream.endText();
//
//        // 第三列（如果需要继续）
//        float x3 = MARGIN + 350;
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
//        contentStream.newLineAtOffset(x3, y);
//        safeShowText(contentStream, "交易笔数:");
//        contentStream.newLineAtOffset(x3, y - LINE_HEIGHT);
//        safeShowText(contentStream, "净变化:");
//        contentStream.endText();
//
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
//        contentStream.newLineAtOffset(x3 + 80, y);
//        safeShowText(contentStream, String.valueOf(summary.getTransactionCount()));
//        contentStream.newLineAtOffset(x3 + 80, y - LINE_HEIGHT);
//        safeShowText(contentStream, formatAmount(summary.getNetChange()) + " 元");
//        contentStream.endText();
//
//        return y - LINE_HEIGHT * 6;
//    }
//
//    /**
//     * 添加分类统计
//     */
//    private float addCategoryStatistics(PDPageContentStream contentStream,
//                                        ReportResponseDTO.CategoryStatistics stats, float y) throws IOException {
//        if (stats == null) return y;
//
//        contentStream.beginText();
//        contentStream.setFont(chineseHeaderFont, HEADER_FONT_SIZE);
//        contentStream.newLineAtOffset(MARGIN, y);
//        safeShowText(contentStream, "分类统计");
//        contentStream.endText();
//        y -= LINE_HEIGHT;
//
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
//
//        float x1 = MARGIN + 20;
//        float x2 = MARGIN + 200;
//
//        contentStream.newLineAtOffset(x1, y);
//        safeShowText(contentStream, "存款总额:");
//        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT);
//        safeShowText(contentStream, "取款总额:");
//        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 2);
//        safeShowText(contentStream, "利息总额:");
//        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 3);
//        safeShowText(contentStream, "手续费总额:");
//        contentStream.endText();
//
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
//        contentStream.newLineAtOffset(x2, y);
//        safeShowText(contentStream, formatAmount(stats.getDepositAmount()) + " 元");
//        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT);
//        safeShowText(contentStream, formatAmount(stats.getWithdrawAmount()) + " 元");
//        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 2);
//        safeShowText(contentStream, formatAmount(stats.getInterestAmount()) + " 元");
//        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 3);
//        safeShowText(contentStream, formatAmount(stats.getFeeAmount()) + " 元");
//        contentStream.endText();
//
//        return y - LINE_HEIGHT * 5;
//    }
//
//    /**
//     * 添加交易明细 - 修复对齐版本
//     */
//    private float addTransactionDetails(PDPageContentStream contentStream,
//                                        java.util.List<ReportResponseDTO.TransactionSummary> transactions,
//                                        float y) throws IOException {
//        // 检查页面空间是否足够
//        if (y < MARGIN + 100) {
//            log.warn("页面空间不足，交易明细可能被截断");
//        }
//
//        contentStream.beginText();
//        contentStream.setFont(chineseHeaderFont, HEADER_FONT_SIZE);
//        contentStream.newLineAtOffset(MARGIN, y);
//        safeShowText(contentStream, "交易明细（最近" + Math.min(transactions.size(), 10) + "条）");
//        contentStream.endText();
//        y -= LINE_HEIGHT * 1.5f;
//
//        // 添加表头
//        float[] columnPositions = {MARGIN + 10, MARGIN + 120, MARGIN + 180, MARGIN + 250, MARGIN + 350};
//        String[] headers = {"时间", "类型", "金额 (元)", "余额 (元)", "备注"};
//
//        // 绘制表头（每列单独绘制，确保对齐）
//        for (int i = 0; i < headers.length; i++) {
//            contentStream.beginText();
//            contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE - 2);
//            contentStream.newLineAtOffset(columnPositions[i], y);
//            safeShowText(contentStream, headers[i]);
//            contentStream.endText();
//        }
//
//        // 绘制表头下划线
//        contentStream.setLineWidth(0.5f);
//        contentStream.moveTo(MARGIN, y - 5);
//        contentStream.lineTo(PAGE_SIZE.getWidth() - MARGIN, y - 5);
//        contentStream.stroke();
//
//        y -= LINE_HEIGHT * 1.2f;
//
//        // 添加交易记录（最多10条）
//        int maxRecords = Math.min(transactions.size(), 10);
//        for (int i = 0; i < maxRecords; i++) {
//            var trans = transactions.get(i);
//
//            // 时间
//            contentStream.beginText();
//            contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE - 2);
//            contentStream.newLineAtOffset(columnPositions[0], y);
//            safeShowText(contentStream, formatShortDate(trans.getTransTime()));
//            contentStream.endText();
//
//            // 类型
//            contentStream.beginText();
//            contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE - 2);
//            contentStream.newLineAtOffset(columnPositions[1], y);
//            safeShowText(contentStream, getTransTypeChinese(trans.getTransType()));
//            contentStream.endText();
//
//            // 金额
//            contentStream.beginText();
//            contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE - 2);
//            contentStream.newLineAtOffset(columnPositions[2], y);
//            safeShowText(contentStream, formatAmount(trans.getAmount()));
//            contentStream.endText();
//
//            // 余额
//            contentStream.beginText();
//            contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE - 2);
//            contentStream.newLineAtOffset(columnPositions[3], y);
//            safeShowText(contentStream, formatAmount(trans.getBalanceAfter()));
//            contentStream.endText();
//
//            // 备注（截断过长的）
//            String remark = trans.getRemark() != null ? trans.getRemark() : "";
//            if (remark.length() > 20) {
//                remark = remark.substring(0, 20) + "...";
//            }
//            contentStream.beginText();
//            contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE - 2);
//            contentStream.newLineAtOffset(columnPositions[4], y);
//            safeShowText(contentStream, remark);
//            contentStream.endText();
//
//            y -= LINE_HEIGHT;
//
//            // 检查是否需要换页（简化处理）
//            if (y < MARGIN + 50 && i < maxRecords - 1) {
//                contentStream.beginText();
//                contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE - 2);
//                contentStream.newLineAtOffset(MARGIN, y);
//                safeShowText(contentStream, "... 更多交易记录请查看完整报告");
//                contentStream.endText();
//                y -= LINE_HEIGHT;
//                break;
//            }
//        }
//
//        return y - LINE_HEIGHT;
//    }
//
//    /**
//     * 添加页脚
//     */
//    private void addFooter(PDPageContentStream contentStream, ReportResponseDTO report) throws IOException {
//        float footerY = MARGIN - 20;
//
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, 10);
//        contentStream.newLineAtOffset(MARGIN, footerY);
//        safeShowText(contentStream, "银行管理系统 - 账单报告");
//        contentStream.endText();
//
//        contentStream.beginText();
//        contentStream.setFont(chineseNormalFont, 10);
//        contentStream.newLineAtOffset(PAGE_SIZE.getWidth() - MARGIN - 150, footerY);
//        safeShowText(contentStream, "生成时间: " + DATE_FORMAT.format(new Date()));
//        contentStream.endText();
//    }
//
//    // ============ 辅助方法 ============
//
//    private String formatAmount(java.math.BigDecimal amount) {
//        if (amount == null) return "0.00";
//        return AMOUNT_FORMAT.format(amount);
//    }
//
//    private String formatShortDate(Date date) {
//        if (date == null) return "";
//        return SIMPLE_DATE_FORMAT.format(date);
//    }
//
//    private String formatShortDate(LocalDateTime dateTime) {
//        if (dateTime == null) return "";
//        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//    }
//
//    private String getTransTypeChinese(String type) {
//        if (type == null) return "";
//        switch (type.toUpperCase()) {
//            case "DEPOSIT": return "存款";
//            case "WITHDRAW": return "取款";
//            case "INTEREST": return "利息";
//            case "TRANSFER": return "转账";
//            default: return type;
//        }
//    }
//}

package com.zychen.bank.service;

import com.zychen.bank.dto.ReportResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class PdfExportService {

    // 页面设置
    private static final PDRectangle PAGE_SIZE = PDRectangle.A4;
    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 20;
    private static final float TITLE_FONT_SIZE = 18;
    private static final float HEADER_FONT_SIZE = 14;
    private static final float NORMAL_FONT_SIZE = 12;
    private static final float SMALL_FONT_SIZE = 10;

    // 表格行高（固定）
    private static final float TABLE_ROW_HEIGHT = 22;

    // 字体变量（动态加载）
    private PDFont chineseTitleFont;
    private PDFont chineseHeaderFont;
    private PDFont chineseNormalFont;

    // 默认英文字体（备用）
    private static final PDFont ENGLISH_TITLE_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont ENGLISH_HEADER_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont ENGLISH_NORMAL_FONT = PDType1Font.HELVETICA;

    // 格式化
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 加载中文字体
     */
    private void loadChineseFonts(PDDocument document) throws IOException {
        try {
            // 1. 尝试加载系统字体
            String[] fontPaths = {
                    "C:/Windows/Fonts/simhei.ttf",           // Windows 黑体
                    "C:/Windows/Fonts/simsun.ttc",           // Windows 宋体
                    "C:/Windows/Fonts/msyh.ttc",             // Windows 微软雅黑
                    "/System/Library/Fonts/PingFang.ttc",    // Mac 苹方
                    "/System/Library/Fonts/STHeiti Light.ttc", // Mac 黑体
                    "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc" // Linux 文泉驿
            };

            File fontFile = null;
            for (String path : fontPaths) {
                File f = new File(path);
                if (f.exists()) {
                    fontFile = f;
                    log.info("找到系统字体: {}", path);
                    break;
                }
            }

            if (fontFile != null) {
                // 加载中文字体
                chineseTitleFont = PDType0Font.load(document, fontFile);
                chineseHeaderFont = chineseTitleFont;
                chineseNormalFont = chineseTitleFont;
                log.info("成功加载系统字体");
                return;
            }

            // 2. 如果系统字体不存在，使用默认英文字体
            log.warn("未找到系统字体，使用默认英文字体（中文将显示为方块）");
            chineseTitleFont = ENGLISH_TITLE_FONT;
            chineseHeaderFont = ENGLISH_HEADER_FONT;
            chineseNormalFont = ENGLISH_NORMAL_FONT;

        } catch (Exception e) {
            log.error("加载字体失败，使用默认字体", e);
            chineseTitleFont = ENGLISH_TITLE_FONT;
            chineseHeaderFont = ENGLISH_HEADER_FONT;
            chineseNormalFont = ENGLISH_NORMAL_FONT;
        }
    }

    /**
     * 安全显示文本（防止字体不支持字符）
     */
    private void safeShowText(PDPageContentStream contentStream, String text) throws IOException {
        try {
            contentStream.showText(text);
        } catch (IllegalArgumentException e) {
            // 如果字体不支持某些字符，替换为问号
            log.warn("字体不支持字符: {}, 替换为?", text);
            contentStream.showText("?");
        }
    }

    /**
     * 导出报告为PDF - 支持多页
     */
    public byte[] exportReportToPdf(ReportResponseDTO report) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 加载字体
            loadChineseFonts(document);

            // 获取交易记录
            List<ReportResponseDTO.TransactionSummary> allTransactions =
                    report.getTransactions() != null ? report.getTransactions() : new ArrayList<>();

            // 计算每页能显示的交易记录数量
            int transactionsPerPage = calculateTransactionsPerPage();

            // 将交易记录分页
            List<List<ReportResponseDTO.TransactionSummary>> transactionPages = new ArrayList<>();
            if (!allTransactions.isEmpty()) {
                for (int i = 0; i < allTransactions.size(); i += transactionsPerPage) {
                    int end = Math.min(i + transactionsPerPage, allTransactions.size());
                    transactionPages.add(allTransactions.subList(i, end));
                }
            } else {
                transactionPages.add(new ArrayList<>()); // 空页面
            }

            int totalPages = transactionPages.size();

            // 生成每一页
            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                List<ReportResponseDTO.TransactionSummary> pageTransactions =
                        transactionPages.get(pageNum - 1);

                // 创建新页面
                PDPage page = new PDPage(PAGE_SIZE);
                document.addPage(page);

                // 创建内容流
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    float y = PAGE_SIZE.getHeight() - MARGIN;

                    if (pageNum == 1) {
                        // 第一页：显示完整报告信息
                        y = addFirstPageContent(contentStream, report, y);
                    } else {
                        // 后续页：显示页眉
                        y = addPageHeader(contentStream, report, pageNum, totalPages, y);
                    }

                    // 添加当前页的交易明细
                    if (!pageTransactions.isEmpty()) {
                        String tableTitle = pageNum == 1 ?
                                "交易明细" :
                                String.format("交易明细（续 %d/%d）", pageNum, totalPages);

                        y = addTransactionDetailsTable(contentStream, pageTransactions, tableTitle, y);
                    }

                    // 添加页脚（带页码）
                    addPageFooter(contentStream, report, pageNum, totalPages, y);
                }
            }

            // 保存文档
            document.save(baos);
            log.info("PDF导出成功，共{}页，报告ID: {}", totalPages, report.getReportId());

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new IOException("生成PDF失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算每页能显示的交易记录数量
     */
    private int calculateTransactionsPerPage() {
        // 页面可用高度 = 总高度 - 上下边距 - 页眉页脚空间 - 标题空间
        float availableHeight = PAGE_SIZE.getHeight() - (2 * MARGIN) - 100;
        int linesPerPage = (int) (availableHeight / TABLE_ROW_HEIGHT);
        return Math.max(10, linesPerPage); // 至少显示10行
    }

    /**
     * 添加第一页内容
     */
    private float addFirstPageContent(PDPageContentStream contentStream,
                                      ReportResponseDTO report, float y) throws IOException {
        // 1. 标题
        y = addTitle(contentStream, "银行账单报告", y);
        y -= LINE_HEIGHT * 0.5f;

        // 2. 报告基本信息
        y = addReportInfo(contentStream, report, y);
        y -= LINE_HEIGHT * 0.5f;

        // 3. 汇总信息
        y = addSummary(contentStream, report.getSummary(), y);
        y -= LINE_HEIGHT * 0.5f;

        // 4. 分类统计
        if (report.getCategoryStatistics() != null) {
            y = addCategoryStatistics(contentStream, report.getCategoryStatistics(), y);
            y -= LINE_HEIGHT * 0.5f;
        }

        return y;
    }

    /**
     * 添加后续页的页眉
     */
    private float addPageHeader(PDPageContentStream contentStream,
                                ReportResponseDTO report,
                                int currentPage,
                                int totalPages,
                                float y) throws IOException {
        String header = String.format("银行账单报告 - 第 %d/%d 页", currentPage, totalPages);

        contentStream.beginText();
        contentStream.setFont(chineseTitleFont, TITLE_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, y);
        safeShowText(contentStream, header);
        contentStream.endText();

        y -= LINE_HEIGHT;

        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE - 2);
        contentStream.newLineAtOffset(MARGIN, y);
        safeShowText(contentStream, String.format("报告编号: %s | 用户: %s | %s",
                report.getReportId(), report.getUserId(), report.getPeriod()));
        contentStream.endText();

        return y - LINE_HEIGHT;
    }

    /**
     * 添加标题
     */
    private float addTitle(PDPageContentStream contentStream, String title, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(chineseTitleFont, TITLE_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, y);
        safeShowText(contentStream, title);
        contentStream.endText();
        return y - LINE_HEIGHT * 1.5f;
    }

    /**
     * 添加报告基本信息
     */
    private float addReportInfo(PDPageContentStream contentStream, ReportResponseDTO report, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(chineseHeaderFont, HEADER_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, y);
        safeShowText(contentStream, "报告信息");
        contentStream.endText();
        y -= LINE_HEIGHT;

        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN + 20, y);
        safeShowText(contentStream, "报告编号: " + report.getReportId());
        contentStream.endText();
        y -= LINE_HEIGHT;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, y);
        safeShowText(contentStream, "用户ID: " + report.getUserId());
        contentStream.endText();
        y -= LINE_HEIGHT;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, y);
        safeShowText(contentStream, "报告类型: " + (report.getReportType().equals("monthly") ? "月度账单" : "年度账单"));
        contentStream.endText();
        y -= LINE_HEIGHT;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, y);
        safeShowText(contentStream, "时间段: " + report.getPeriod());
        contentStream.endText();
        y -= LINE_HEIGHT;

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN + 20, y);
        safeShowText(contentStream, "生成时间: " + DATE_FORMAT.format(report.getGeneratedTime()));
        contentStream.endText();

        return y - LINE_HEIGHT;
    }

    /**
     * 添加汇总信息
     */
    private float addSummary(PDPageContentStream contentStream,
                             ReportResponseDTO.ReportSummary summary, float y) throws IOException {
        if (summary == null) return y;

        contentStream.beginText();
        contentStream.setFont(chineseHeaderFont, HEADER_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, y);
        safeShowText(contentStream, "汇总信息");
        contentStream.endText();
        y -= LINE_HEIGHT;

        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);

        float x1 = MARGIN + 20;
        float x2 = MARGIN + 200;

        // 第一列
        contentStream.newLineAtOffset(x1, y);
        safeShowText(contentStream, "期初余额:");
        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT);
        safeShowText(contentStream, "期末余额:");
        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 2);
        safeShowText(contentStream, "总收入:");
        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 3);
        safeShowText(contentStream, "总支出:");
        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 4);
        safeShowText(contentStream, "利息收入:");
        contentStream.endText();

        // 第二列
        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
        contentStream.newLineAtOffset(x2, y);
        safeShowText(contentStream, formatAmount(summary.getStartBalance()) + " 元");
        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT);
        safeShowText(contentStream, formatAmount(summary.getEndBalance()) + " 元");
        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 2);
        safeShowText(contentStream, formatAmount(summary.getTotalDeposit()) + " 元");
        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 3);
        safeShowText(contentStream, formatAmount(summary.getTotalWithdraw()) + " 元");
        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 4);
        safeShowText(contentStream, formatAmount(summary.getInterestEarned()) + " 元");
        contentStream.endText();

        // 第三列
        float x3 = MARGIN + 350;
        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
        contentStream.newLineAtOffset(x3, y);
        safeShowText(contentStream, "交易笔数:");
        contentStream.newLineAtOffset(x3, y - LINE_HEIGHT);
        safeShowText(contentStream, "净变化:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
        contentStream.newLineAtOffset(x3 + 80, y);
        safeShowText(contentStream, String.valueOf(summary.getTransactionCount()));
        contentStream.newLineAtOffset(x3 + 80, y - LINE_HEIGHT);
        safeShowText(contentStream, formatAmount(summary.getNetChange()) + " 元");
        contentStream.endText();

        return y - LINE_HEIGHT * 6;
    }

    /**
     * 添加分类统计
     */
    private float addCategoryStatistics(PDPageContentStream contentStream,
                                        ReportResponseDTO.CategoryStatistics stats, float y) throws IOException {
        if (stats == null) return y;

        contentStream.beginText();
        contentStream.setFont(chineseHeaderFont, HEADER_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, y);
        safeShowText(contentStream, "分类统计");
        contentStream.endText();
        y -= LINE_HEIGHT;

        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);

        float x1 = MARGIN + 20;
        float x2 = MARGIN + 200;

        contentStream.newLineAtOffset(x1, y);
        safeShowText(contentStream, "存款总额:");
        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT);
        safeShowText(contentStream, "取款总额:");
        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 2);
        safeShowText(contentStream, "利息总额:");
        contentStream.newLineAtOffset(x1, y - LINE_HEIGHT * 3);
        safeShowText(contentStream, "手续费总额:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, NORMAL_FONT_SIZE);
        contentStream.newLineAtOffset(x2, y);
        safeShowText(contentStream, formatAmount(stats.getDepositAmount()) + " 元");
        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT);
        safeShowText(contentStream, formatAmount(stats.getWithdrawAmount()) + " 元");
        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 2);
        safeShowText(contentStream, formatAmount(stats.getInterestAmount()) + " 元");
        contentStream.newLineAtOffset(x2, y - LINE_HEIGHT * 3);
        safeShowText(contentStream, formatAmount(stats.getFeeAmount()) + " 元");
        contentStream.endText();

        return y - LINE_HEIGHT * 5;
    }

    /**
     * 添加交易明细表格 - 支持分页
     */
    private float addTransactionDetailsTable(PDPageContentStream contentStream,
                                             List<ReportResponseDTO.TransactionSummary> transactions,
                                             String title,
                                             float startY) throws IOException {
        float currentY = startY;

        // 表格标题
        contentStream.beginText();
        contentStream.setFont(chineseHeaderFont, HEADER_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, currentY);
        safeShowText(contentStream, title);
        contentStream.endText();
        currentY -= LINE_HEIGHT * 1.5f;

        // 定义表格列位置（固定位置，确保对齐）
        float[] columnPositions = {
                MARGIN + 10,     // 时间列
                MARGIN + 120,    // 类型列
                MARGIN + 180,    // 金额列
                MARGIN + 250,    // 余额列
                MARGIN + 350     // 备注列
        };

        String[] headers = {"时间", "类型", "金额 (元)", "余额 (元)", "备注"};

        // 绘制表头
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(columnPositions[i], currentY);
            safeShowText(contentStream, headers[i]);
            contentStream.endText();
        }

        // 表头下划线
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, currentY - 5);
        contentStream.lineTo(PAGE_SIZE.getWidth() - MARGIN, currentY - 5);
        contentStream.stroke();

        currentY -= TABLE_ROW_HEIGHT;

        // 绘制数据行
        for (ReportResponseDTO.TransactionSummary trans : transactions) {
            // 检查页面空间是否足够
            if (currentY < MARGIN + TABLE_ROW_HEIGHT * 2) {
                // 空间不足，需要在外部创建新页面
                break;
            }

            // 时间
            contentStream.beginText();
            contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(columnPositions[0], currentY);
            safeShowText(contentStream, formatShortDate(trans.getTransTime()));
            contentStream.endText();

            // 类型
            contentStream.beginText();
            contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(columnPositions[1], currentY);
            safeShowText(contentStream, getTransTypeChinese(trans.getTransType()));
            contentStream.endText();

            // 金额
            contentStream.beginText();
            contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(columnPositions[2], currentY);
            safeShowText(contentStream, formatAmount(trans.getAmount()));
            contentStream.endText();

            // 余额
            contentStream.beginText();
            contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(columnPositions[3], currentY);
            safeShowText(contentStream, formatAmount(trans.getBalanceAfter()));
            contentStream.endText();

            // 备注
            String remark = trans.getRemark() != null ? trans.getRemark() : "";
            if (remark.length() > 20) {
                remark = remark.substring(0, 20) + "...";
            }
            contentStream.beginText();
            contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(columnPositions[4], currentY);
            safeShowText(contentStream, remark);
            contentStream.endText();

            currentY -= TABLE_ROW_HEIGHT;
        }

        // 显示记录统计
        if (transactions.size() > 0) {
            contentStream.beginText();
            contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(MARGIN, currentY);
            safeShowText(contentStream, String.format("共 %d 条记录", transactions.size()));
            contentStream.endText();
            currentY -= LINE_HEIGHT;
        }

        return currentY;
    }

    /**
     * 添加页脚（带页码）
     */
    private void addPageFooter(PDPageContentStream contentStream,
                               ReportResponseDTO report,
                               int currentPage,
                               int totalPages,
                               float currentY) throws IOException {
        float footerY = MARGIN - 20;

        // 左侧：系统信息
        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
        contentStream.newLineAtOffset(MARGIN, footerY);
        safeShowText(contentStream, "银行管理系统 - 账单报告");
        contentStream.endText();

        // 中间：免责声明
        String disclaimer = "本报告仅供参考，具体以银行系统为准";
        float disclaimerWidth = chineseNormalFont.getStringWidth(disclaimer) / 1000 * SMALL_FONT_SIZE;
        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
        contentStream.newLineAtOffset((PAGE_SIZE.getWidth() - disclaimerWidth) / 2, footerY);
        safeShowText(contentStream, disclaimer);
        contentStream.endText();

        // 右侧：页码和生成时间
        String pageInfo = String.format("第 %d/%d 页 | 生成时间: %s",
                currentPage, totalPages, DATE_FORMAT.format(new Date()));
        float pageInfoWidth = chineseNormalFont.getStringWidth(pageInfo) / 1000 * SMALL_FONT_SIZE;
        contentStream.beginText();
        contentStream.setFont(chineseNormalFont, SMALL_FONT_SIZE);
        contentStream.newLineAtOffset(PAGE_SIZE.getWidth() - MARGIN - pageInfoWidth, footerY);
        safeShowText(contentStream, pageInfo);
        contentStream.endText();
    }

    // ============ 辅助方法 ============

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return "0.00";
        return AMOUNT_FORMAT.format(amount);
    }

    private String formatShortDate(Date date) {
        if (date == null) return "";
        return SIMPLE_DATE_FORMAT.format(date);
    }

    private String formatShortDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String getTransTypeChinese(String type) {
        if (type == null) return "";
        switch (type.toUpperCase()) {
            case "DEPOSIT": return "存款";
            case "WITHDRAW": return "取款";
            case "INTEREST": return "利息";
            case "TRANSFER": return "转账";
            default: return type;
        }
    }
}