package com.zychen.bank.service;
import com.zychen.bank.model.InterestRate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface InterestRateService {

    /**
     * 获取所有利率配置
     */
    List<InterestRate> getAllInterestRates();

    /**
     * 获取格式化后的利率信息（给前端用）
     */
    Map<String, Object> getFormattedInterestRates();

    /**
     * 根据期限获取利率
     */
    BigDecimal getRateByTerm(Integer termMonths);

    /**
     * 获取活期利率
     */
    BigDecimal getCurrentRate();
}