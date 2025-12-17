package com.zychen.bank.controller;

import com.zychen.bank.dto.AddAdminDTO;
import com.zychen.bank.dto.OperationLogQueryDTO;
import com.zychen.bank.dto.ResetUserPasswordDTO;
import com.zychen.bank.dto.UserQueryDTO;
import com.zychen.bank.model.User;
import com.zychen.bank.service.OperationLogService;
import com.zychen.bank.service.UserService;
import com.zychen.bank.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 管理员添加新管理员
     * 仅管理员可访问
     */
    @PostMapping("/users/admins")
    public ResponseEntity<?> addAdmin(
            @Valid @RequestBody AddAdminDTO addAdminDTO,
            HttpServletRequest request) {
        try {
            // 从token获取当前用户信息
            String token = request.getHeader("Authorization").substring(7);
            String currentUserId = jwtUtil.getUserIdFromToken(token);
            Integer currentUserRole = jwtUtil.getRoleFromToken(token);

            // 验证当前用户是管理员
            if (currentUserRole != 1) {
                // ============ 添加权限拒绝日志 ============
                String ipAddress = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");

                operationLogService.logOperation(
                        currentUserId,
                        currentUserRole,
                        "USER",
                        "ADD_ADMIN",
                        "尝试添加管理员被拒绝：权限不足，当前角色：" + currentUserRole,
                        "USER",
                        null,
                        ipAddress,
                        userAgent,
                        0,
                        "权限不足",
                        0
                );
                // ============ 日志结束 ============
                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "权限不足，仅管理员可操作");
                error.put("data", null);
                return ResponseEntity.status(403).body(error);
            }

            // 调用Service添加管理员
            Map<String, Object> result = userService.addAdmin(addAdminDTO, currentUserId);
            // ============ 添加成功日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取新管理员的ID
            String newAdminId = (String) result.get("user_id");

            operationLogService.logOperation(
                    currentUserId,
                    currentUserRole,
                    "USER",
                    "ADD_ADMIN",
                    "管理员添加新管理员成功，新管理员ID：" + newAdminId + "，用户名：" + addAdminDTO.getUsername(),
                    "USER",
                    newAdminId,
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "管理员账号创建成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取操作者信息
            String currentUserId = null;
            Integer currentUserRole = null;
            try {
                String token = request.getHeader("Authorization").substring(7);
                currentUserId = jwtUtil.getUserIdFromToken(token);
                currentUserRole = jwtUtil.getRoleFromToken(token);
            } catch (Exception ex) {
                // 忽略
            }

            operationLogService.logOperation(
                    currentUserId,
                    currentUserRole,
                    "USER",
                    "ADD_ADMIN",
                    "添加新管理员失败：" + e.getMessage() + "，尝试添加的用户名：" + addAdminDTO.getUsername(),
                    "USER",
                    null,
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
            // ============ 添加系统异常日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            operationLogService.logOperation(
                    null,
                    null,
                    "USER",
                    "ADD_ADMIN",
                    "添加新管理员时发生系统异常",
                    "USER",
                    null,
                    ipAddress,
                    userAgent,
                    0,
                    "系统内部错误",
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 查询所有用户（管理员功能）
     */
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @ModelAttribute UserQueryDTO queryDTO,
            HttpServletRequest request) {
        try {
            // 验证管理员权限
            String token = request.getHeader("Authorization").substring(7);
            Integer currentUserRole = jwtUtil.getRoleFromToken(token);

            if (currentUserRole != 1) {

                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "权限不足，仅管理员可查询");
                error.put("data", null);
                return ResponseEntity.status(403).body(error);
            }

            // 调用Service查询用户
            Map<String, Object> result = userService.getUsers(queryDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 查看用户的所有银行卡（管理员功能）
     */
    @GetMapping("/users/{userId}/cards")
    public ResponseEntity<?> getUserCards(
            @PathVariable String userId,
            HttpServletRequest request) {
        try {
            // 验证管理员权限
            String token = request.getHeader("Authorization").substring(7);
            Integer currentUserRole = jwtUtil.getRoleFromToken(token);

            if (currentUserRole != 1) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "权限不足，仅管理员可查询");
                return ResponseEntity.status(403).body(error);
            }

            // 调用Service查询用户银行卡
            Map<String, Object> result = userService.getUserCards(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 查询操作日志（管理员功能）
     */
    @GetMapping("/operation-logs")
    public ResponseEntity<?> getOperationLogs(
            @ModelAttribute OperationLogQueryDTO queryDTO,
            HttpServletRequest request) {
        try {
            // 验证管理员权限
            String token = request.getHeader("Authorization").substring(7);
            Integer currentUserRole = jwtUtil.getRoleFromToken(token);

            if (currentUserRole != 1) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "权限不足，仅管理员可查询");
                error.put("data", null);
                return ResponseEntity.status(403).body(error);
            }

            // 调用Service查询操作日志
            Map<String, Object> result = operationLogService.getOperationLogs(queryDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 管理员重置用户密码
     * POST /api/admin/users/reset-password
     */
    @PostMapping("/users/reset-password")
    public ResponseEntity<?> resetUserPassword(
            @Valid @RequestBody ResetUserPasswordDTO dto,
            HttpServletRequest request) {
        try {
            // 获取管理员ID
            String token = request.getHeader("Authorization").substring(7);
            String adminId = jwtUtil.getUserIdFromToken(token);

            // 验证管理员角色 - 使用正确的方法名
            User admin = userService.findByUserId(adminId);  // 修复这里！
            if (admin == null || admin.getRole() != 1) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "无权执行此操作");
                return ResponseEntity.status(403).body(error);
            }

            // 执行密码重置
            userService.resetUserPassword(adminId, dto.getTargetUserId(), dto.getReason());

            // 记录操作日志
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            operationLogService.logOperation(
                    adminId,
                    1,  // 管理员角色
                    "ADMIN",
                    "RESET_USER_PASSWORD",
                    "管理员重置用户密码，目标用户：" + dto.getTargetUserId() +
                            (dto.getReason() != null ? "，原因：" + dto.getReason() : ""),
                    "USER",
                    dto.getTargetUserId(),
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );

            // 返回成功
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "密码重置成功，新密码：123456");
            response.put("data", Map.of(
                    "target_user_id", dto.getTargetUserId(),
                    "new_password", "123456",
                    "reset_time", new Date()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("重置用户密码失败", e);

            // 记录失败日志
            try {
                String token = request.getHeader("Authorization").substring(7);
                String adminId = jwtUtil.getUserIdFromToken(token);

                operationLogService.logOperation(
                        adminId,
                        1,
                        "ADMIN",
                        "RESET_USER_PASSWORD",
                        "重置用户密码失败：" + e.getMessage(),
                        "USER",
                        dto != null ? dto.getTargetUserId() : null,
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent"),
                        0,
                        e.getMessage(),
                        0
                );
            } catch (Exception logEx) {
                log.error("记录操作日志失败", logEx);
            }

            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}