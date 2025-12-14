package com.zychen.bank.service;

import com.zychen.bank.dto.DepositDTO;
import com.zychen.bank.dto.WithdrawDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.mapper.TransactionMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.model.Transaction;
import com.zychen.bank.service.TransactionService;
import com.zychen.bank.utils.IDGenerator;
import com.zychen.bank.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {
    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private BankCardMapper bankCardMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private IDGenerator idGenerator;

    @Override
    @Transactional
    public Map<String, Object> deposit(String userId, DepositDTO depositDTO) {
        // 1. 验证银行卡
        BankCard bankCard = bankCardMapper.findByCardId(depositDTO.getCardId());
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }
        if (!bankCard.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此银行卡");
        }
        if (bankCard.getStatus() != 0) {
            throw new RuntimeException("银行卡状态异常，无法存款");
        }

        // 2. 验证金额
        if (depositDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("存款金额必须大于0");
        }

        // 3. 计算新余额
        BigDecimal newBalance = bankCard.getBalance().add(depositDTO.getAmount());
        BigDecimal newAvailableBalance = bankCard.getAvailableBalance().add(depositDTO.getAmount());

        // 4. 更新银行卡余额
        LocalDateTime now = LocalDateTime.now();
        int updateResult = bankCardMapper.updateBalance(
                depositDTO.getCardId(),
                newBalance,
                newAvailableBalance,
                now
        );
        if (updateResult <= 0) {
            throw new RuntimeException("更新余额失败");
        }

        // 5. 记录交易流水
        Transaction transaction = new Transaction();
        transaction.setTransNo(idGenerator.generateTransNo());
        transaction.setCardId(depositDTO.getCardId());
        transaction.setUserId(userId);
        transaction.setTransType("DEPOSIT");
        transaction.setTransSubtype("CURRENT_DEPOSIT");
        transaction.setAmount(depositDTO.getAmount());
        transaction.setBalanceBefore(bankCard.getBalance());
        transaction.setBalanceAfter(newBalance);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setCurrency("CNY");
        transaction.setStatus(1);
        transaction.setRemark(depositDTO.getRemark());
        transaction.setOperatorId(userId);
        transaction.setOperatorType("USER");
        transaction.setTransTime(now);
        transaction.setCompletedTime(now);

        int insertResult = transactionMapper.insert(transaction);
        if (insertResult <= 0) {
            throw new RuntimeException("记录交易流水失败");
        }

        // 6. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("transNo", transaction.getTransNo());
        result.put("cardId", depositDTO.getCardId());
        result.put("amount", depositDTO.getAmount());
        result.put("balanceBefore", bankCard.getBalance());
        result.put("balanceAfter", newBalance);
        result.put("transactionTime", now);

        log.info("存款成功: 用户={}, 卡号={}, 金额={}, 流水号={}",
                userId, depositDTO.getCardId(), depositDTO.getAmount(), transaction.getTransNo());

        return result;
    }

    @Override
    public BigDecimal getCardBalance(String cardId, String userId) {
        BankCard bankCard = bankCardMapper.findByCardId(cardId);
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }
        if (!bankCard.getUserId().equals(userId)) {
            throw new RuntimeException("无权查询此银行卡余额");
        }
        return bankCard.getBalance();
    }

    @Override
    @Transactional
    public Map<String, Object> withdraw(String userId, WithdrawDTO withdrawDTO) {
        // 1. 验证银行卡
        BankCard bankCard = bankCardMapper.findByCardId(withdrawDTO.getCardId());
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }
        if (!bankCard.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此银行卡");
        }
        if (bankCard.getStatus() != 0) {
            throw new RuntimeException("银行卡状态异常，无法取款");
        }

        // 2. 验证交易密码
        if (!passwordUtil.matches(withdrawDTO.getCardPassword(), bankCard.getCardPassword())) {
            throw new RuntimeException("交易密码错误");
        }

        // 3. 验证取款金额
        if (withdrawDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("取款金额必须大于0");
        }

        // 4. 检查余额是否足够
        if (withdrawDTO.getAmount().compareTo(bankCard.getAvailableBalance()) > 0) {
            throw new RuntimeException("余额不足");
        }

        // 5. 检查日限额和月限额（简化版，先不实现）

        // 6. 计算新余额
        BigDecimal newBalance = bankCard.getBalance().subtract(withdrawDTO.getAmount());
        BigDecimal newAvailableBalance = bankCard.getAvailableBalance().subtract(withdrawDTO.getAmount());

        // 7. 更新银行卡余额
        LocalDateTime now = LocalDateTime.now();
        int updateResult = bankCardMapper.updateBalance(
                withdrawDTO.getCardId(),
                newBalance,
                newAvailableBalance,
                now
        );
        if (updateResult <= 0) {
            throw new RuntimeException("更新余额失败");
        }

        // 8. 记录交易流水
        Transaction transaction = new Transaction();
        transaction.setTransNo(idGenerator.generateTransNo());
        transaction.setCardId(withdrawDTO.getCardId());
        transaction.setUserId(userId);
        transaction.setTransType("WITHDRAW");
        transaction.setTransSubtype("CURRENT_WITHDRAW");
        transaction.setAmount(withdrawDTO.getAmount());
        transaction.setBalanceBefore(bankCard.getBalance());
        transaction.setBalanceAfter(newBalance);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setCurrency("CNY");
        transaction.setStatus(1);
        transaction.setRemark(withdrawDTO.getRemark());
        transaction.setOperatorId(userId);
        transaction.setOperatorType("USER");
        transaction.setTransTime(now);
        transaction.setCompletedTime(now);

        int insertResult = transactionMapper.insert(transaction);
        if (insertResult <= 0) {
            throw new RuntimeException("记录交易流水失败");
        }

        // 9. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("transNo", transaction.getTransNo());
        result.put("cardId", withdrawDTO.getCardId());
        result.put("amount", withdrawDTO.getAmount());
        result.put("balanceBefore", bankCard.getBalance());
        result.put("balanceAfter", newBalance);
        result.put("transactionTime", now);

        log.info("取款成功: 用户={}, 卡号={}, 金额={}, 流水号={}",
                userId, withdrawDTO.getCardId(), withdrawDTO.getAmount(), transaction.getTransNo());

        return result;
    }
}