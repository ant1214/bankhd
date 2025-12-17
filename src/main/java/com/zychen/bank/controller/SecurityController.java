package com.zychen.bank.controller;


import com.zychen.bank.dto.AdminFreezeRequestDTO;
import com.zychen.bank.dto.FreezeCardDTO;
import com.zychen.bank.dto.LostReportDTO;
import com.zychen.bank.dto.UnfreezeCardDTO;
import com.zychen.bank.service.OperationLogService;
import com.zychen.bank.service.SecurityService;
import com.zychen.bank.service.UserService;
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
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private UserService userService;

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
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Integer userRole = getUserRoleSafely(userId);

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "SECURITY",
                    "FREEZE_CARD",
                    "用户申请冻结银行卡成功，卡号：" + dto.getCardId() + "，原因：" + dto.getReason(),
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
            response.put("message", "银行卡冻结申请已提交");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取用户ID（可能失败）
            String userId = null;
            Integer userRole = null;
            try {
                String token = request.getHeader("Authorization").substring(7);
                userId = jwtUtil.getUserIdFromToken(token);
                userRole = getUserRoleSafely(userId);
            } catch (Exception ex) {
                // 忽略
            }

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "SECURITY",
                    "FREEZE_CARD",
                    "用户申请冻结银行卡失败：" + e.getMessage() + "，卡号：" + dto.getCardId(),
                    "CARD",
                    dto.getCardId(),
                    ipAddress,
                    userAgent,
                    0,
                    e.getMessage(),
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
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Integer userRole = getUserRoleSafely(userId);

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "SECURITY",
                    "UNFREEZE_CARD",
                    "用户申请解冻银行卡成功，卡号：" + dto.getCardId() + "，原因：" + dto.getReason(),
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
            response.put("message", "银行卡解冻成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            String userId = null;
            Integer userRole = null;
            try {
                String token = request.getHeader("Authorization").substring(7);
                userId = jwtUtil.getUserIdFromToken(token);
                userRole = getUserRoleSafely(userId);
            } catch (Exception ex) {
                // 忽略
            }

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "SECURITY",
                    "UNFREEZE_CARD",
                    "用户申请解冻银行卡失败：" + e.getMessage() + "，卡号：" + dto.getCardId(),
                    "CARD",
                    dto.getCardId(),
                    ipAddress,
                    userAgent,
                    0,
                    e.getMessage(),
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


    /**
     * 管理员冻结/解冻账户或银行卡
     * 权限：仅管理员
     */
    @PostMapping("/admin/freeze")
    public ResponseEntity<?> adminFreeze(
            @Valid @RequestBody AdminFreezeRequestDTO dto,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String operatorId = jwtUtil.getUserIdFromToken(token);

            // 调用Service方法
            Map<String, Object> result = securityService.adminFreezeOrUnfreeze(dto, operatorId);

            // ============ 添加管理员操作日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Integer operatorRole = getUserRoleSafely(operatorId);

            String operationType = "freeze".equals(dto.getOperation()) ? "ADMIN_FREEZE" : "ADMIN_UNFREEZE";
            String operationDetail = "管理员" + ("freeze".equals(dto.getOperation()) ? "冻结" : "解冻") +
                    (dto.getTargetType().equals("account") ? "账户" : "银行卡") +
                    "，目标ID：" + dto.getTargetId() + "，原因：" + dto.getReasonDetail();

            operationLogService.logOperation(
                    operatorId,
                    operatorRole,
                    "SECURITY",
                    operationType,
                    operationDetail,
                    dto.getTargetType().toUpperCase(),
                    dto.getTargetId(),
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> response = new HashMap<>();
            if (Boolean.TRUE.equals(result.get("success"))) {
                response.put("code", 200);
                response.put("message", result.get("message"));
                response.put("data", result);
            } else {
                response.put("code", 400);
                response.put("message", result.get("message"));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 失败日志
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            String operatorId = null;
            try {
                String token = request.getHeader("Authorization").substring(7);
                operatorId = jwtUtil.getUserIdFromToken(token);
            } catch (Exception ex) {
                // 忽略
            }

            operationLogService.logOperation(
                    operatorId,
                    operatorId != null ? getUserRoleSafely(operatorId) : null,
                    "SECURITY",
                    "ADMIN_FREEZE",
                    "管理员冻结/解冻操作失败：" + e.getMessage(),
                    "USER",
                    null,
                    ipAddress,
                    userAgent,
                    0,
                    e.getMessage(),
                    0
            );

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 400);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 管理员挂失/解挂银行卡
     * 权限：仅管理员
     */
    @PostMapping("/admin/lost-report")
    public ResponseEntity<?> adminLostReport(
            @Valid @RequestBody LostReportDTO dto,
            HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").substring(7);
            String operatorId = jwtUtil.getUserIdFromToken(token);

            // 调用Service方法
            Map<String, Object> result = securityService.adminLostReport(dto, operatorId);
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Integer operatorRole = getUserRoleSafely(operatorId);

            String operationType = "report".equals(dto.getOperation()) ? "ADMIN_LOST_REPORT" : "ADMIN_CANCEL_LOST";
            String operationDetail = "管理员" + ("report".equals(dto.getOperation()) ? "挂失" : "解挂") +
                    "银行卡，卡号：" + dto.getCardId() + "，原因：" + dto.getReasonDetail();

            operationLogService.logOperation(
                    operatorId,
                    operatorRole,
                    "SECURITY",
                    operationType,
                    operationDetail,
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
            if (Boolean.TRUE.equals(result.get("success"))) {
                response.put("code", 200);
                response.put("message", result.get("message"));
                response.put("data", result);
            } else {
                response.put("code", 400);
                response.put("message", result.get("message"));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            String operatorId = null;
            try {
                String token = request.getHeader("Authorization").substring(7);
                operatorId = jwtUtil.getUserIdFromToken(token);
            } catch (Exception ex) {
                // 忽略
            }

            operationLogService.logOperation(
                    operatorId,
                    operatorId != null ? getUserRoleSafely(operatorId) : null,
                    "SECURITY",
                    "ADMIN_LOST_REPORT",
                    "管理员挂失/解挂银行卡失败：" + e.getMessage() + "，卡号：" + dto.getCardId(),
                    "CARD",
                    dto.getCardId(),
                    ipAddress,
                    userAgent,
                    0,
                    e.getMessage(),
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