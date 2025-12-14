package com.zychen.bank.service;

import com.zychen.bank.dto.DepositDTO;
import com.zychen.bank.dto.WithdrawDTO;

import java.math.BigDecimal;
import java.util.Map;

public interface TransactionService {

    // 存款
    Map<String, Object> deposit(String userId, DepositDTO depositDTO);

    // 获取银行卡余额
    BigDecimal getCardBalance(String cardId, String userId);

    // 取款
    Map<String, Object> withdraw(String userId, WithdrawDTO withdrawDTO);
}