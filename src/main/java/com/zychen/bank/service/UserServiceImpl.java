package com.zychen.bank.service;

import com.zychen.bank.dto.*;
import com.zychen.bank.mapper.UserInfoMapper;
import com.zychen.bank.mapper.UserMapper;
import com.zychen.bank.model.User;
import com.zychen.bank.model.UserInfo;
import com.zychen.bank.utils.IDGenerator;
import com.zychen.bank.utils.JwtUtil;
import com.zychen.bank.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {


    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private IDGenerator idGenerator;

    @Override
    @Transactional
    public User register(RegisterDTO registerDTO) {
        // 1. 检查用户名是否已存在
        if (isUsernameExists(registerDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 检查手机号是否已存在
        if (isPhoneExists(registerDTO.getPhone())) {
            throw new RuntimeException("手机号已注册");
        }

        // 3. 检查身份证号是否已存在
        if (isIdNumberExists(registerDTO.getIdNumber())) {
            throw new RuntimeException("身份证号已注册");
        }

        // 4. 生成用户ID
        String userId = idGenerator.generateUserId();

        // 5. 创建用户
        User user = new User();
        user.setUserId(userId);
        user.setUsername(registerDTO.getUsername());
        user.setPhone(registerDTO.getPhone());
        user.setPassword(passwordUtil.encode(registerDTO.getPassword()));
        user.setRole(0);  // 普通用户
        user.setAccountStatus(0);  // 正常状态
        user.setCreatedTime(LocalDateTime.now());

        // 6. 保存用户
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("用户注册失败");
        }

        // 7. 创建用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setName(registerDTO.getName());
        userInfo.setIdNumber(registerDTO.getIdNumber());
        userInfo.setGender(registerDTO.getGender());
        userInfo.setEmail(registerDTO.getEmail());
        userInfo.setAddress(registerDTO.getAddress());
        userInfo.setUpdatedTime(LocalDateTime.now());

        userInfoMapper.insert(userInfo);

        log.info("用户注册成功: {}, ID: {}", registerDTO.getUsername(), userId);
        return user;
    }

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        // 1. 查找用户
        User user = findByAccount(loginDTO.getAccount());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 检查账户状态
        if (user.getAccountStatus() == 1) {
            throw new RuntimeException("账户已被冻结");
        }

        // 3. 验证密码
        if (!passwordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 4. 更新最后登录时间
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginTime(now);
        userMapper.updateLastLoginTime(user.getUserId(), now);

        // 5. 生成JWT token
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole());

        // 6. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("token", token);
        result.put("role", user.getRole());
        result.put("lastLoginTime", now);

        log.info("用户登录成功: {}, token生成", user.getUsername());
        return result;
    }

    @Override
    public User findByAccount(String account) {
        // 先尝试按用户名查找
        User user = userMapper.findByUsername(account);
        if (user == null) {
            // 再尝试按手机号查找
            user = userMapper.findByPhone(account);
        }
        return user;
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userMapper.findByUsername(username) != null;
    }

    @Override
    public boolean isPhoneExists(String phone) {
        return userMapper.findByPhone(phone) != null;
    }

    @Override
    public boolean isIdNumberExists(String idNumber) {
        return userInfoMapper.findByIdNumber(idNumber) != null;
    }

    @Override
    public User findByUserId(String userId) {
        return userMapper.findByUserId(userId);
    }


    @Override
    public Map<String, Object> getUserFullInfo(String userId) {
        // 1. 获取用户基本信息
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 获取用户详细信息
        UserInfo userInfo = userInfoMapper.findByUserId(userId);

        // 3. 合并信息
        Map<String, Object> result = new HashMap<>();

        // 用户表信息
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        result.put("role", user.getRole());
        result.put("accountStatus", user.getAccountStatus());
        result.put("createdTime", user.getCreatedTime());
        result.put("lastLoginTime", user.getLastLoginTime());

        // 用户信息表信息
        if (userInfo != null) {
            result.put("name", userInfo.getName());
            result.put("idNumber", maskIdNumber(userInfo.getIdNumber())); // 身份证号脱敏
            result.put("gender", userInfo.getGender());
            result.put("email", userInfo.getEmail());
            result.put("address", userInfo.getAddress());
        }

        return result;
    }

    // 身份证号脱敏：110101********1234
    private String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() != 18) {
            return idNumber;
        }
        return idNumber.substring(0, 6) + "********" + idNumber.substring(14);
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordDTO changePasswordDTO) {
        // 1. 验证两次输入的新密码是否一致
        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            throw new RuntimeException("新密码与确认密码不一致");
        }

        // 2. 查询用户
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 验证原密码
        if (!passwordUtil.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 4. 验证新旧密码是否相同
        if (changePasswordDTO.getOldPassword().equals(changePasswordDTO.getNewPassword())) {
            throw new RuntimeException("新密码不能与原密码相同");
        }

        // 5. 加密新密码
        String newEncodedPassword = passwordUtil.encode(changePasswordDTO.getNewPassword());

        // 6. 更新密码
        // 需要先在UserMapper中添加更新密码的方法
        int result = userMapper.updatePassword(userId, newEncodedPassword);
        if (result <= 0) {
            throw new RuntimeException("密码更新失败");
        }

        log.info("用户修改密码成功: {}", userId);
    }


    @Override
    @Transactional
    public Map<String, Object> addAdmin(AddAdminDTO addAdminDTO, String operatorId) {
        // 1. 检查用户名是否已存在
        if (isUsernameExists(addAdminDTO.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 检查手机号是否已存在
        if (isPhoneExists(addAdminDTO.getPhone())) {
            throw new RuntimeException("手机号已注册");
        }

        // 3. 生成用户ID
        String userId = idGenerator.generateAdminId();

        // 4. 创建管理员用户
        User user = new User();
        user.setUserId(userId);
        user.setUsername(addAdminDTO.getUsername());
        user.setPhone(addAdminDTO.getPhone());
        user.setPassword(passwordUtil.encode(addAdminDTO.getPassword()));
        user.setRole(1);  // 管理员角色
        user.setAccountStatus(0);  // 正常状态
        user.setCreatedTime(LocalDateTime.now());

        // 5. 保存用户
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("创建管理员账号失败");
        }

        // 6. 创建用户信息（可选）
        if (addAdminDTO.getName() != null) {
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(userId);
            userInfo.setName(addAdminDTO.getName());
            userInfo.setIdNumber(addAdminDTO.getIdNumber());
            userInfo.setUpdatedTime(LocalDateTime.now());

            userInfoMapper.insert(userInfo);
        }

        // 7. 记录操作日志（可选，可以先跳过）

        // 8. 返回结果
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("user_id", userId);
        resultMap.put("username", addAdminDTO.getUsername());
        resultMap.put("phone", addAdminDTO.getPhone());
        resultMap.put("role", 1);
        resultMap.put("created_by", operatorId);
        resultMap.put("created_time", LocalDateTime.now());

        log.info("管理员添加新管理员成功: 操作者={}, 新管理员={}", operatorId, addAdminDTO.getUsername());

        return resultMap;
    }


    @Override
    @Transactional
    public Map<String, Object> updateUserInfo(String userId, UpdateUserInfoDTO updateDTO) {
        // 1. 验证用户存在
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 如果要更新手机号，检查手机号是否已存在（排除自己）
        if (updateDTO.getPhone() != null && !updateDTO.getPhone().equals(user.getPhone())) {
            User existingUser = userMapper.findByPhone(updateDTO.getPhone());
            if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                throw new RuntimeException("手机号已被其他用户使用");
            }
        }

        // 3. 更新用户表（user）
        boolean userUpdated = false;
        if (updateDTO.getPhone() != null) {
            userMapper.updatePhone(userId, updateDTO.getPhone());
            userUpdated = true;
        }

        // 4. 更新用户信息表（user_info）
        boolean userInfoUpdated = false;
        UserInfo userInfo = userInfoMapper.findByUserId(userId);

        if (userInfo == null) {
            // 如果用户信息不存在，创建新的
            userInfo = new UserInfo();
            userInfo.setUserId(userId);
            userInfo.setName(updateDTO.getName() != null ? updateDTO.getName() : "");
            userInfo.setIdNumber("");  // 身份证号不能为空，但这里可能还没有
            userInfo.setGender(updateDTO.getGender());
            if (updateDTO.getBirthDate() != null) {
                userInfo.setBirthDate(updateDTO.getBirthDate());  // LocalDate类型
                userInfoUpdated = true;
            }
            userInfo.setEmail(updateDTO.getEmail());
            userInfo.setAddress(updateDTO.getAddress());
            userInfo.setUpdatedTime(LocalDateTime.now());

            userInfoMapper.insert(userInfo);
            userInfoUpdated = true;
        } else {
            // 更新现有用户信息
            if (updateDTO.getName() != null && !updateDTO.getName().equals(userInfo.getName())) {
                userInfo.setName(updateDTO.getName());
                userInfoUpdated = true;
            }
            if (updateDTO.getGender() != null && !updateDTO.getGender().equals(userInfo.getGender())) {
                userInfo.setGender(updateDTO.getGender());
                userInfoUpdated = true;
            }
            if (updateDTO.getBirthDate() != null && !updateDTO.getBirthDate().equals(userInfo.getBirthDate())) {
                userInfo.setBirthDate(updateDTO.getBirthDate());
                userInfoUpdated = true;
            }
            if (updateDTO.getEmail() != null && !updateDTO.getEmail().equals(userInfo.getEmail())) {
                userInfo.setEmail(updateDTO.getEmail());
                userInfoUpdated = true;
            }
            if (updateDTO.getAddress() != null && !updateDTO.getAddress().equals(userInfo.getAddress())) {
                userInfo.setAddress(updateDTO.getAddress());
                userInfoUpdated = true;
            }

            if (userInfoUpdated) {
                userInfo.setUpdatedTime(LocalDateTime.now());
                userInfoMapper.update(userInfo);
            }
        }

        // 5. 返回更新结果
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("updatedFields", new HashMap<>());

        Map<String, Object> updatedFields = (Map<String, Object>) result.get("updatedFields");
        if (userUpdated) {
            updatedFields.put("phone", updateDTO.getPhone());
        }
        if (userInfoUpdated) {
            if (updateDTO.getName() != null) updatedFields.put("name", updateDTO.getName());
            if (updateDTO.getGender() != null) updatedFields.put("gender", updateDTO.getGender());
            if (updateDTO.getBirthDate() != null) updatedFields.put("birthDate", updateDTO.getBirthDate());
            if (updateDTO.getEmail() != null) updatedFields.put("email", updateDTO.getEmail());
            if (updateDTO.getAddress() != null) updatedFields.put("address", updateDTO.getAddress());
        }

        result.put("updateTime", LocalDateTime.now());

        log.info("用户信息更新成功: userId={}, 更新字段={}", userId, updatedFields.keySet());

        return result;
    }
}