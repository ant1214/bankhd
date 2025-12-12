package com.zychen.bank.service;

import com.zychen.bank.dto.LoginDTO;
import com.zychen.bank.dto.RegisterDTO;
import com.zychen.bank.mapper.UserInfoMapper;
import com.zychen.bank.mapper.UserMapper;
import com.zychen.bank.model.User;
import com.zychen.bank.model.UserInfo;
import com.zychen.bank.utils.IDGenerator;
import com.zychen.bank.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

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
    public String login(LoginDTO loginDTO) {
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
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateLastLoginTime(user.getUserId(), user.getLastLoginTime());

        log.info("用户登录成功: {}", user.getUsername());

        // 5. 返回用户ID（稍后我们会添加token）
        return user.getUserId();
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
}