package com.zychen.bank.service;

import com.zychen.bank.dto.AdminFreezeRequestDTO;
import com.zychen.bank.dto.FreezeCardDTO;
import com.zychen.bank.dto.LostReportDTO;
import com.zychen.bank.dto.UnfreezeCardDTO;
import com.zychen.bank.model.FreezeRecord;

import javax.xml.transform.Result;
import java.util.List;
import java.util.Map;

public interface SecurityService {

    /**
     * 用户申请冻结银行卡
     */
    Map<String, Object> freezeCard(FreezeCardDTO dto, String userId);

    /**
     * 用户申请解冻银行卡
     */
    Map<String, Object> unfreezeCard(UnfreezeCardDTO dto, String userId);

    /**
     * 查询用户的冻结记录
     */
    List<FreezeRecord> getFreezeRecords(String userId, String cardId);

    // 管理员冻结/解冻账户或银行卡
    Map<String, Object> adminFreezeOrUnfreeze(AdminFreezeRequestDTO request, String operatorId);
    // 管理员挂失/解挂银行卡
    Map<String, Object> adminLostReport(LostReportDTO request, String operatorId);

}
