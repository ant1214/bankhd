package com.zychen.bank.service;

import com.zychen.bank.dto.FixedDepositDTO;
import com.zychen.bank.model.FixedDeposit;
import java.util.List;

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
}
