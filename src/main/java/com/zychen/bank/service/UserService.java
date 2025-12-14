package com.zychen.bank.service;

import com.zychen.bank.dto.ChangePasswordDTO;
import com.zychen.bank.dto.LoginDTO;
import com.zychen.bank.dto.RegisterDTO;
import com.zychen.bank.model.User;

import java.util.Map;

public interface UserService {

    // 用户注册
    User register(RegisterDTO registerDTO);

    // 用户登录
    Map<String, Object> login(LoginDTO loginDTO);

    // 根据账号（用户名或手机号）查找用户
    User findByAccount(String account);

    // 检查用户名是否已存在
    boolean isUsernameExists(String username);

    // 检查手机号是否已存在
    boolean isPhoneExists(String phone);

    // 检查身份证号是否已存在
    boolean isIdNumberExists(String idNumber);

    User findByUserId(String userId);

    // 获取用户完整信息（包括user_info）
    Map<String, Object> getUserFullInfo(String userId);

    // 修改密码
    void changePassword(String userId, ChangePasswordDTO changePasswordDTO);
}
