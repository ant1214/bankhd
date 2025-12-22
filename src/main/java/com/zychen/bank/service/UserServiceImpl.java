package com.zychen.bank.service;

import com.zychen.bank.dto.*;
import com.zychen.bank.mapper.*;
import com.zychen.bank.model.*;
import com.zychen.bank.utils.IDGenerator;
import com.zychen.bank.utils.JwtUtil;
import com.zychen.bank.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private BankCardMapper bankCardMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private FixedDepositMapper fixedDepositMapper;
    @Override
    public UserStatisticsDTO getUserStatistics(String userId) {
        UserStatisticsDTO statistics = new UserStatisticsDTO();

        // 1. 调用银行卡统计方法
        calculateCardStatistics(userId, statistics);

        // 2. 调用定期存款统计方法
        calculateFixedDepositStatistics(userId, statistics);

        // 3. 调用本月交易统计方法
        calculateMonthTransactionStatistics(userId, statistics);

        return statistics;
    }

    private void calculateCardStatistics(String userId, UserStatisticsDTO statistics) {
        // 查询用户所有银行卡
        List<BankCard> cards = bankCardMapper.findByUserId(userId);

        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal availableBalance = BigDecimal.ZERO;
        BigDecimal frozenAmount = BigDecimal.ZERO;
        int activeCardCount = 0;

        for (BankCard card : cards) {
            // 处理可能的null值
            BigDecimal balance = card.getBalance() != null ? card.getBalance() : BigDecimal.ZERO;
            BigDecimal availBalance = card.getAvailableBalance() != null ? card.getAvailableBalance() : BigDecimal.ZERO;

            totalBalance = totalBalance.add(balance);
            availableBalance = availableBalance.add(availBalance);

            // 🔥 判断银行卡状态
            Integer status = card.getStatus();
            if (status != null) {
                switch (status) {
                    case 0: // 正常
                        activeCardCount++;
                        // 正常卡也可能有部分冻结金额
                        if (card.getFrozenAmount() != null) {
                            frozenAmount = frozenAmount.add(card.getFrozenAmount());
                        }
                        break;
                    case 1: // 挂失
                        // 挂失卡，整个卡余额算作冻结
                        frozenAmount = frozenAmount.add(balance);
                        break;
                    case 2: // 冻结
                        // 冻结卡，整个卡余额算作冻结
                        frozenAmount = frozenAmount.add(balance);
                        break;
                    case 3: // 已注销
                        // 已注销卡，不计入活跃，也不计入冻结
                        break;
                }
            }
        }

        statistics.setTotalBalance(totalBalance);
        statistics.setAvailableBalance(availableBalance);
        statistics.setFrozenAmount(frozenAmount);
        statistics.setCardCount(cards.size());
        statistics.setActiveCardCount(activeCardCount);
    }

    private void calculateFixedDepositStatistics(String userId, UserStatisticsDTO statistics) {
        // 查询用户所有定期存款
        List<FixedDeposit> fixedDeposits = fixedDepositMapper.findByUserId(userId);

        BigDecimal fixedDepositAmount = BigDecimal.ZERO;

        for (FixedDeposit fd : fixedDeposits) {
            // 状态：0=持有中，1=已到期（都应计入总额）
            Integer status = fd.getStatus();
            if (status != null && (status == 0 || status == 1)) {
                BigDecimal principal = fd.getPrincipal() != null ? fd.getPrincipal() : BigDecimal.ZERO;
                fixedDepositAmount = fixedDepositAmount.add(principal);
            }
        }

        statistics.setFixedDepositAmount(fixedDepositAmount);
    }
    private void calculateMonthTransactionStatistics(String userId, UserStatisticsDTO statistics) {
        UserStatisticsDTO.MonthStatistics monthStats = new UserStatisticsDTO.MonthStatistics();

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        List<Transaction> transactions = transactionMapper.findByUserIdAndMonth(userId, currentYear, currentMonth);

        int depositCount = 0;
        BigDecimal depositAmount = BigDecimal.ZERO;
        int withdrawCount = 0;
        BigDecimal withdrawAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;

        for (Transaction trans : transactions) {
            String transType = trans.getTransType();
            BigDecimal amount = trans.getAmount() != null ? trans.getAmount() : BigDecimal.ZERO;

            if ("DEPOSIT".equalsIgnoreCase(transType)) {
                // 存款：金额为正
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    depositCount++;
                    depositAmount = depositAmount.add(amount);
                }
            } else if ("WITHDRAW".equalsIgnoreCase(transType)) {
                // 取款：金额为正
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    withdrawCount++;
                    withdrawAmount = withdrawAmount.add(amount);
                }
            } else if ("TRANSFER".equalsIgnoreCase(transType)) {
                // 转账：金额为负表示转出（算作支出）
                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    withdrawCount++; // 把转账转出算作支出
                    withdrawAmount = withdrawAmount.add(amount.abs()); // 取绝对值加入支出
                }
                // 转账转入（算作收入）
                else if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    depositCount++; // 把转账转入算作收入
                    depositAmount = depositAmount.add(amount);
                }
            } else if ("INTEREST".equalsIgnoreCase(transType)) {
                interestAmount = interestAmount.add(amount);
            }
        }

        monthStats.setDepositCount(depositCount);
        monthStats.setDepositAmount(depositAmount);
        monthStats.setWithdrawCount(withdrawCount);
        monthStats.setWithdrawAmount(withdrawAmount);
        monthStats.setInterestEarned(interestAmount);
        monthStats.setTransactionCount(transactions.size());

        statistics.setThisMonth(monthStats);
    }
//    private void calculateMonthTransactionStatistics(String userId, UserStatisticsDTO statistics) {
//        UserStatisticsDTO.MonthStatistics monthStats = new UserStatisticsDTO.MonthStatistics();
//
//        // 获取当前年月
//        LocalDate now = LocalDate.now();
//        int currentYear = now.getYear();
//        int currentMonth = now.getMonthValue();
//
//        // 查询本月所有成功交易（status=1）
//        List<Transaction> transactions = transactionMapper.findByUserIdAndMonth(userId, currentYear, currentMonth);
//
//        int depositCount = 0;
//        BigDecimal depositAmount = BigDecimal.ZERO;
//        int withdrawCount = 0;
//        BigDecimal withdrawAmount = BigDecimal.ZERO;
//        BigDecimal interestAmount = BigDecimal.ZERO;
//
//        for (Transaction trans : transactions) {
//            String transType = trans.getTransType();
//            BigDecimal amount = trans.getAmount() != null ? trans.getAmount() : BigDecimal.ZERO;
//
//            // 根据数据库字段：DEPOSIT=存款，WITHDRAW=取款，INTEREST=利息
//            if ("DEPOSIT".equalsIgnoreCase(transType)) {
//                depositCount++;
//                depositAmount = depositAmount.add(amount);
//            } else if ("WITHDRAW".equalsIgnoreCase(transType)) {
//                withdrawCount++;
//                withdrawAmount = withdrawAmount.add(amount);
//            } else if ("INTEREST".equalsIgnoreCase(transType)) {
//                interestAmount = interestAmount.add(amount);
//            }
//        }
//
//        monthStats.setDepositCount(depositCount);
//        monthStats.setDepositAmount(depositAmount);
//        monthStats.setWithdrawCount(withdrawCount);
//        monthStats.setWithdrawAmount(withdrawAmount);
//        monthStats.setInterestEarned(interestAmount);
//        monthStats.setTransactionCount(transactions.size());
//
//        statistics.setThisMonth(monthStats);
//    }

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


    @Override
    public Map<String, Object> getUsers(UserQueryDTO queryDTO) {
        // 1. 验证分页参数
        if (queryDTO.getPage() == null || queryDTO.getPage() < 1) {
            queryDTO.setPage(1);
        }
        if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(20);
        }

        // 2. 计算分页偏移量
        int offset = (queryDTO.getPage() - 1) * queryDTO.getPageSize();

        // 3. 查询用户列表
        List<User> users = userMapper.findUsers(
                queryDTO.getSearch(),
                queryDTO.getRole(),
                queryDTO.getAccountStatus(),
                offset,
                queryDTO.getPageSize()
        );

        // 4. 查询用户总数
        int total = userMapper.countUsers(
                queryDTO.getSearch(),
                queryDTO.getRole(),
                queryDTO.getAccountStatus()
        );

        // 5. 查询每个用户的银行卡统计信息
        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> userInfo = new HashMap<>();

            // 用户基本信息
            userInfo.put("userId", user.getUserId());
            userInfo.put("username", user.getUsername());
            userInfo.put("phone", user.getPhone());
            userInfo.put("role", user.getRole());
            userInfo.put("accountStatus", user.getAccountStatus());
            userInfo.put("createdTime", user.getCreatedTime());
            userInfo.put("lastLoginTime", user.getLastLoginTime());

            // 查询用户详细信息
            UserInfo userDetail = userInfoMapper.findByUserId(user.getUserId());
            if (userDetail != null) {
                userInfo.put("name", userDetail.getName());
                userInfo.put("idNumber", maskIdNumber(userDetail.getIdNumber()));
            }

            // 查询银行卡统计
            List<BankCard> cards = bankCardMapper.findByUserId(user.getUserId());
            int activeCardCount = 0;
            BigDecimal totalBalance = BigDecimal.ZERO;

            for (BankCard card : cards) {
                if (card.getStatus() == 0) {  // 正常状态的卡
                    activeCardCount++;
                    totalBalance = totalBalance.add(card.getBalance());
                }
            }

            userInfo.put("cardCount", cards.size());
            userInfo.put("activeCardCount", activeCardCount);
            userInfo.put("totalBalance", totalBalance);

            userList.add(userInfo);
        }

        // 6. 计算总页数
        int totalPages = (int) Math.ceil((double) total / queryDTO.getPageSize());

        // 7. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("users", userList);
        result.put("pagination", Map.of(
                "page", queryDTO.getPage(),
                "pageSize", queryDTO.getPageSize(),
                "total", total,
                "totalPages", totalPages
        ));

        return result;
    }

    @Override
    public Map<String, Object> getUserCards(String userId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 验证用户是否存在
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 查询用户信息
        UserInfo userInfo = userInfoMapper.findByUserId(userId);

        // 3. 查询用户的所有银行卡
        List<BankCard> cards = bankCardMapper.findByUserId(userId);

        // 4. 构建响应
        List<Map<String, Object>> cardList = new ArrayList<>();
        for (BankCard card : cards) {
            Map<String, Object> cardMap = new HashMap<>();
            cardMap.put("cardId", card.getCardId());
            cardMap.put("balance", card.getBalance());
            cardMap.put("availableBalance", card.getAvailableBalance());
            cardMap.put("frozenAmount", card.getFrozenAmount());
            cardMap.put("status", card.getStatus());
            cardMap.put("statusText", getCardStatusText(card.getStatus()));
            cardMap.put("cardType", card.getCardType());
            cardMap.put("bindTime", card.getBindTime());
            cardMap.put("lastTransactionTime", card.getLastTransactionTime());
            cardMap.put("dailyLimit", card.getDailyLimit());
            cardMap.put("monthlyLimit", card.getMonthlyLimit());

            cardList.add(cardMap);
        }

        // 5. 返回用户信息和银行卡列表
        result.put("user", Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "name", userInfo.getName(),
                "phone", user.getPhone(),
                "idNumber", maskIdNumber(userInfo.getIdNumber()),
                "accountStatus", user.getAccountStatus()
        ));

        result.put("cards", cardList);
        result.put("total", cardList.size());

        return result;
    }



    private String getCardStatusText(Integer status) {
        switch (status) {
            case 0: return "正常";
            case 1: return "挂失";
            case 2: return "冻结";
            case 3: return "已注销";
            default: return "未知";
        }
    }

    @Override
    public Integer getUserRole(String userId) {
        User user = userMapper.findByUserId(userId);
        return user != null ? user.getRole() : null;
    }

    @Override
    @Transactional
    public void resetUserPassword(String adminId, String targetUserId, String reason) {
        // 1. 验证管理员权限（确保是管理员操作）
        User admin = userMapper.findByUserId(adminId);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        if (admin.getRole() != 1) {
            throw new RuntimeException("无权执行此操作");
        }

        // 2. 验证目标用户是否存在
        User targetUser = userMapper.findByUserId(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("目标用户不存在");
        }

        // 3. 验证目标用户不是管理员（不能重置管理员密码）
        if (targetUser.getRole() == 1) {
            throw new RuntimeException("不能重置管理员的密码");
        }

        // 4. 重置密码为 "123456"（BCrypt加密）
        String encryptedPassword = passwordUtil.encode("123456");
        int result = userMapper.updatePassword(targetUserId, encryptedPassword);

        if (result == 0) {
            throw new RuntimeException("重置密码失败");
        }

        // 5. 记录操作日志（通过日志服务）
        log.info("管理员 {} 重置用户 {} 的密码，原因：{}", adminId, targetUserId, reason);
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            log.info("开始获取仪表盘统计数据");

            // 1. 用户统计
            stats.put("totalUsers", userMapper.countTotalUsers());
            stats.put("activeUsers", userMapper.countActiveUsers());
            stats.put("frozenUsers", userMapper.countFrozenUsers());
            stats.put("newUsersToday", userMapper.countNewUsersToday());

            // 2. 银行卡统计
            stats.put("totalCards", bankCardMapper.countTotalCards());
            stats.put("activeCards", bankCardMapper.countCardsByStatus(0));
            stats.put("frozenCards", bankCardMapper.countCardsByStatus(2));
            stats.put("lostCards", bankCardMapper.countCardsByStatus(1));

            // 3. 交易统计
            stats.put("totalTransactions", transactionMapper.countTotalTransactions());
            stats.put("todayTransactions", transactionMapper.countTodayTransactions());
            stats.put("pendingTransactions", transactionMapper.countPendingTransactions());

            // 4. 资金统计
            stats.put("totalBalance", bankCardMapper.getTotalBalance());
            stats.put("todayIncome", transactionMapper.getTodayIncome());
            stats.put("todayOutcome", transactionMapper.getTodayOutcome());
            stats.put("fixedDepositTotal", fixedDepositMapper.getTotalFixedDepositAmount());

            // 5. 定期存款统计
            stats.put("activeFixedDeposits", fixedDepositMapper.countByStatus(0));
            stats.put("maturedFixedDeposits", fixedDepositMapper.countByStatus(1));

            // 6. 系统状态（简单判断）
            long frozenCount = (Long) stats.get("frozenUsers") + (Long) stats.get("frozenCards");
            String systemStatus = "健康";
            if (frozenCount > 10) {
                systemStatus = "警告";
            } else if (frozenCount > 20) {
                systemStatus = "危险";
            }
            stats.put("systemStatus", systemStatus);

            // 7. 安全等级（简单判断）
            String securityLevel = "高";
            Long todayTransactions = (Long) stats.get("todayTransactions");
            if (todayTransactions > 1000) {
                securityLevel = "中";
            } else if (todayTransactions > 5000) {
                securityLevel = "低";
            }
            stats.put("securityLevel", securityLevel);

            // 8. 最近注册用户（前5个）
            try {
                List<Map<String, Object>> recentUsers = userMapper.getRecentUsers(5);
                stats.put("recentUsers", recentUsers);
                log.info("获取到最近用户数据: {} 条", recentUsers.size());
            } catch (Exception e) {
                log.warn("获取最近用户数据失败: {}", e.getMessage());
                stats.put("recentUsers", new ArrayList<>());
            }

            // 9. 系统告警（暂时为空）
            stats.put("systemAlerts", new ArrayList<>());

            log.info("仪表盘统计数据获取完成，共 {} 项", stats.size());

        } catch (Exception e) {
            log.error("获取仪表盘统计失败", e);
            // 打印具体哪个方法出错
            log.error("错误详情: ", e);
            // 返回错误信息而不是抛出异常
            stats.put("error", true);
            stats.put("message", "统计服务暂时不可用: " + e.getMessage());
            // 重新抛出异常，让Controller处理
            throw new RuntimeException("获取统计信息失败: " + e.getMessage(), e);
        }

        return stats;
    }

    @Override
    public Map<String, Object> getAllCards(String search, String status, Integer page, Integer pageSize) {
        // 1. 验证分页参数
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 2. 计算分页偏移量
        int offset = (page - 1) * pageSize;

        // 3. 转换状态参数
        Integer statusInt = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusInt = Integer.parseInt(status);
            } catch (NumberFormatException e) {
                // 如果状态不是数字，就按null处理
                log.warn("状态参数格式错误: {}", status);
            }
        }

        // 4. 查询银行卡列表
        List<Map<String, Object>> cardList = bankCardMapper.findAllCards(search, statusInt, offset, pageSize);

        // 5. 补充用户信息（确保每个银行卡都有用户信息）
        for (Map<String, Object> card : cardList) {
            // 如果没有user_name，尝试查询用户信息
            if (card.get("user_name") == null) {
                String userId = (String) card.get("user_id");
                if (userId != null) {
                    User user = userMapper.findByUserId(userId);
                    UserInfo userInfo = userInfoMapper.findByUserId(userId);
                    if (user != null) {
                        card.put("user_name", user.getUsername());
                    }
                    if (userInfo != null) {
                        card.put("name", userInfo.getName());
                    }
                }
            }

            // 添加状态文本
            Integer cardStatus = (Integer) card.get("status");
            card.put("status_text", getCardStatusText(cardStatus));
        }

        // 6. 查询银行卡总数
        int total = bankCardMapper.countAllCards(search, statusInt);

        // 7. 计算总页数
        int totalPages = (int) Math.ceil((double) total / pageSize);

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("cards", cardList);
        result.put("pagination", Map.of(
                "page", page,
                "pageSize", pageSize,
                "total", total,
                "totalPages", totalPages
        ));

        return result;
    }
}