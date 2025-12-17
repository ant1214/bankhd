package com.zychen.bank.mapper;

import com.zychen.bank.model.InterestRate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface InterestRateMapper {

    /**
     * 获取当前有效的利率配置
     */
    @Select("SELECT * FROM interest_rate_config " +
            "WHERE is_active = 1 " +
            "ORDER BY account_type, term_months")
    List<InterestRate> findAllActiveRates();

    /**
     * 根据账户类型获取利率
     */
    @Select("SELECT * FROM interest_rate_config " +
            "WHERE account_type = #{accountType} " +
            "AND is_active = 1 " +
            "ORDER BY effective_date DESC LIMIT 1")
    InterestRate findCurrentRateByType(String accountType);

    /**
     * 获取活期利率
     */
    @Select("SELECT * FROM interest_rate_config " +
            "WHERE account_type = 'CURRENT' " +
            "AND is_active = 1 " +
            "ORDER BY effective_date DESC LIMIT 1")
    InterestRate findCurrentDepositRate();

    /**
     * 根据期限获取定期利率
     */
    @Select("SELECT * FROM interest_rate_config " +
            "WHERE account_type LIKE 'FIXED_%' " +
            "AND term_months = #{termMonths} " +
            "AND is_active = 1 " +
            "ORDER BY effective_date DESC LIMIT 1")
    InterestRate findFixedRateByTerm(Integer termMonths);
}