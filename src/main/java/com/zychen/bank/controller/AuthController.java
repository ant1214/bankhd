package com.zychen.bank.controller;

import com.zychen.bank.dto.LoginDTO;
import com.zychen.bank.dto.RegisterDTO;
import com.zychen.bank.model.User;
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
    private UserService userService;
    // 在AuthController类中添加这个方法
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
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
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            Map<String, Object> loginResult = userService.login(loginDTO);

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
    public ResponseEntity<Map<String, Object>> adminLogin(@Valid @RequestBody LoginDTO loginDTO) {
        try {
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
                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "权限不足，仅管理员可登录");
                error.put("data", null);
                return ResponseEntity.status(403).body(error);
            }



            // 添加管理员标识
            loginResult.put("is_admin", true);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "管理员登录成功");
            response.put("data", loginResult);

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
}