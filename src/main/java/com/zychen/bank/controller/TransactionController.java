package com.zychen.bank.controller;

import com.zychen.bank.dto.DepositDTO;
import com.zychen.bank.dto.TransactionQueryDTO;
import com.zychen.bank.dto.WithdrawDTO;
import com.zychen.bank.service.TransactionService;
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

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "存款成功");
            response.put("data", depositResult);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
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

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "取款成功");
            response.put("data", withdrawResult);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("取款失败: {}", e.getMessage());

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