package com.zychen.bank.service;


import com.zychen.bank.dto.FreezeCardDTO;
import com.zychen.bank.dto.UnfreezeCardDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.mapper.FreezeRecordMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.model.FreezeRecord;
import com.zychen.bank.service.SecurityService;
import com.zychen.bank.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecurityServiceImpl implements SecurityService {

    @Autowired
    private BankCardMapper bankCardMapper;

    @Autowired
    private FreezeRecordMapper freezeRecordMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Override
    @Transactional
    public Map<String, Object> freezeCard(FreezeCardDTO dto, String userId) {
        // 1. 验证银行卡
        BankCard bankCard = bankCardMapper.findByCardId(dto.getCardId());
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }

        // 2. 验证权限
        if (!bankCard.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此银行卡");
        }

        // 3. 验证交易密码
        if (!passwordUtil.matches(dto.getCardPassword(), bankCard.getCardPassword())) {
            throw new RuntimeException("交易密码错误");
        }

        // 4. 验证银行卡状态
        if (bankCard.getStatus() != 0) {
            String statusText = getCardStatusText(bankCard.getStatus());
            throw new RuntimeException("银行卡状态异常：" + statusText);
        }

        // 5. 检查是否已冻结
        FreezeRecord activeFreeze = freezeRecordMapper.findActiveFreezeByCardId(dto.getCardId());
        if (activeFreeze != null) {
            throw new RuntimeException("银行卡已处于冻结状态");
        }

        // 6. 更新银行卡状态为"冻结"(2)
        bankCardMapper.updateStatus(dto.getCardId(), 2);

        // 7. 创建冻结记录
        FreezeRecord freezeRecord = new FreezeRecord();

        // 生成冻结编号
        String freezeNo = "FZ" + LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int)(Math.random() * 10000));

        freezeRecord.setFreezeNo(freezeNo);
        freezeRecord.setFreezeType(1);        // 1=银行卡冻结
        freezeRecord.setFreezeLevel(1);       // 1=用户申请
        freezeRecord.setTargetId(dto.getCardId());
        freezeRecord.setUserId(userId);
        freezeRecord.setCardId(dto.getCardId());
        freezeRecord.setReasonType("USER_APPLICATION");
        freezeRecord.setReasonDetail(dto.getReason());
        freezeRecord.setFreezeTime(LocalDateTime.now());
        freezeRecord.setOperatorId(userId);
        freezeRecord.setOperatorRole(0);      // 0=用户
        freezeRecord.setStatus(1);            // 1=冻结中

        freezeRecordMapper.insert(freezeRecord);

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("freezeNo", freezeNo);
        result.put("cardId", dto.getCardId());
        result.put("reason", dto.getReason());
        result.put("freezeTime", LocalDateTime.now());
        result.put("contactPhone", dto.getContactPhone());
        result.put("message", "银行卡冻结申请已提交");

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> unfreezeCard(UnfreezeCardDTO dto, String userId) {
        // 1. 验证银行卡
        BankCard bankCard = bankCardMapper.findByCardId(dto.getCardId());
        if (bankCard == null) {
            throw new RuntimeException("银行卡不存在");
        }

        // 2. 验证权限
        if (!bankCard.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此银行卡");
        }

        // 3. 验证交易密码
        if (!passwordUtil.matches(dto.getCardPassword(), bankCard.getCardPassword())) {
            throw new RuntimeException("交易密码错误");
        }

        // 4. 验证银行卡状态
        if (bankCard.getStatus() != 2) {
            throw new RuntimeException("银行卡未处于冻结状态");
        }

        // 5. 查询冻结记录
        FreezeRecord activeFreeze = freezeRecordMapper.findActiveFreezeByCardId(dto.getCardId());
        if (activeFreeze == null) {
            throw new RuntimeException("未找到有效的冻结记录");
        }

        // 6. 更新银行卡状态为"正常"(0)
        bankCardMapper.updateStatus(dto.getCardId(), 0);

        // 7. 更新冻结记录状态为"已解冻"(0)
        LocalDateTime now = LocalDateTime.now();
        freezeRecordMapper.updateStatus(activeFreeze.getRecordId(), 0, now);

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("freezeNo", activeFreeze.getFreezeNo());
        result.put("cardId", dto.getCardId());
        result.put("freezeTime", activeFreeze.getFreezeTime());
        result.put("unfreezeTime", now);
        result.put("reason", dto.getReason());
        result.put("message", "银行卡解冻成功");

        return result;
    }

    @Override
    public List<FreezeRecord> getFreezeRecords(String userId, String cardId) {
        if (cardId != null) {
            return freezeRecordMapper.findByCardId(cardId);
        } else {
            return freezeRecordMapper.findByUserId(userId);
        }
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
}