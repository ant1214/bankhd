package com.zychen.bank.controller;

import com.zychen.bank.dto.*;
import com.zychen.bank.model.User;
import com.zychen.bank.service.OperationLogService;
import com.zychen.bank.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private UserService userService;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    // ============ 工具方法：安全截断错误信息 ============
    private String truncateErrorMessage(String errorMsg, int maxLength) {
        if (errorMsg == null) return null;
        if (errorMsg.length() <= maxLength) return errorMsg;
        return errorMsg.substring(0, maxLength - 3) + "...";
    }

    private String getSafeErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null) return "未知错误";
        String[] lines = e.getMessage().split("\n");
        String firstLine = lines[0].trim();
        return truncateErrorMessage(firstLine, 200);
    }


    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            User user = userService.register(registerDTO);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "注册成功");
            response.put("data", Map.of(
                    "userId", user.getUserId(),
                    "username", user.getUsername(),
                    "phone", user.getPhone()
            ));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);

            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("注册失败", e);

            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);

            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request){
        try {
            Map<String, Object> loginResult = userService.login(loginDTO);

            // 获取客户端信息
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            // 从登录结果获取用户信息
            String userId = (String) loginResult.get("userId");
            Integer role = (Integer) loginResult.get("role");

            // 记录登录成功日志
            operationLogService.logOperation(
                    userId,
                    role,
                    "AUTH",
                    "LOGIN",
                    "用户登录成功",
                    "USER",
                    userId,
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "登录成功");
            response.put("data", loginResult);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            error.put("data", null);
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            // 获取客户端信息
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            String safeErrorMsg = getSafeErrorMessage(e);
            // 记录登录失败日志
            operationLogService.logOperation(
                    null,
                    null,
                    "AUTH",
                    "LOGIN",
                    "用户登录失败: ",
                    "USER",
                    null,
                    ipAddress,
                    userAgent,
                    0,
                    safeErrorMsg,
                    0
            );
            log.error("登录失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "bank-system-auth");
        return ResponseEntity.ok(response);
    }

    /**
     * 测试token验证的接口
     */
    @GetMapping("/test-token")
    public ResponseEntity<Map<String, Object>> testToken(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "Token验证成功");
        response.put("data", Map.of(
                "userId", userId,
                "role", role,
                "message", "您的token有效"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 管理员登录
     * 验证必须是管理员角色
     */
    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> adminLogin(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request){
        try {
            // 获取客户端信息
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            Map<String, Object> loginResult = userService.login(loginDTO);

            // 验证必须是管理员
            Object roleObj = loginResult.get("role");
            System.out.println("Role object: " + roleObj);

            if (roleObj == null) {
                throw new RuntimeException("role字段不存在");
            }

            Integer role;
            if (roleObj instanceof Integer) {
                role = (Integer) roleObj;
            } else if (roleObj instanceof Number) {
                role = ((Number) roleObj).intValue();
            } else {
                throw new RuntimeException("role字段类型错误: " + roleObj.getClass());
            }

            if (role != 1) {
                // 记录权限不足的失败日志（添加这4行）
                operationLogService.logOperation(
                        (String) loginResult.get("userId"),
                        role,
                        "AUTH",
                        "LOGIN",
                        "管理员登录失败：权限不足",
                        "USER",
                        (String) loginResult.get("userId"),
                        ipAddress,
                        userAgent,
                        0,
                        "权限不足，仅管理员可登录",
                        0
                );

                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "权限不足，仅管理员可登录");
                error.put("data", null);
                return ResponseEntity.status(403).body(error);
            }



            // 添加管理员标识
            loginResult.put("is_admin", true);
            // 记录管理员登录成功日志（添加这4行）
            operationLogService.logOperation(
                    (String) loginResult.get("userId"),
                    role,
                    "AUTH",
                    "LOGIN",
                    "管理员登录成功",
                    "USER",
                    (String) loginResult.get("userId"),
                    ipAddress,
                    userAgent,
                    1,
                    null,
                    0
            );
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "管理员登录成功");
            response.put("data", loginResult);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // 获取客户端信息
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            String safeErrorMsg = getSafeErrorMessage(e);
            // 记录登录失败日志（添加这4行）
            operationLogService.logOperation(
                    null,
                    null,
                    "AUTH",
                    "LOGIN",
                    "管理员登录失败: " ,
                    "USER",
                    null,
                    ipAddress,
                    userAgent,
                    0,
                    safeErrorMsg,
                    0
            );
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
     * 检查用户名是否可用
     */
    @PostMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@Valid @RequestBody CheckUsernameDTO dto) {
        try {
            boolean exists = userService.isUsernameExists(dto.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", exists ? "用户名已存在" : "用户名可用");
            response.put("data", Map.of(
                    "username", dto.getUsername(),
                    "available", !exists,
                    "exists", exists
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 检查手机号是否可用
     */
    @PostMapping("/check-phone")
    public ResponseEntity<Map<String, Object>> checkPhone(@Valid @RequestBody CheckPhoneDTO dto) {
        try {
            boolean exists = userService.isPhoneExists(dto.getPhone());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", exists ? "手机号已注册" : "手机号可用");
            response.put("data", Map.of(
                    "phone", dto.getPhone(),
                    "available", !exists,
                    "exists", exists
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 检查身份证号是否可用
     */
    @PostMapping("/check-id-number")
    public ResponseEntity<Map<String, Object>> checkIdNumber(@Valid @RequestBody CheckIdNumberDTO dto) {
        try {
            boolean exists = userService.isIdNumberExists(dto.getIdNumber());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", exists ? "身份证号已注册" : "身份证号可用");
            response.put("data", Map.of(
                    "idNumber", dto.getIdNumber(),
                    "available", !exists,
                    "exists", exists
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "系统内部错误");
            error.put("data", null);
            return ResponseEntity.internalServerError().body(error);
        }
    }
}