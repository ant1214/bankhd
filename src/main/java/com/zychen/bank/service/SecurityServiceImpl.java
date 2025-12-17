package com.zychen.bank.service;


import com.zychen.bank.dto.AdminFreezeRequestDTO;
import com.zychen.bank.dto.FreezeCardDTO;
import com.zychen.bank.dto.LostReportDTO;
import com.zychen.bank.dto.UnfreezeCardDTO;
import com.zychen.bank.mapper.BankCardMapper;
import com.zychen.bank.mapper.FreezeRecordMapper;
import com.zychen.bank.mapper.UserMapper;
import com.zychen.bank.model.BankCard;
import com.zychen.bank.model.FreezeRecord;
import com.zychen.bank.model.User;
import com.zychen.bank.service.SecurityService;
import com.zychen.bank.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SecurityServiceImpl implements SecurityService {
    @Autowired
    private UserMapper userMapper;  // 添加userMapper

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

//        // 5. 查询冻结记录
//        FreezeRecord activeFreeze = freezeRecordMapper.findActiveFreezeByCardId(dto.getCardId());
//        if (activeFreeze == null) {
//            throw new RuntimeException("未找到有效的冻结记录");
//        }
        // 5. 查询用户自己冻结的记录
        FreezeRecord userFreeze = freezeRecordMapper.findActiveUserFreezeByCardId(dto.getCardId(), userId);
        if (userFreeze == null) {
            // 检查是否是管理员冻结的
            FreezeRecord anyFreeze = freezeRecordMapper.findActiveFreezeByCardId(dto.getCardId());
            if (anyFreeze != null && anyFreeze.getFreezeLevel() == 2) {
                throw new RuntimeException("此卡被管理员冻结，请联系管理员解冻");
            }
            throw new RuntimeException("未找到有效的冻结记录");
        }

        // 6. 更新银行卡状态为"正常"(0)
        bankCardMapper.updateStatus(dto.getCardId(), 0);

        // 7. 更新冻结记录状态为"已解冻"(0)
        LocalDateTime now = LocalDateTime.now();
        freezeRecordMapper.updateStatus(userFreeze.getRecordId(), 0, now);

        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("freezeNo", userFreeze.getFreezeNo());
        result.put("cardId", dto.getCardId());
        result.put("freezeTime", userFreeze.getFreezeTime());
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

    @Override
    @Transactional
    public Map<String, Object> adminFreezeOrUnfreeze(AdminFreezeRequestDTO request, String operatorId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证管理员权限
            User operator = userMapper.findByUserId(operatorId);
            if (operator == null || operator.getRole() != 1) {
                result.put("success", false);
                result.put("message", "无操作权限");
                return result;
            }

            String targetType = request.getTargetType();
            String targetId = request.getTargetId();
            String operation = request.getOperation();

            if ("account".equals(targetType)) {
                return handleAdminAccountFreeze(request, operatorId, operator);
            } else if ("card".equals(targetType)) {
                return handleAdminCardFreeze(request, operatorId, operator);
            } else {
                result.put("success", false);
                result.put("message", "无效的目标类型");
                return result;
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> adminLostReport(LostReportDTO request, String operatorId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证管理员权限
            User operator = userMapper.findByUserId(operatorId);
            if (operator == null || operator.getRole() != 1) {
                result.put("success", false);
                result.put("message", "无操作权限");
                return result;
            }

            String cardId = request.getCardId();
            String operation = request.getOperation();

            // 2. 验证银行卡是否存在
            BankCard bankCard = bankCardMapper.findByCardId(cardId);
            if (bankCard == null) {
                result.put("success", false);
                result.put("message", "银行卡不存在");
                return result;
            }

            // 3. 挂失操作
            if ("report".equals(operation)) {
                // 检查当前状态
                if (bankCard.getStatus() == 1) {
                    result.put("success", false);
                    result.put("message", "银行卡已挂失");
                    return result;
                }
                if (bankCard.getStatus() != 0) {
                    String statusText = getCardStatusText(bankCard.getStatus());
                    result.put("success", false);
                    result.put("message", "银行卡当前状态无法挂失，当前状态：" + statusText);
                    return result;
                }

                // 更新银行卡状态为挂失
                bankCardMapper.updateStatus(cardId, 1);

                // 创建冻结记录（为了记录挂失操作）
                FreezeRecord freezeRecord = new FreezeRecord();

                String freezeNo = "LS" + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        + String.format("%04d", (int)(Math.random() * 10000));

                freezeRecord.setFreezeNo(freezeNo);
                freezeRecord.setFreezeType(1);        // 1=银行卡冻结
                freezeRecord.setFreezeLevel(2);       // 2=管理员操作
                freezeRecord.setTargetId(cardId);
                freezeRecord.setUserId(bankCard.getUserId());
                freezeRecord.setCardId(cardId);
                freezeRecord.setReasonType(request.getReasonType());
                freezeRecord.setReasonDetail(request.getReasonDetail());
                freezeRecord.setFreezeTime(LocalDateTime.now());
                freezeRecord.setOperatorId(operatorId);
                freezeRecord.setOperatorRole(1);      // 1=管理员
                freezeRecord.setStatus(1);            // 1=冻结中

                freezeRecordMapper.insert(freezeRecord);

                result.put("success", true);
                result.put("cardId", cardId);
                result.put("operation", "report");
                result.put("newStatus", 1);
                result.put("reportTime", LocalDateTime.now());
                result.put("freezeId", freezeNo);
                result.put("message", "银行卡挂失成功");

                return result;
            }
            // 4. 解挂操作
            else if ("cancel".equals(operation)) {
                if (bankCard.getStatus() != 1) {
                    result.put("success", false);
                    result.put("message", "银行卡当前不是挂失状态");
                    return result;
                }

                // 更新银行卡状态为正常
                bankCardMapper.updateStatus(cardId, 0);

                // 更新冻结记录状态
                FreezeRecord activeFreeze = freezeRecordMapper.findActiveFreezeByCardId(cardId);
                if (activeFreeze != null) {
                    LocalDateTime now = LocalDateTime.now();
                    freezeRecordMapper.updateStatus(activeFreeze.getRecordId(), 0, now);
                }

                result.put("success", true);
                result.put("cardId", cardId);
                result.put("operation", "cancel");
                result.put("newStatus", 0);
                result.put("cancelTime", LocalDateTime.now());
                result.put("message", "银行卡解挂成功");

                return result;
            } else {
                result.put("success", false);
                result.put("message", "无效的操作类型");
                return result;
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
            return result;
        }
    }

    // 私有辅助方法
    private Map<String, Object> handleAdminAccountFreeze(AdminFreezeRequestDTO request, String operatorId, User operator) {
        Map<String, Object> result = new HashMap<>();

        String userId = request.getTargetId();
        String operation = request.getOperation();

        // 1. 验证用户是否存在
        User user = userMapper.findByUserId(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        if ("freeze".equals(operation)) {
            // 检查是否已经是冻结状态
            if (user.getAccountStatus() == 1) {
                result.put("success", false);
                result.put("message", "账户已是冻结状态");
                return result;
            }

            // 更新用户账户状态为冻结
            user.setAccountStatus(1);
            userMapper.updateAccountStatus(userId, 1);

            // 冻结该用户的所有正常状态银行卡
            List<BankCard> cards = bankCardMapper.findByUserId(userId);
            int frozenCards = 0;
            for (BankCard card : cards) {
                if (card.getStatus() == 0) { // 只冻结正常状态的卡
                    bankCardMapper.updateStatus(card.getCardId(), 2);
                    frozenCards++;
                }
            }

            // 创建冻结记录
            FreezeRecord freezeRecord = new FreezeRecord();

            String freezeNo = "FZ" + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + String.format("%04d", (int)(Math.random() * 10000));

            freezeRecord.setFreezeNo(freezeNo);
            freezeRecord.setFreezeType(2);        // 2=账户冻结
            freezeRecord.setFreezeLevel(2);       // 2=管理员操作
            freezeRecord.setTargetId(userId);
            freezeRecord.setUserId(userId);
            freezeRecord.setReasonType(request.getReasonType());
            freezeRecord.setReasonDetail(request.getReasonDetail());
            freezeRecord.setFreezeTime(LocalDateTime.now());

            // 设置计划解冻时间
            if (request.getFreezeDuration() > 0) {
                freezeRecord.setPlannedUnfreezeTime(
                        LocalDateTime.now().plusDays(request.getFreezeDuration())
                );
            }

            freezeRecord.setOperatorId(operatorId);
            freezeRecord.setOperatorRole(1);      // 1=管理员
            freezeRecord.setStatus(1);            // 1=冻结中

            freezeRecordMapper.insert(freezeRecord);

            result.put("success", true);
            result.put("freezeId", freezeNo);
            result.put("targetType", "account");
            result.put("targetId", userId);
            result.put("operation", "freeze");
            result.put("freezeTime", LocalDateTime.now());
            result.put("affectedCards", frozenCards);
            result.put("message", "账户冻结成功，共冻结" + frozenCards + "张银行卡");

        } else if ("unfreeze".equals(operation)) {
            // 检查是否已经是正常状态
            if (user.getAccountStatus() == 0) {
                result.put("success", false);
                result.put("message", "账户已是正常状态");
                return result;
            }

            // 更新用户账户状态为正常
            user.setAccountStatus(0);
            userMapper.updateAccountStatus(userId, 0);

            // 解冻该用户的冻结状态银行卡（只解冻因账户冻结而冻结的卡）
            List<BankCard> cards = bankCardMapper.findByUserId(userId);
            int unfrozenCards = 0;
            for (BankCard card : cards) {
                if (card.getStatus() == 2) { // 只解冻冻结状态的卡
                    bankCardMapper.updateStatus(card.getCardId(), 0);
                    unfrozenCards++;
                }
            }

            // 更新账户冻结记录状态
            List<FreezeRecord> accountFreezeRecords = freezeRecordMapper.findByUserId(userId);
            for (FreezeRecord record : accountFreezeRecords) {
                if (record.getFreezeType() == 2 && record.getStatus() == 1) {
                    record.setStatus(0);
                    record.setUnfreezeTime(LocalDateTime.now());
                    freezeRecordMapper.update(record);
                }
            }

            result.put("success", true);
            result.put("targetType", "account");
            result.put("targetId", userId);
            result.put("operation", "unfreeze");
            result.put("unfreezeTime", LocalDateTime.now());
            result.put("unfrozenCards", unfrozenCards);
            result.put("message", "账户解冻成功，共解冻" + unfrozenCards + "张银行卡");

        } else {
            result.put("success", false);
            result.put("message", "无效的操作类型");
        }

        return result;
    }

    private Map<String, Object> handleAdminCardFreeze(AdminFreezeRequestDTO request, String operatorId, User operator) {
        Map<String, Object> result = new HashMap<>();

        String cardId = request.getTargetId();
        String operation = request.getOperation();

        // 1. 验证银行卡是否存在
        BankCard bankCard = bankCardMapper.findByCardId(cardId);
        if (bankCard == null) {
            result.put("success", false);
            result.put("message", "银行卡不存在");
            return result;
        }

        if ("freeze".equals(operation)) {
            if (bankCard.getStatus() != 0) {
                String statusText = getCardStatusText(bankCard.getStatus());
                result.put("success", false);
                result.put("message", "银行卡当前状态无法冻结，当前状态：" + statusText);
                return result;
            }

            // 更新银行卡状态
            bankCardMapper.updateStatus(cardId, 2);

            // 创建冻结记录
            FreezeRecord freezeRecord = new FreezeRecord();

            String freezeNo = "FZ" + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + String.format("%04d", (int)(Math.random() * 10000));

            freezeRecord.setFreezeNo(freezeNo);
            freezeRecord.setFreezeType(1);        // 1=银行卡冻结
            freezeRecord.setFreezeLevel(2);       // 2=管理员操作
            freezeRecord.setTargetId(cardId);
            freezeRecord.setUserId(bankCard.getUserId());
            freezeRecord.setCardId(cardId);
            freezeRecord.setReasonType(request.getReasonType());
            freezeRecord.setReasonDetail(request.getReasonDetail());
            freezeRecord.setFreezeTime(LocalDateTime.now());

            // 设置计划解冻时间
            if (request.getFreezeDuration() > 0) {
                freezeRecord.setPlannedUnfreezeTime(
                        LocalDateTime.now().plusDays(request.getFreezeDuration())
                );
            }

            freezeRecord.setOperatorId(operatorId);
            freezeRecord.setOperatorRole(1);      // 1=管理员
            freezeRecord.setStatus(1);            // 1=冻结中

            freezeRecordMapper.insert(freezeRecord);

            result.put("success", true);
            result.put("freezeId", freezeNo);
            result.put("cardId", cardId);
            result.put("operation", "freeze");
            result.put("freezeTime", LocalDateTime.now());
            result.put("message", "银行卡冻结成功");

        } else if ("unfreeze".equals(operation)) {
            if (bankCard.getStatus() != 2) {
                result.put("success", false);
                result.put("message", "银行卡当前不是冻结状态");
                return result;
            }

            // 更新银行卡状态
            bankCardMapper.updateStatus(cardId, 0);

            // 更新冻结记录状态
            FreezeRecord activeFreeze = freezeRecordMapper.findActiveFreezeByCardId(cardId);
            if (activeFreeze != null) {
                LocalDateTime now = LocalDateTime.now();
                freezeRecordMapper.updateStatus(activeFreeze.getRecordId(), 0, now);
            }

            result.put("success", true);
            result.put("cardId", cardId);
            result.put("operation", "unfreeze");
            result.put("unfreezeTime", LocalDateTime.now());
            result.put("message", "银行卡解冻成功");

        } else {
            result.put("success", false);
            result.put("message", "无效的操作类型");
        }

        return result;
    }
}