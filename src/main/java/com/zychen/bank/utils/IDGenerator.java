package com.zychen.bank.utils;

import com.zychen.bank.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IDGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final AtomicInteger userCounter = new AtomicInteger(1);
    @Autowired
    private UserMapper userMapper;

    // 生成普通用户ID：U + 7位数字
    public String generateUserId() {
        String maxUserId = userMapper.findMaxUserId("U%");  // 只查U开头的
        if (maxUserId == null) {
            return "U0000001";
        }

        try {
            int seq = Integer.parseInt(maxUserId.substring(1)) + 1;
            return String.format("U%07d", seq);
        } catch (NumberFormatException e) {
            return "U" + System.currentTimeMillis() % 10000000;
        }
    }

    // 生成管理员ID：ADMIN + 3位数字
    public String generateAdminId() {
        String maxAdminId = userMapper.findMaxUserId("ADMIN%");  // 只查ADMIN开头的
        if (maxAdminId == null) {
            return "ADMIN001";
        }

        try {
            int seq = Integer.parseInt(maxAdminId.substring(5)) + 1;  // 去掉"ADMIN"
            return String.format("ADMIN%03d", seq);
        } catch (NumberFormatException e) {
            return "ADMIN" + (System.currentTimeMillis() % 1000);
        }
    }

    // 生成交易流水号: T + 年月日时分秒(14位) + 4位随机数
    public String generateTransNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 10000);
        return String.format("T%s%04d", timestamp, random);
    }
}