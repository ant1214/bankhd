package com.zychen.bank.controller;

import com.zychen.bank.dto.GenerateReportDTO;
import com.zychen.bank.dto.ReportResponseDTO;
import com.zychen.bank.service.OperationLogService;
import com.zychen.bank.service.PdfExportService;
import com.zychen.bank.service.ReportService;
import com.zychen.bank.service.UserService;
import com.zychen.bank.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private PdfExportService pdfExportService;

    /**
     * 生成账单报告
     * API: POST /api/reports/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(
            @Valid @RequestBody GenerateReportDTO request,
            HttpServletRequest httpRequest) {
        try {
            // 权限验证：用户只能生成自己的报告
            String currentUserId = (String) httpRequest.getAttribute("userId");

            if (!request.getUserId().equals(currentUserId)) {
                // 检查是否是管理员
                String token = httpRequest.getHeader("Authorization").substring(7);
                Integer role = jwtUtil.getRoleFromToken(token);

                if (role == null || role != 1) { // 不是管理员
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", 403);
                    error.put("message", "只能生成自己的账单报告");
                    return ResponseEntity.status(403).body(error);
                }
            }

            // 生成报告
            ReportResponseDTO report = reportService.generateReport(request);

            // 记录操作日志
            logOperation(httpRequest, currentUserId, "生成账单报告", request.getReportType(), 1, null);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "报告生成成功");
            response.put("data", report);

            log.info("账单报告生成成功: userId={}, type={}", request.getUserId(), request.getReportType());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("生成账单报告失败: {}", e.getMessage());

            // 记录失败日志
            String currentUserId = (String) httpRequest.getAttribute("userId");
            logOperation(httpRequest, currentUserId, "生成账单报告失败", e.getMessage(), 0, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("生成账单报告异常", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{reportId}/download")
    public ResponseEntity<?> downloadReport(
            @PathVariable String reportId,
            @RequestParam(required = false, defaultValue = "pdf") String format,
            HttpServletRequest request) {
        try {
            // 权限验证
            String currentUserId = (String) request.getAttribute("userId");

            // 获取报告数据
            ReportResponseDTO report = reportService.getReportData(reportId);
            if (report == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 404);
                error.put("message", "报告不存在或已过期");
                return ResponseEntity.status(404).body(error);
            }

            // 验证权限：用户只能下载自己的报告
            if (!report.getUserId().equals(currentUserId)) {
                // 检查是否是管理员
                String token = request.getHeader("Authorization").substring(7);
                Integer role = jwtUtil.getRoleFromToken(token);

                if (role == null || role != 1) { // 不是管理员
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", 403);
                    error.put("message", "无权下载此报告");
                    return ResponseEntity.status(403).body(error);
                }
            }

            byte[] fileContent;
            String contentType;
            String fileName;

            if ("csv".equalsIgnoreCase(format)) {
                // CSV导出（如果实现了）
                fileContent = reportService.exportReportAsCsv(reportId);
                contentType = "text/csv;charset=UTF-8";
                fileName = report.getReportId() + ".csv";
            } else {
                // PDF导出
                // fileContent = pdfExportService.exportReportToPdf(report);
                fileContent = reportService.exportReportAsPdf(reportId);
                contentType = "application/pdf";
                fileName = report.getReportId() + ".pdf";
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);

        } catch (Exception e) {
            log.error("下载报告异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 记录操作日志
     */
    private void logOperation(HttpServletRequest request, String userId,
                              String operation, String detail,
                              int status, String errorMessage) {
        try {
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取用户角色
            Integer userRole = null;
            try {
                userRole = userService.getUserRole(userId);
            } catch (Exception e) {
                log.warn("获取用户角色失败: {}", userId, e);
                userRole = 0; // 默认普通用户
            }

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "REPORT",
                    operation,
                    detail,
                    "USER",
                    userId,
                    ipAddress,
                    userAgent,
                    status,
                    errorMessage,
                    0
            );
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    // 需要注入的依赖
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;
}