package com.zychen.bank.controller;


import com.zychen.bank.dto.FreezeCardDTO;
import com.zychen.bank.dto.UnfreezeCardDTO;
import com.zychen.bank.service.SecurityService;
import com.zychen.bank.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/security")
public class SecurityController {

    @Autowired
    private SecurityService securityService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户申请冻结银行卡
     */
    @PostMapping("/freeze/card")
    public ResponseEntity<?> freezeCard(
            @Valid @RequestBody FreezeCardDTO dto,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            Map<String, Object> result = securityService.freezeCard(dto, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "银行卡冻结申请已提交");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 用户申请解冻银行卡
     */
    @PostMapping("/unfreeze/card")
    public ResponseEntity<?> unfreezeCard(
            @Valid @RequestBody UnfreezeCardDTO dto,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            Map<String, Object> result = securityService.unfreezeCard(dto, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "银行卡解冻成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 查询冻结记录
     */
    @GetMapping("/freeze-records")
    public ResponseEntity<?> getFreezeRecords(
            @RequestParam(required = false) String cardId,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            var records = securityService.getFreezeRecords(userId, cardId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", records);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}