package com.zychen.bank.controller;

import com.zychen.bank.dto.ChangePasswordDTO;
import com.zychen.bank.dto.UpdateUserInfoDTO;
import com.zychen.bank.dto.UserStatisticsDTO;
import com.zychen.bank.model.User;
import com.zychen.bank.service.OperationLogService;
import com.zychen.bank.service.UserService;
import com.zychen.bank.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;
    @Autowired
    private OperationLogService operationLogService;
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        try {
            // 从拦截器设置的属性中获取用户ID
            String userId = (String) request.getAttribute("userId");

            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("未找到用户信息");
            }

//            // 查询用户
//            User user = userService.findByUserId(userId);
//            if (user == null) {
//                throw new RuntimeException("用户不存在");
//            }
//
//            // 构建响应数据
//            Map<String, Object> userInfo = new HashMap<>();
//            userInfo.put("userId", user.getUserId());
//            userInfo.put("username", user.getUsername());
//            userInfo.put("phone", user.getPhone());
//            userInfo.put("role", user.getRole());
//            userInfo.put("accountStatus", user.getAccountStatus());
//            userInfo.put("createdTime", user.getCreatedTime());
//            userInfo.put("lastLoginTime", user.getLastLoginTime());
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("code", 200);
//            response.put("message", "获取成功");
//            response.put("data", userInfo);
//
//            log.info("用户信息查询成功: {}", userId);
//            return ResponseEntity.ok(response);
// 使用新方法获取完整信息
            Map<String, Object> userInfo = userService.getUserFullInfo(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", userInfo);

            log.info("用户信息查询成功: {}", userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("用户信息查询失败: {}", e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);

            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("用户信息查询异常", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO,
            HttpServletRequest request) {
        try {
            String userId = (String) request.getAttribute("userId");

            userService.changePassword(userId, changePasswordDTO);
            // ============ 添加成功日志 ============
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
                    "SECURITY",
                    "CHANGE_PASSWORD",
                    "修改密码成功",
                    "USER",
                    userId,
                    ipAddress,
                    userAgent,
                    1,  // 成功
                    null,
                    0
            );
            // ============ 日志结束 ============
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "密码修改成功");
            response.put("data", null);

            log.info("密码修改成功: {}", userId);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("密码修改失败: {}", e.getMessage());
            // ============ 添加失败日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 获取用户ID（可能为null）
            String userId = null;
            try {
                userId = (String) request.getAttribute("userId");
            } catch (Exception ex) {
                log.warn("获取用户ID失败", ex);
            }

            // 获取用户角色
            Integer userRole = null;
            if (userId != null) {
                try {
                    userRole = userService.getUserRole(userId);
                } catch (Exception ex) {
                    userRole = 0; // 默认普通用户
                }
            }

            operationLogService.logOperation(
                    userId,
                    userRole,
                    "SECURITY",
                    "CHANGE_PASSWORD",
                    "修改密码失败：" + e.getMessage(),
                    "USER",
                    userId,
                    ipAddress,
                    userAgent,
                    0,  // 失败
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
            log.error("密码修改异常", e);
            // ============ 添加系统异常日志 ============
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            operationLogService.logOperation(
                    null,
                    null,
                    "SECURITY",
                    "CHANGE_PASSWORD",
                    "修改密码时发生系统异常",
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
     * 更新用户信息
     * 用户只能更新自己的信息
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUserInfo(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserInfoDTO updateDTO,
            HttpServletRequest request) {
        try {
            // 从token获取当前用户ID
            String token = request.getHeader("Authorization").substring(7);
            String currentUserId = jwtUtil.getUserIdFromToken(token);
            Integer currentUserRole = jwtUtil.getRoleFromToken(token);

            // 权限验证：用户只能更新自己的信息，管理员可以更新任何用户
            if (!currentUserId.equals(userId) && currentUserRole != 1) {
                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "无权更新其他用户信息");
                error.put("data", null);
                return ResponseEntity.status(403).body(error);
            }

            // 调用Service更新用户信息
            Map<String, Object> result = userService.updateUserInfo(userId, updateDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "用户信息更新成功");
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.badRequest().body(error);  // 改为 error
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);
            return ResponseEntity.internalServerError().body(error);  // 改为 error
        }
    }

    /**
     * 获取用户统计信息
     * API: GET /api/users/{userId}/statistics
     */
    @GetMapping("/{userId}/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics(
            @PathVariable String userId,
            HttpServletRequest request) {
        try {
            // 权限验证：用户只能查看自己的统计信息
            String currentUserId = (String) request.getAttribute("userId");

            if (!userId.equals(currentUserId)) {
                // 检查是否是管理员
                String token = request.getHeader("Authorization").substring(7);
                Integer role = jwtUtil.getRoleFromToken(token);

                if (role == null || role != 1) { // 不是管理员
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", 403);
                    error.put("message", "只能查看自己的统计信息");
                    return ResponseEntity.status(403).body(error);
                }
            }

            // 获取统计信息
            UserStatisticsDTO statistics = userService.getUserStatistics(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "查询成功");
            response.put("data", statistics);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("获取用户统计信息异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}