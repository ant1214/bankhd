package com.zychen.bank.service;

import com.zychen.bank.dto.BindCardDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.mapper.UserInfoMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.model.UserInfo;
import com.zychen.bank.service.BankCardService;
import com.zychen.bank.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class BankCardServiceImpl implements BankCardService {

    @Autowired
    private BankCardMapper bankCardMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Override
    @Transactional
    public BankCard bindCard(String userId, BindCardDTO bindCardDTO) {
        // 1. 验证身份信息
        UserInfo userInfo = userInfoMapper.findByUserId(userId);
        if (userInfo == null) {
            throw new RuntimeException("用户信息不存在");
        }

        // 验证姓名和身份证号
        if (!userInfo.getName().equals(bindCardDTO.getName()) ||
                !userInfo.getIdNumber().equals(bindCardDTO.getIdNumber())) {
            throw new RuntimeException("身份信息不匹配");
        }

        // 2. 检查银行卡是否已存在
        if (isCardExists(bindCardDTO.getCardId())) {
            throw new RuntimeException("银行卡已绑定");
        }

        // 3. 创建银行卡记录
        BankCard bankCard = new BankCard();
        bankCard.setCardId(bindCardDTO.getCardId());
        bankCard.setUserId(userId);
        bankCard.setCardPassword(passwordUtil.encode(bindCardDTO.getCardPassword())); // 加密交易密码
        bankCard.setBalance(BigDecimal.ZERO);
        bankCard.setAvailableBalance(BigDecimal.ZERO);
        bankCard.setFrozenAmount(BigDecimal.ZERO);
        bankCard.setCardType(0);  // 储蓄卡
        bankCard.setStatus(0);    // 正常状态
        bankCard.setBindTime(LocalDateTime.now());
        bankCard.setDailyLimit(new BigDecimal("50000.00"));  // 默认日限额5万
        bankCard.setMonthlyLimit(new BigDecimal("200000.00")); // 默认月限额20万

        // 4. 保存到数据库
        int result = bankCardMapper.insert(bankCard);
        if (result <= 0) {
            throw new RuntimeException("银行卡绑定失败");
        }

        log.info("银行卡绑定成功: 用户={}, 卡号={}", userId, bindCardDTO.getCardId());
        return bankCard;
    }

    @Override
    public List<BankCard> getUserCards(String userId) {
        return bankCardMapper.findByUserId(userId);
    }

    @Override
    public BankCard getCardDetail(String cardId, String userId) {
        BankCard bankCard = bankCardMapper.findByCardId(cardId);
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }
        // 验证银行卡是否属于当前用户
        if (!bankCard.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问此银行卡");
        }
        return bankCard;
    }

    @Override
    public boolean isCardExists(String cardId) {
        return bankCardMapper.countByCardId(cardId) > 0;
    }
}