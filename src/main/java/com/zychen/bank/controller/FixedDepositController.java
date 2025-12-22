package com.zychen.bank.controller;

import com.zychen.bank.dto.EarlyWithdrawDTO;
import com.zychen.bank.dto.FixedDepositDTO;
import com.zychen.bank.dto.MatureWithdrawDTO;
import com.zychen.bank.model.FixedDeposit;
import com.zychen.bank.service.FixedDepositService;
import com.zychen.bank.service.OperationLogService;
import com.zychen.bank.service.UserService;
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

    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private UserService userService;

    // 安全截断错误信息
    private String truncateErrorMessage(String errorMsg, int maxLength) {
        if (errorMsg == null) return null;
        if (errorMsg.length() <= maxLength) return errorMsg;
        return errorMsg.substring(0, maxLength - 3) + "...";
    }

    /**
     * 安全获取用户角色，避免异常影响主流程
     */
    private Integer getUserRoleSafely(String userId) {
        if (userId == null) {
            return null;
        }
        try {
            return userService.getUserRole(userId);
        } catch (Exception e) {
            return 0;  // 默认普通用户
        }
    }
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
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Integer userRole = getUserRoleSafely(userId);

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "FIXED_DEPOSIT",
                    "CREATE_FD",
                    "创建定期存款成功，金额：" + dto.getPrincipal() + "元，卡号：" + dto.getCardId() + "，期限：" + dto.getTerm() + "个月",
                    "CARD",
                    dto.getCardId(),
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "定期存款创建成功");
            response.put("data", fixedDeposit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取用户ID
            String userId = null;
            try {
                String token = request.getHeader("Authorization").substring(7);
                userId = jwtUtil.getUserIdFromToken(token);
            } catch (Exception ex) {
                // 忽略
            }

            // ✅ 关键：不要将异常信息放到 operation_detail 中
            // 只放简单的描述
            String operationDetail = "创建定期存款失败";

            // ✅ 只截取第一行异常信息（不要包含堆栈跟踪）
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                // 只取第一行，去掉堆栈跟踪
                String[] lines = errorMsg.split("\n");
                errorMsg = lines[0].trim();
                // 截断到195字符
                if (errorMsg.length() > 195) {
                    errorMsg = errorMsg.substring(0, 195) + "...";
                }
            }

            operationLogService.logOperation(
                    userId,
                    userId != null ? getUserRoleSafely(userId) : null,
                    "FIXED_DEPOSIT",
                    "CREATE_FD",
                    operationDetail,  // ✅ 简单描述
                    "CARD",
                    dto.getCardId(),
                    ipAddress,
                    userAgent,
                    0,
                    errorMsg,  // ✅ 只放简洁的错误信息
                    0
            );
            // ============ 日志结束 ============

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


    /**
     * 提前支取定期存款
     */
    @PostMapping("/{fdId}/early-withdraw")
    public ResponseEntity<?> earlyWithdraw(
            @PathVariable Integer fdId,
            @Valid @RequestBody EarlyWithdrawDTO dto,
            HttpServletRequest request) {
        try {
            // 从token获取用户ID
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            Map<String, Object> result = fixedDepositService.earlyWithdraw(fdId, userId, dto.getCardPassword());
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取定期存款详情（用于记录卡号）
            FixedDeposit deposit = fixedDepositService.getFixedDepositDetail(fdId, userId);

            operationLogService.logOperation(
                    userId,
                    getUserRoleSafely(userId),
                    "FIXED_DEPOSIT",
                    "EARLY_WITHDRAW_FD",
                    "提前支取定期存款成功，存单ID：" + fdId + "，卡号：" + deposit.getCardId() + "，本金：" + deposit.getPrincipal() + "元",
                    "FIXED_DEPOSIT",
                    String.valueOf(fdId),
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "定期存款提前支取成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            String userId = null;
            try {
                String token = request.getHeader("Authorization").substring(7);
                userId = jwtUtil.getUserIdFromToken(token);
            } catch (Exception ex) {
                // 忽略
            }

            operationLogService.logOperation(
                    userId,
                    userId != null ? getUserRoleSafely(userId) : null,
                    "FIXED_DEPOSIT",
                    "EARLY_WITHDRAW_FD",
                    "提前支取定期存款失败：" +  "，存单ID：" + fdId,
                    "FIXED_DEPOSIT",
                    String.valueOf(fdId),
                    ipAddress,
                    userAgent,
                    0,
                    truncateErrorMessage(e.getMessage(), 200),
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 到期转出定期存款
     */
    @PostMapping("/{fdId}/mature")
    public ResponseEntity<?> matureWithdraw(
            @PathVariable Integer fdId,
            @Valid @RequestBody MatureWithdrawDTO dto,
            HttpServletRequest request) {
        try {
            // 从token获取用户ID
            String token = request.getHeader("Authorization").substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);

            Map<String, Object> result = fixedDepositService.matureWithdraw(fdId, userId, dto.getCardPassword());
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取定期存款详情
            FixedDeposit deposit = fixedDepositService.getFixedDepositDetail(fdId, userId);

            operationLogService.logOperation(
                    userId,
                    getUserRoleSafely(userId),
                    "FIXED_DEPOSIT",
                    "MATURE_FD",
                    "到期转出定期存款成功，存单ID：" + fdId + "，卡号：" + deposit.getCardId() + "，本金：" + deposit.getPrincipal() + "元",
                    "FIXED_DEPOSIT",
                    String.valueOf(fdId),
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );
            // ============ 日志结束 ============

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "定期存款到期转出成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            String userId = null;
            try {
                String token = request.getHeader("Authorization").substring(7);
                userId = jwtUtil.getUserIdFromToken(token);
            } catch (Exception ex) {
                // 忽略
            }

            operationLogService.logOperation(
                    userId,
                    userId != null ? getUserRoleSafely(userId) : null,
                    "FIXED_DEPOSIT",
                    "MATURE_FD",
                    "到期转出定期存款失败：" + "，存单ID：" + fdId,
                    "FIXED_DEPOSIT",
                    String.valueOf(fdId),
                    ipAddress,
                    userAgent,
                    0,
                    truncateErrorMessage(e.getMessage(), 200),
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}