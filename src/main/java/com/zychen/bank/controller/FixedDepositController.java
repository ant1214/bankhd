package com.zychen.bank.controller;

import com.zychen.bank.dto.FixedDepositDTO;
import com.zychen.bank.model.FixedDeposit;
import com.zychen.bank.service.FixedDepositService;
import com.zychen.bank.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fixed-deposits")
public class FixedDepositController {

    @Autowired
    private FixedDepositService fixedDepositService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建定期存款
     */
    @PostMapping("/create")
    public ResponseEntity<?> createFixedDeposit(
            @Valid @RequestBody FixedDepositDTO dto,
            HttpServletRequest request) {
        try {
            // 从token获取用户ID
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            FixedDeposit fixedDeposit = fixedDepositService.createFixedDeposit(dto, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "定期存款创建成功");
            response.put("data", fixedDeposit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取用户的定期存款列表
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyFixedDeposits(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            List<FixedDeposit> deposits = fixedDepositService.getFixedDepositsByUser(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", deposits);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取单笔定期存款详情
     */
    @GetMapping("/{fdId}")
    public ResponseEntity<?> getFixedDepositDetail(
            @PathVariable Integer fdId,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            FixedDeposit deposit = fixedDepositService.getFixedDepositDetail(fdId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", deposit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 获取指定银行卡的定期存款
     */
    @GetMapping("/card/{cardId}")
    public ResponseEntity<?> getFixedDepositsByCard(
            @PathVariable String cardId,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            List<FixedDeposit> deposits = fixedDepositService.getFixedDepositsByCard(cardId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", deposits);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}