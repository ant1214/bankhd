package com.zychen.bank.utils;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IDGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final AtomicInteger userCounter = new AtomicInteger(1);

    // 生成8位用户ID：U + 7位数字
    public String generateUserId() {
        int seq = userCounter.getAndIncrement();
        return String.format("U%07d", seq);  // 例如: U0000001, U0000002, U0000003
    }

    // 生成交易流水号: T + 年月日时分秒(14位) + 4位随机数
    public String generateTransNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 10000);
        return String.format("T%s%04d", timestamp, random);
    }
}