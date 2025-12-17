package com.zychen.bank.service;

import com.zychen.bank.dto.*;
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

    Integer getUserRole(String userId);
    // 修改密码
    void changePassword(String userId, ChangePasswordDTO changePasswordDTO);

    /**
     * 管理员添加新管理员
     */
    Map<String, Object> addAdmin(AddAdminDTO addAdminDTO, String operatorId);

    /**
     * 更新用户信息
     */
    Map<String, Object> updateUserInfo(String userId, UpdateUserInfoDTO updateDTO);


    /**
     * 查询所有用户（管理员用）
     */
    Map<String, Object> getUsers(UserQueryDTO queryDTO);

    /**
     * 获取用户的所有银行卡（管理员用）
     */
    Map<String, Object> getUserCards(String userId);
    /**
     * 获取用户统计信息
     * @param userId 用户ID
     * @return 用户统计信息
     */
    UserStatisticsDTO getUserStatistics(String userId);

    /**
     * 管理员重置用户密码
     * @param adminId 管理员ID
     * @param targetUserId 目标用户ID
     * @param reason 重置原因
     */
    void resetUserPassword(String adminId, String targetUserId, String reason);

}
