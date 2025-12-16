package com.zychen.bank.controller;

import com.zychen.bank.dto.AddAdminDTO;
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
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

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
                Map<String, Object> error = new HashMap<>();
                error.put("code", 403);
                error.put("message", "权限不足，仅管理员可操作");
                error.put("data", null);
                return ResponseEntity.status(403).body(error);
            }

            // 调用Service添加管理员
            Map<String, Object> result = userService.addAdmin(addAdminDTO, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "管理员账号创建成功");
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
}