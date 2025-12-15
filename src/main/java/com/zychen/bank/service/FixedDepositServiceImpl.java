package com.zychen.bank.service;

import com.zychen.bank.dto.FixedDepositDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.mapper.FixedDepositMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.model.FixedDeposit;
import com.zychen.bank.service.FixedDepositService;
import com.zychen.bank.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FixedDepositServiceImpl implements FixedDepositService {
    @Autowired
    private PasswordUtil passwordUtil;
    @Autowired
    private FixedDepositMapper fixedDepositMapper;

    @Autowired
    private BankCardMapper bankCardMapper;

    // 定期存款利率表：期限(月) -> 年利率
    private static final Map<Integer, BigDecimal> INTEREST_RATES = new HashMap<>();
    static {
        INTEREST_RATES.put(3, new BigDecimal("0.010"));   // 3个月，1.0%
        INTEREST_RATES.put(6, new BigDecimal("0.013"));   // 6个月，1.3%
        INTEREST_RATES.put(12, new BigDecimal("0.015"));  // 1年，1.5%
        INTEREST_RATES.put(24, new BigDecimal("0.020"));  // 2年，2.0%
        INTEREST_RATES.put(36, new BigDecimal("0.025"));  // 3年，2.5%
    }

    @Override
    @Transactional
    public FixedDeposit createFixedDeposit(FixedDepositDTO dto, String userId) {
        // 1. 验证银行卡信息
        BankCard bankCard = bankCardMapper.findByCardId(dto.getCardId());
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }

        // 2. 验证银行卡是否属于当前用户
        if (!bankCard.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此银行卡");
        }

        // 3. 验证交易密码
        if (!passwordUtil.matches(dto.getCardPassword(), bankCard.getCardPassword())) {
            throw new RuntimeException("交易密码错误");
        }

        // 4. 验证银行卡状态
        if (bankCard.getStatus() != 0) {
            throw new RuntimeException("银行卡状态异常，无法办理定期存款");
        }

        // 5. 验证余额是否足够
        if (bankCard.getBalance().compareTo(dto.getPrincipal()) < 0) {
            throw new RuntimeException("余额不足");
        }

        // 6. 验证存款期限是否合法
        if (!INTEREST_RATES.containsKey(dto.getTerm())) {
            throw new RuntimeException("存款期限不合法，请选择3、6、12、24、36个月");
        }

        // 7. 从活期账户扣除本金
        BigDecimal newBalance = bankCard.getBalance().subtract(dto.getPrincipal());
        BigDecimal newAvailableBalance = bankCard.getAvailableBalance().subtract(dto.getPrincipal());

        int updateResult = bankCardMapper.updateBalance(
                dto.getCardId(),
                newBalance,
                newAvailableBalance,
                java.time.LocalDateTime.now()
        );

        if (updateResult == 0) {
            throw new RuntimeException("扣除余额失败");
        }

        // 8. 创建定期存款记录
        FixedDeposit fixedDeposit = new FixedDeposit();
        String fdNo = "FD" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", (int)(Math.random() * 10000));
        fixedDeposit.setFdNo(fdNo);
        fixedDeposit.setCardId(dto.getCardId());
        fixedDeposit.setUserId(userId);
        fixedDeposit.setPrincipal(dto.getPrincipal());
        fixedDeposit.setRate(INTEREST_RATES.get(dto.getTerm()));
        fixedDeposit.setTerm(dto.getTerm());
        fixedDeposit.setAutoRenew(dto.getAutoRenew());
        fixedDeposit.setStatus(0); // 进行中

        // 设置开始和结束时间
        Date startTime = new Date();
        fixedDeposit.setStartTime(startTime);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.add(Calendar.MONTH, dto.getTerm());
        fixedDeposit.setEndTime(calendar.getTime());

        fixedDeposit.setCreatedTime(new Date());

        // 9. 保存到数据库
        fixedDepositMapper.insert(fixedDeposit);

        return fixedDeposit;
    }

    @Override
    public List<FixedDeposit> getFixedDepositsByUser(String userId) {
        return fixedDepositMapper.findByUserId(userId);
    }

    @Override
    public FixedDeposit getFixedDepositDetail(Integer fdId, String userId) {
        FixedDeposit fixedDeposit = fixedDepositMapper.findById(fdId);
        if (fixedDeposit == null) {
            throw new RuntimeException("定期存款不存在");
        }

        // 验证权限：只能查看自己的定期存款
        if (!fixedDeposit.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此定期存款");
        }

        return fixedDeposit;
    }

    @Override
    public List<FixedDeposit> getFixedDepositsByCard(String cardId, String userId) {
        // 先验证银行卡是否属于用户
        BankCard bankCard = bankCardMapper.findByCardId(cardId);
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }

        if (!bankCard.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此银行卡的定期存款");
        }

        return fixedDepositMapper.findByCardId(cardId);
    }
}
