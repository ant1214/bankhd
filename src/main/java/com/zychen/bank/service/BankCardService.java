package com.zychen.bank.service;

import com.zychen.bank.dto.BindCardDTO;
import com.zychen.bank.model.BankCard;

import java.util.List;
import java.util.Map;

public interface BankCardService {

    // 绑定银行卡
    BankCard bindCard(String userId, BindCardDTO bindCardDTO);

    // 查询用户的所有银行卡
    List<BankCard> getUserCards(String userId);

    // 查询单张银行卡详情
    BankCard getCardDetail(String cardId, String userId);

    // 检查银行卡是否已存在
    boolean isCardExists(String cardId);
}