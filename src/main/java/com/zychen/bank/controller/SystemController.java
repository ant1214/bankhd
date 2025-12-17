package com.zychen.bank.controller;


import com.zychen.bank.service.InterestRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/system")
public class SystemController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InterestRateService interestRateService;

    /**
     * 系统健康检查
     * GET /api/system/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();

        // 基本状态
        result.put("status", "UP");
        result.put("timestamp", new Date());
        result.put("service", "银行管理系统");
        result.put("version", "1.0.0");
        result.put("environment", "production");

        // 检查数据库连接
        String dbStatus = "CONNECTED";
        String dbError = null;
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                dbStatus = "CONNECTED";
            } else {
                dbStatus = "DISCONNECTED";
            }
        } catch (Exception e) {
            dbStatus = "ERROR";
            dbError = e.getMessage();
            log.error("数据库连接检查失败", e);
        }

        Map<String, Object> database = new HashMap<>();
        database.put("status", dbStatus);
        if (dbError != null) {
            database.put("error", dbError);
        }
        result.put("database", database);

        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total_mb", runtime.totalMemory() / 1024 / 1024);
        memory.put("free_mb", runtime.freeMemory() / 1024 / 1024);
        memory.put("used_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        memory.put("max_mb", runtime.maxMemory() / 1024 / 1024);
        memory.put("usage_percent",
                String.format("%.1f%%",
                        (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100));
        result.put("memory", memory);

        // 系统信息
        Map<String, Object> system = new HashMap<>();
        system.put("processor_count", runtime.availableProcessors());
        system.put("uptime", formatUptime());
        system.put("java_version", System.getProperty("java.version"));
        system.put("os_name", System.getProperty("os.name"));
        result.put("system", system);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取利率配置
     * GET /api/system/interest-rates
     */
    @GetMapping("/interest-rates")
    public ResponseEntity<Map<String, Object>> getInterestRates() {
        try {
            Map<String, Object> rates = interestRateService.getFormattedInterestRates();

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取利率配置成功");
            response.put("data", rates);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取利率配置失败", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取利率配置失败: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime() {
        try {
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            long days = uptime / (1000 * 60 * 60 * 24);
            long hours = (uptime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
            long minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60);
            long seconds = (uptime % (1000 * 60)) / 1000;

            if (days > 0) {
                return String.format("%d天 %d小时 %d分钟 %d秒", days, hours, minutes, seconds);
            } else if (hours > 0) {
                return String.format("%d小时 %d分钟 %d秒", hours, minutes, seconds);
            } else if (minutes > 0) {
                return String.format("%d分钟 %d秒", minutes, seconds);
            } else {
                return String.format("%d秒", seconds);
            }
        } catch (Exception e) {
            return "未知";
        }
    }
}