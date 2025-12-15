package com.zychen.bank.mapper;

import com.zychen.bank.model.BankCard;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface BankCardMapper {

    // 插入银行卡
    @Insert("INSERT INTO bank_card (card_id, user_id, card_password, balance, available_balance, " +
            "frozen_amount, card_type, status, bind_time, daily_limit, monthly_limit) " +
            "VALUES (#{cardId}, #{userId}, #{cardPassword}, #{balance}, #{availableBalance}, " +
            "#{frozenAmount}, #{cardType}, #{status}, #{bindTime}, #{dailyLimit}, #{monthlyLimit})")
    int insert(BankCard bankCard);

    // 根据卡号查询
    @Select("SELECT * FROM bank_card WHERE card_id = #{cardId}")
    BankCard findByCardId(@Param("cardId") String cardId);

    // 根据用户ID查询所有银行卡
    @Select("SELECT * FROM bank_card WHERE user_id = #{userId} AND status != 3 ORDER BY bind_time DESC")
    java.util.List<BankCard> findByUserId(@Param("userId") String userId);

    // 检查卡号是否已存在（包括已解绑的）
    @Select("SELECT COUNT(*) FROM bank_card WHERE card_id = #{cardId}")
    int countByCardId(@Param("cardId") String cardId);

    // 更新余额
    @Update("UPDATE bank_card SET balance = #{balance}, " +
            "available_balance = #{availableBalance}, " +
            "last_transaction_time = #{lastTransactionTime} " +
            "WHERE card_id = #{cardId}")
    int updateBalance(@Param("cardId") String cardId,
                      @Param("balance") BigDecimal balance,
                      @Param("availableBalance") BigDecimal availableBalance,
                      @Param("lastTransactionTime") LocalDateTime lastTransactionTime);

}