package com.zychen.bank.service;

import com.zychen.bank.dto.ChangePasswordDTO;
import com.zychen.bank.dto.LoginDTO;
import com.zychen.bank.dto.RegisterDTO;
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
}