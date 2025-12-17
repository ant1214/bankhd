package com.zychen.bank.controller;

import com.zychen.bank.dto.DepositDTO;
import com.zychen.bank.dto.TransactionQueryDTO;
import com.zychen.bank.dto.WithdrawDTO;
import com.zychen.bank.service.OperationLogService;
import com.zychen.bank.service.TransactionService;
import com.zychen.bank.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserService userService;

    /**
     * 安全获取用户角色，避免异常影响主流程
     */
    private Integer getSafeUserRole(String userId) {
        if (userId == null) {
            return null;
        }
        try {
            return userService.getUserRole(userId);
        } catch (Exception e) {
            log.warn("获取用户角色失败: {}", userId, e);
            return 0;  // 默认普通用户
        }
    }

    /**
     * 存款
     */
    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(
            @Valid @RequestBody DepositDTO depositDTO,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");

            Map<String, Object> depositResult = transactionService.deposit(userId, depositDTO);
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取用户角色（安全方式）
            Integer userRole = getSafeUserRole(userId);

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "TRANSACTION",
                    "DEPOSIT",
                    "存款成功，金额：" + depositDTO.getAmount() + "元，卡号：" + depositDTO.getCardId(),
                    "CARD",
                    depositDTO.getCardId(),
                    ipAddress,
                    userAgent,
                    1,  // 成功
                    null,
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "存款成功");
            response.put("data", depositResult);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取用户ID（从request中）
            String userId = null;
            try {
                userId = (String) request.getAttribute("userId");
            } catch (Exception ex) {
                // 忽略
            }

            // 获取用户角色
            Integer userRole = null;
            if (userId != null) {
                userRole = getSafeUserRole(userId);
            }

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "TRANSACTION",
                    "DEPOSIT",
                    "存款失败：" + e.getMessage() + "，金额：" + depositDTO.getAmount() + "元，卡号：" + depositDTO.getCardId(),
                    "CARD",
                    depositDTO.getCardId(),
                    ipAddress,
                    userAgent,
                    0,  // 失败
                    e.getMessage(),
                    0
            );
            // ============ 日志结束 ============
            log.warn("存款失败: {}", e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);

            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("存款异常", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);

            return ResponseEntity.internalServerError().body(error);
        }
    }


    /**
     * 取款
     */
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            @Valid @RequestBody WithdrawDTO withdrawDTO,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");

            Map<String, Object> withdrawResult = transactionService.withdraw(userId, withdrawDTO);
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            operationLogService.logOperation(
                    userId,
                    getSafeUserRole(userId),
                    "TRANSACTION",
                    "WITHDRAW",
                    "取款成功，金额：" + withdrawDTO.getAmount() + "元，卡号：" + withdrawDTO.getCardId(),
                    "CARD",
                    withdrawDTO.getCardId(),
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "取款成功");
            response.put("data", withdrawResult);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("取款失败: {}", e.getMessage());
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            String userId = null;
            try {
                userId = (String) request.getAttribute("userId");
            } catch (Exception ex) {
                // 忽略
            }

            operationLogService.logOperation(
                    userId,
                    userId != null ? getSafeUserRole(userId) : null,
                    "TRANSACTION",
                    "WITHDRAW",
                    "取款失败：" + e.getMessage() + "，金额：" + withdrawDTO.getAmount() + "元，卡号：" + withdrawDTO.getCardId(),
                    "CARD",
                    withdrawDTO.getCardId(),
                    ipAddress,
                    userAgent,
                    0,
                    e.getMessage(),
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);

            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("取款异常", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);

            return ResponseEntity.internalServerError().body(error);
        }
    }


    /**
     * 查询交易记录
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTransactions(
            @ModelAttribute TransactionQueryDTO queryDTO,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");

            Map<String, Object> result = transactionService.getTransactions(userId, queryDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", result);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("查询交易记录异常", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);

            return ResponseEntity.internalServerError().body(error);
        }
    }
}