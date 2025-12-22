package com.zychen.bank.service;

import com.zychen.bank.dto.FixedDepositDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.mapper.FixedDepositMapper;
import com.zychen.bank.mapper.TransactionMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.model.FixedDeposit;
import com.zychen.bank.model.Transaction;
import com.zychen.bank.service.FixedDepositService;
import com.zychen.bank.utils.IDGenerator;
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
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
@Service
public class FixedDepositServiceImpl implements FixedDepositService {
    @Autowired
    private PasswordUtil passwordUtil;
    @Autowired
    private FixedDepositMapper fixedDepositMapper;

    @Autowired
    private BankCardMapper bankCardMapper;

    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private InterestRateService interestRateService;
    @Autowired
    private IDGenerator idGenerator;
    // 添加这个辅助方法
    private boolean isValidTerm(Integer term) {
        if (term == null) return false;
        int[] validTerms = {3, 6, 12, 24, 36};
        for (int validTerm : validTerms) {
            if (term == validTerm) {
                return true;
            }
        }
        return false;
    }
    // 定期存款利率表：期限(月) -> 年利率
//    private static final Map<Integer, BigDecimal> INTEREST_RATES = new HashMap<>();
//    static {
//        INTEREST_RATES.put(3, new BigDecimal("0.010"));   // 3个月，1.0%
//        INTEREST_RATES.put(6, new BigDecimal("0.013"));   // 6个月，1.3%
//        INTEREST_RATES.put(12, new BigDecimal("0.015"));  // 1年，1.5%
//        INTEREST_RATES.put(24, new BigDecimal("0.020"));  // 2年，2.0%
//        INTEREST_RATES.put(36, new BigDecimal("0.025"));  // 3年，2.5%
//    }

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
        // 获取当前余额（扣除前）
        BigDecimal currentBalance = bankCard.getBalance();
        BigDecimal currentAvailableBalance = bankCard.getAvailableBalance();

//        // 6. 验证存款期限是否合法
//        if (!INTEREST_RATES.containsKey(dto.getTerm())) {
//            throw new RuntimeException("存款期限不合法，请选择3、6、12、24、36个月");
//        }
        // 6. 验证存款期限是否合法（修改这里）
        // 原来：if (!INTEREST_RATES.containsKey(dto.getTerm()))
        // 现在：检查是否支持的期限
        if (!isValidTerm(dto.getTerm())) {
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

//        // 8. 创建定期存款记录
//        FixedDeposit fixedDeposit = new FixedDeposit();
//        String fdNo = "FD" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
//                + String.format("%04d", (int)(Math.random() * 10000));
//        fixedDeposit.setFdNo(fdNo);
//        fixedDeposit.setCardId(dto.getCardId());
//        fixedDeposit.setUserId(userId);
//        fixedDeposit.setPrincipal(dto.getPrincipal());
//        fixedDeposit.setRate(INTEREST_RATES.get(dto.getTerm()));
//        fixedDeposit.setTerm(dto.getTerm());
//        fixedDeposit.setAutoRenew(dto.getAutoRenew());
//        fixedDeposit.setStatus(0); // 进行中
        // ✅ 8. 记录定期存款存入交易（新增代码）
        Transaction depositTransaction = new Transaction();
        depositTransaction.setTransNo(idGenerator.generateTransNo());
        depositTransaction.setCardId(dto.getCardId());
        depositTransaction.setUserId(userId);
        depositTransaction.setTransType("TRANSFER");
        depositTransaction.setTransSubtype("FIXED_DEPOSIT_IN");
        depositTransaction.setAmount(dto.getPrincipal().negate()); // 负值表示转出
        depositTransaction.setBalanceBefore(currentBalance);
        depositTransaction.setBalanceAfter(newBalance);
        depositTransaction.setFee(BigDecimal.ZERO);
        depositTransaction.setCurrency("CNY");
        depositTransaction.setStatus(1);
        depositTransaction.setRemark("定期存款存入");
        depositTransaction.setOperatorId(userId);
        depositTransaction.setOperatorType("USER");
        depositTransaction.setTransTime(java.time.LocalDateTime.now());
        depositTransaction.setCompletedTime(java.time.LocalDateTime.now());

        transactionMapper.insert(depositTransaction);
        // 8. 创建定期存款记录 - 修改利率获取方式
        FixedDeposit fixedDeposit = new FixedDeposit();
        String fdNo = "FD" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", (int)(Math.random() * 10000));
        fixedDeposit.setFdNo(fdNo);
        fixedDeposit.setCardId(dto.getCardId());
        fixedDeposit.setUserId(userId);
        fixedDeposit.setPrincipal(dto.getPrincipal());

        // 修改这里：从利率服务获取利率
        // 原来：fixedDeposit.setRate(INTEREST_RATES.get(dto.getTerm()));
        BigDecimal annualRate = interestRateService.getRateByTerm(dto.getTerm());
        fixedDeposit.setRate(annualRate);

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


    @Override
    @Transactional
    public Map<String, Object> earlyWithdraw(Integer fdId, String userId, String cardPassword) {
        // 1. 查询定期存款
        FixedDeposit fixedDeposit = fixedDepositMapper.findById(fdId);
        if (fixedDeposit == null) {
            throw new RuntimeException("定期存款不存在");
        }

        // 2. 验证权限
        if (!fixedDeposit.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此定期存款");
        }

        // 3. 验证状态
        if (fixedDeposit.getStatus() != 0) {
            throw new RuntimeException("定期存款状态异常，无法提前支取");
        }

        // 4. 查询银行卡
        BankCard bankCard = bankCardMapper.findByCardId(fixedDeposit.getCardId());
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }

        // 5. 验证交易密码
        if (!passwordUtil.matches(cardPassword, bankCard.getCardPassword())) {
            throw new RuntimeException("交易密码错误");
        }

        // 6. 计算持有天数
        Date startDate = fixedDeposit.getStartTime();
        Date currentDate = new Date();
        long diffInMillies = Math.abs(currentDate.getTime() - startDate.getTime());
        int heldDays = (int) (diffInMillies / (1000 * 60 * 60 * 24));

        if (heldDays <= 0) {
            throw new RuntimeException("存款时间太短，无法支取");
        }

        // 7. 计算利息（活期利率）
        // 活期年利率：0.35%，日利率：0.35%/365
//        BigDecimal currentAnnualRate = new BigDecimal("0.0035");
//        BigDecimal dailyRate = currentAnnualRate.divide(new BigDecimal("365"), 8, BigDecimal.ROUND_HALF_UP);
// 7. 计算利息（活期利率）- 修改这里
        // 原来：BigDecimal currentAnnualRate = new BigDecimal("0.0035");
        // 现在：从利率服务获取活期利率
        BigDecimal currentAnnualRate = interestRateService.getCurrentRate();
        BigDecimal dailyRate = currentAnnualRate.divide(new BigDecimal("365"), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal principal = fixedDeposit.getPrincipal();
        BigDecimal interest = principal.multiply(dailyRate)
                .multiply(new BigDecimal(heldDays))
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        // 8. 计算总金额（本金 + 活期利息）
        BigDecimal totalAmount = principal.add(interest);

        // 9. 更新银行卡余额
        BigDecimal newBalance = bankCard.getBalance().add(totalAmount);
        BigDecimal newAvailableBalance = bankCard.getAvailableBalance().add(totalAmount);

        int updateResult = bankCardMapper.updateBalance(
                fixedDeposit.getCardId(),
                newBalance,
                newAvailableBalance,
                java.time.LocalDateTime.now()
        );

        if (updateResult == 0) {
            throw new RuntimeException("更新余额失败");
        }

        // 10. 更新定期存款状态为"已支取"
        fixedDepositMapper.updateStatus(fdId, 2); // 2=已支取

        // 11. 记录交易流水（添加这部分代码）
        Transaction transaction = new Transaction();
        transaction.setTransNo(idGenerator.generateTransNo());
        transaction.setCardId(fixedDeposit.getCardId());
        transaction.setUserId(userId);
        transaction.setTransType("DEPOSIT");
        transaction.setTransSubtype("FIXED_DEPOSIT_EARLY");
        transaction.setAmount(totalAmount);
        transaction.setBalanceBefore(bankCard.getBalance());
        transaction.setBalanceAfter(newBalance);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setCurrency("CNY");
        transaction.setStatus(1);
        transaction.setRemark("定期存款提前支取");
        transaction.setOperatorId(userId);
        transaction.setOperatorType("USER");
        transaction.setTransTime(java.time.LocalDateTime.now());
        transaction.setCompletedTime(java.time.LocalDateTime.now());
        transactionMapper.insert(transaction);

        // 12. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("fdId", fdId);
        result.put("cardId", fixedDeposit.getCardId());
        result.put("principal", principal);
        result.put("heldDays", heldDays);
        result.put("interest", interest);
        result.put("totalAmount", totalAmount);
        result.put("transNo", transaction.getTransNo());  // 添加交易流水号
        result.put("withdrawTime", new Date());

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> matureWithdraw(Integer fdId, String userId, String cardPassword) {
        // 1. 查询定期存款
        FixedDeposit fixedDeposit = fixedDepositMapper.findById(fdId);
        if (fixedDeposit == null) {
            throw new RuntimeException("定期存款不存在");
        }

        // 2. 验证权限
        if (!fixedDeposit.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此定期存款");
        }

        // 3. 验证状态 - 必须是"进行中"(0)
        if (fixedDeposit.getStatus() != 0) {
            throw new RuntimeException("定期存款状态异常，无法转出");
        }

        // 4. 验证是否已到期
        Date endDate = fixedDeposit.getEndTime();
        Date currentDate = new Date();
        if (currentDate.before(endDate)) {
            throw new RuntimeException("定期存款尚未到期");
        }

        // 5. 查询银行卡
        BankCard bankCard = bankCardMapper.findByCardId(fixedDeposit.getCardId());
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }

        // 6. 验证交易密码
        if (!passwordUtil.matches(cardPassword, bankCard.getCardPassword())) {
            throw new RuntimeException("交易密码错误");
        }

        // 7. 计算利息（按定期利率）
        BigDecimal principal = fixedDeposit.getPrincipal();
        BigDecimal annualRate = fixedDeposit.getRate(); // 定期年利率

        // 计算持有月数
        Date startDate = fixedDeposit.getStartTime();
        long diffInMonths = ChronoUnit.MONTHS.between(
                startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                currentDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        );

        // 月利率 = 年利率 / 12
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 8, BigDecimal.ROUND_HALF_UP);
        BigDecimal interest = principal.multiply(monthlyRate)
                .multiply(new BigDecimal(diffInMonths))
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        // 8. 计算总金额（本金 + 定期利息）
        BigDecimal totalAmount = principal.add(interest);

        // 9. 更新银行卡余额
        BigDecimal newBalance = bankCard.getBalance().add(totalAmount);
        BigDecimal newAvailableBalance = bankCard.getAvailableBalance().add(totalAmount);

        int updateResult = bankCardMapper.updateBalance(
                fixedDeposit.getCardId(),
                newBalance,
                newAvailableBalance,
                java.time.LocalDateTime.now()
        );

        if (updateResult == 0) {
            throw new RuntimeException("更新余额失败");
        }

        // 10. 更新定期存款状态为"已转出"
        fixedDepositMapper.updateStatus(fdId, 3); // 3=已转出（根据你的数据库状态）

        // 11. 记录交易流水
        Transaction transaction = new Transaction();

        // 生成交易流水号
        String transNo = "T" + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int)(Math.random() * 10000));

        transaction.setTransNo(transNo);
        transaction.setCardId(fixedDeposit.getCardId());
        transaction.setUserId(userId);
        transaction.setTransType("DEPOSIT");
        transaction.setTransSubtype("FIXED_DEPOSIT_MATURE");
        transaction.setAmount(totalAmount);
        transaction.setBalanceBefore(bankCard.getBalance());
        transaction.setBalanceAfter(newBalance);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setCurrency("CNY");
        transaction.setStatus(1);
        transaction.setRemark("定期存款到期转出");
        transaction.setOperatorId(userId);
        transaction.setOperatorType("USER");
        transaction.setTransTime(java.time.LocalDateTime.now());
        transaction.setCompletedTime(java.time.LocalDateTime.now());

        transactionMapper.insert(transaction);

        // 12. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("fdId", fdId);
        result.put("cardId", fixedDeposit.getCardId());
        result.put("principal", principal);
        result.put("annualRate", annualRate);
        result.put("interest", interest);
        result.put("totalAmount", totalAmount);
        result.put("heldMonths", diffInMonths);
        result.put("transNo", transNo);
        result.put("withdrawTime", new Date());

        return result;
    }

}
