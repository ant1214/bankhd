package com.zychen.bank.service;

import com.zychen.bank.dto.FixedDepositDTO;
import com.zychen.bank.model.FixedDeposit;
import java.util.List;
import java.util.Map;

public interface FixedDepositService {

    /**
     * 创建定期存款
     */
    FixedDeposit createFixedDeposit(FixedDepositDTO dto, String userId);

    /**
     * 获取用户的定期存款列表
     */
    List<FixedDeposit> getFixedDepositsByUser(String userId);

    /**
     * 获取定期存款详情
     */
    FixedDeposit getFixedDepositDetail(Integer fdId, String userId);

    /**
     * 获取银行卡的定期存款列表
     */
    List<FixedDeposit> getFixedDepositsByCard(String cardId, String userId);

    /**
     * 提前支取定期存款
     * @param fdId 定期存单ID
     * @param userId 用户ID
     * @param cardPassword 交易密码
     * @return 支取结果
     */
    Map<String, Object> earlyWithdraw(Integer fdId, String userId, String cardPassword);

    /**
     * 到期转出定期存款
     */
    Map<String, Object> matureWithdraw(Integer fdId, String userId, String cardPassword);
}
